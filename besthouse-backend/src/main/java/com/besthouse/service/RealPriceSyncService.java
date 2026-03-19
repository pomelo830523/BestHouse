package com.besthouse.service;

import com.besthouse.entity.RealPriceRecord;
import com.besthouse.repository.RealPriceRecordRepository;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealPriceSyncService {

    private final RealPriceRecordRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String SEASON_URL =
            "https://plvr.land.moi.gov.tw/DownloadSeason?type=csv&season=%s&fileName=%s_lvr_land_A.csv";

    private static final int LOOKBACK_YEARS = 3;

    // 支援的縣市代碼
    public static final Map<String, String> SUPPORTED_CITIES = Map.of(
            "J", "新竹縣",
            "O", "新竹市"
    );

    // CSV 欄位名稱（優先取標準欄位，再容錯舊格式）
    private static final List<String> COL_ADDRESS_CANDIDATES =
            List.of("土地位置建物門牌", "土地區段位置或建物區段門牌", "土地區段位置建物區段門牌", "土地區段位置/建物區段門牌");

    @Transactional
    public Map<String, Integer> syncAll() throws Exception {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String cityCode : SUPPORTED_CITIES.keySet()) {
            result.put(cityCode, sync(cityCode));
        }
        return result;
    }

    @Transactional
    public int sync(String cityCode) throws Exception {
        if (!SUPPORTED_CITIES.containsKey(cityCode)) {
            throw new IllegalArgumentException("不支援的縣市代碼：" + cityCode);
        }

        List<String> seasons = generateSeasons(LOOKBACK_YEARS);
        log.info("開始同步 {}（{}），共 {} 季", SUPPORTED_CITIES.get(cityCode), cityCode, seasons.size());

        List<RealPriceRecord> allRecords = new ArrayList<>();
        int failed = 0;

        for (String season : seasons) {
            String url = String.format(SEASON_URL, season, cityCode);
            try {
                ResponseEntity<byte[]> response = restTemplate.exchange(
                        url, HttpMethod.GET, null, byte[].class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    byte[] csvBytes = response.getBody();
                    String csvContent = decodeCsv(csvBytes);
                    List<RealPriceRecord> records = parseCsv(csvContent, cityCode);
                    allRecords.addAll(records);
                    log.info("  {} → {} 筆", season, records.size());
                }
            } catch (HttpClientErrorException.NotFound e) {
                log.debug("  {} → 無資料（404），跳過", season);
            } catch (Exception e) {
                failed++;
                log.warn("  {} → 下載失敗：{}", season, e.getMessage());
            }
        }

        log.info("解析完成：共 {} 筆（cityCode={}），{} 季失敗", allRecords.size(), cityCode, failed);
        repository.deleteByCityCode(cityCode);
        repository.saveAll(allRecords);
        log.info("同步完成：{} 筆（cityCode={}）", allRecords.size(), cityCode);
        return allRecords.size();
    }

    /**
     * 產生過去 N 年的所有季度字串，例如 ["110S1", "110S2", ..., "115S1"]
     */
    private List<String> generateSeasons(int yearsBack) {
        LocalDate now = LocalDate.now();
        LocalDate cutoff = now.minusYears(yearsBack);

        int startRocYear = cutoff.getYear() - 1911;
        int startQ = (cutoff.getMonthValue() - 1) / 3 + 1;
        int endRocYear = now.getYear() - 1911;
        int endQ = (now.getMonthValue() - 1) / 3 + 1;

        List<String> seasons = new ArrayList<>();
        for (int year = startRocYear; year <= endRocYear; year++) {
            int qFrom = (year == startRocYear) ? startQ : 1;
            int qTo   = (year == endRocYear)   ? endQ   : 4;
            for (int q = qFrom; q <= qTo; q++) {
                seasons.add(year + "S" + q);
            }
        }
        return seasons;
    }

    /**
     * 嘗試 UTF-8（含 BOM），若第一行找不到「鄉鎮市區」則改用 Big5
     */
    private String decodeCsv(byte[] bytes) {
        // 去掉 UTF-8 BOM
        int offset = (bytes.length >= 3 && bytes[0] == (byte) 0xEF
                && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) ? 3 : 0;
        String utf8 = new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8);
        if (utf8.contains("鄉鎮市區")) {
            log.info("CSV 編碼：UTF-8");
            return utf8;
        }
        try {
            String big5 = new String(bytes, offset, bytes.length - offset,
                    java.nio.charset.Charset.forName("Big5"));
            if (big5.contains("鄉鎮市區")) {
                log.info("CSV 編碼：Big5");
                return big5;
            }
        } catch (Exception e) {
            log.warn("Big5 解碼失敗：{}", e.getMessage());
        }
        log.warn("無法確認 CSV 編碼，使用 UTF-8（前100字元：{}）", utf8.substring(0, Math.min(100, utf8.length())));
        return utf8;
    }

    private List<RealPriceRecord> parseCsv(String csvContent, String cityCode) throws Exception {
        List<RealPriceRecord> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int skipped = 0;

        try (CSVReader reader = new CSVReader(new StringReader(csvContent))) {
            Map<String, Integer> colIndex = null;
            String[] line;
            int lineNum = 0;

            while ((line = reader.readNext()) != null) {
                lineNum++;

                // 找 header 行（包含「鄉鎮市區」的那行）
                if (colIndex == null) {
                    if (lineNum <= 3) {
                        log.info("CSV 第{}行（{}欄）：{}", lineNum, line.length,
                                line.length > 0 ? line[0] : "(空)");
                    }
                    for (int i = 0; i < line.length; i++) {
                        if ("鄉鎮市區".equals(line[i].trim())) {
                            colIndex = new HashMap<>();
                            for (int j = 0; j < line.length; j++) {
                                colIndex.put(line[j].trim(), j);
                            }
                            log.info("找到 header，共 {} 欄，欄位：{}", colIndex.size(), colIndex.keySet());
                            break;
                        }
                    }
                    continue;
                }

                if (line.length < 10) continue;

                // 僅處理含「建物」的交易標的
                String target = get(line, colIndex, "交易標的");
                if (target == null || !target.contains("建物")) continue;

                // 僅處理住家用途
                String mainUse = get(line, colIndex, "主要用途");
                if (mainUse != null && !mainUse.contains("住家") && !mainUse.contains("住宅")) continue;

                try {
                    RealPriceRecord record = parseRecord(line, colIndex, cityCode, now);
                    if (record != null) result.add(record);
                } catch (Exception e) {
                    skipped++;
                    log.debug("跳過第 {} 行，解析失敗: {}", lineNum, e.getMessage());
                }
            }
        }

        if (skipped > 0) log.warn("共跳過 {} 筆無法解析的資料", skipped);
        return result;
    }

    private RealPriceRecord parseRecord(String[] line, Map<String, Integer> colIndex,
                                        String cityCode, LocalDateTime syncedAt) {
        // 建物面積（㎡ → 坪）
        String areaSqmStr = get(line, colIndex, "建物移轉總面積平方公尺");
        if (areaSqmStr == null || areaSqmStr.isBlank()) return null;
        double areaSqm = parseDouble(areaSqmStr);
        if (areaSqm <= 0) return null;
        BigDecimal totalAreaPing = BigDecimal.valueOf(areaSqm * 0.3025).setScale(2, RoundingMode.HALF_UP);

        // 總價（元 → 萬）
        String priceStr = get(line, colIndex, "總價元");
        if (priceStr == null || priceStr.isBlank()) return null;
        long totalPriceYuan = parseLong(priceStr);
        if (totalPriceYuan <= 0) return null;
        BigDecimal totalPriceWan = BigDecimal.valueOf(totalPriceYuan).divide(
                BigDecimal.valueOf(10000), 2, RoundingMode.HALF_UP);

        // 車位總價（元 → 萬）
        BigDecimal parkingPriceWan = null;
        String parkingStr = get(line, colIndex, "車位總價元");
        if (parkingStr != null && !parkingStr.isBlank()) {
            long parkingYuan = parseLong(parkingStr);
            if (parkingYuan > 0) {
                parkingPriceWan = BigDecimal.valueOf(parkingYuan).divide(
                        BigDecimal.valueOf(10000), 2, RoundingMode.HALF_UP);
            }
        }

        // 車位面積（㎡ → 坪）
        BigDecimal parkingAreaPing = null;
        String parkingAreaStr = get(line, colIndex, "車位移轉總面積平方公尺");
        if (parkingAreaStr != null && !parkingAreaStr.isBlank()) {
            double parkingSqm = parseDouble(parkingAreaStr);
            if (parkingSqm > 0) {
                parkingAreaPing = BigDecimal.valueOf(parkingSqm * 0.3025).setScale(2, RoundingMode.HALF_UP);
            }
        }

        // 每坪單價（扣除車位價與車位坪，與房屋列表「不含車位單價」計算方式一致）
        BigDecimal pricePerPingWan = null;
        if (parkingPriceWan != null && parkingAreaPing != null
                && parkingAreaPing.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal netPrice = totalPriceWan.subtract(parkingPriceWan);
            BigDecimal netArea  = totalAreaPing.subtract(parkingAreaPing);
            if (netArea.compareTo(BigDecimal.ZERO) > 0) {
                pricePerPingWan = netPrice.divide(netArea, 2, RoundingMode.HALF_UP);
            }
        } else if (totalAreaPing.compareTo(BigDecimal.ZERO) > 0) {
            // 無車位資料時，直接用總價 / 總坪
            pricePerPingWan = totalPriceWan.divide(totalAreaPing, 2, RoundingMode.HALF_UP);
        }

        // 交易日期（民國 → 西元）
        LocalDate transactionDate = parseRocDate(get(line, colIndex, "交易年月日"));

        // 建築完成年份（民國 → 西元）
        Integer completedYear = parseCompletedYear(get(line, colIndex, "建築完成年月"));
        Integer houseAgeYear = completedYear != null ? (LocalDate.now().getYear() - completedYear) : null;

        // 樓層
        String floorDesc = get(line, colIndex, "移轉層次");
        Integer floorNum = parseFloor(floorDesc);
        Integer totalFloor = parseInt(get(line, colIndex, "總樓層數"));

        // 格局
        Integer bedroom   = parseInt(get(line, colIndex, "建物現況格局-房"));
        Integer livingRoom = parseInt(get(line, colIndex, "建物現況格局-廳"));
        Integer bathroom  = parseInt(get(line, colIndex, "建物現況格局-衛"));

        // 電梯 / 管委會
        String elevatorStr = get(line, colIndex, "電梯");
        Boolean hasElevator = elevatorStr != null ? elevatorStr.contains("有") : null;
        String mgmtStr = get(line, colIndex, "有無管理組織");
        Boolean hasManagement = mgmtStr != null ? mgmtStr.contains("有") : null;

        // 地址（嘗試多種欄位名稱）
        String address = null;
        for (String candidate : COL_ADDRESS_CANDIDATES) {
            address = get(line, colIndex, candidate);
            if (address != null && !address.isBlank()) break;
        }

        // 去除開頭重複的縣市／行政區名（例：「新竹市新竹市○○路」→「○○路」）
        String districtVal = get(line, colIndex, "鄉鎮市區");
        if (address != null && districtVal != null) {
            while (address.startsWith(districtVal)) {
                address = address.substring(districtVal.length()).stripLeading();
            }
        }

        return RealPriceRecord.builder()
                .cityCode(cityCode)
                .district(districtVal)
                .address(address)
                .transactionDate(transactionDate)
                .buildingType(get(line, colIndex, "建物型態"))
                .totalAreaPing(totalAreaPing)
                .floorDesc(floorDesc)
                .floorNum(floorNum)
                .totalFloor(totalFloor)
                .bedroomCount(bedroom)
                .livingRoomCount(livingRoom)
                .bathroomCount(bathroom)
                .completedYear(completedYear)
                .houseAgeYear(houseAgeYear)
                .hasElevator(hasElevator)
                .hasManagement(hasManagement)
                .totalPriceWan(totalPriceWan)
                .parkingPriceWan(parkingPriceWan)
                .parkingAreaPing(parkingAreaPing)
                .pricePerPingWan(pricePerPingWan)
                .syncedAt(syncedAt)
                .build();
    }

    // ── 解析工具方法 ──────────────────────────────────────

    private String get(String[] line, Map<String, Integer> colIndex, String colName) {
        Integer idx = colIndex.get(colName);
        if (idx == null || idx >= line.length) return null;
        String val = line[idx].trim();
        return val.isEmpty() ? null : val;
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; }
    }

    private long parseLong(String s) {
        try { return Long.parseLong(s.trim().replace(",", "")); } catch (Exception e) { return 0; }
    }

    private Integer parseInt(String s) {
        if (s == null) return null;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    /**
     * 解析民國年月日 → LocalDate
     * 格式：1130815（民國113年8月15日）= 2024-08-15
     */
    private LocalDate parseRocDate(String rocDateStr) {
        if (rocDateStr == null || rocDateStr.length() < 7) return null;
        try {
            int rocYear = Integer.parseInt(rocDateStr.substring(0, 3));
            int month   = Integer.parseInt(rocDateStr.substring(3, 5));
            int day     = Integer.parseInt(rocDateStr.substring(5, 7));
            return LocalDate.of(rocYear + 1911, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析民國建築完成年月 → 西元年
     * 格式：11001（民國110年1月）或 1050601
     */
    private Integer parseCompletedYear(String rocYearStr) {
        if (rocYearStr == null || rocYearStr.isBlank()) return null;
        try {
            // 取前3碼作為民國年
            String yearPart = rocYearStr.trim().replaceAll("[^0-9]", "");
            if (yearPart.length() < 3) return null;
            int rocYear = Integer.parseInt(yearPart.substring(0, 3));
            return rocYear + 1911;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析樓層文字 → 數字（支援國字與阿拉伯數字）
     * 例：三層→3、十二層→12、3F→3
     */
    private Integer parseFloor(String floorDesc) {
        if (floorDesc == null || floorDesc.isBlank()) return null;
        // 去掉 層/樓/F
        String cleaned = floorDesc.replaceAll("[層樓FfＦ]", "").trim();
        // 嘗試直接解析數字
        try { return Integer.parseInt(cleaned); } catch (Exception ignored) {}
        // 解析國字數字
        return parseChineseNumber(cleaned);
    }

    private Integer parseChineseNumber(String text) {
        if (text == null || text.isBlank()) return null;
        Map<Character, Integer> digitMap = new HashMap<>();
        "零一二三四五六七八九".chars().forEach(c ->
                digitMap.put((char) c, "零一二三四五六七八九".indexOf(c)));

        try {
            text = text.trim();
            if (text.equals("十")) return 10;
            if (text.startsWith("十")) {
                Integer ones = digitMap.get(text.charAt(1));
                return 10 + (ones != null ? ones : 0);
            }
            int tenIdx = text.indexOf('十');
            if (tenIdx > 0) {
                int tens = digitMap.getOrDefault(text.charAt(tenIdx - 1), 0) * 10;
                int ones = tenIdx + 1 < text.length()
                        ? digitMap.getOrDefault(text.charAt(tenIdx + 1), 0) : 0;
                return tens + ones;
            }
            if (text.length() == 1) return digitMap.getOrDefault(text.charAt(0), null);
        } catch (Exception ignored) {}
        return null;
    }
}
