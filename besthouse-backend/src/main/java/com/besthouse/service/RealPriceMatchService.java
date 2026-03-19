package com.besthouse.service;

import com.besthouse.dto.RealPriceMatchDto;
import com.besthouse.entity.House;
import com.besthouse.entity.RealPriceRecord;
import com.besthouse.exception.ResourceNotFoundException;
import com.besthouse.repository.HouseRepository;
import com.besthouse.repository.RealPriceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealPriceMatchService {

    private final HouseRepository houseRepository;
    private final RealPriceRecordRepository realPriceRecordRepository;

    private static final int TOP_N = 10;
    private static final int LOOKBACK_YEARS = 3;

    // 面積預篩容許範圍（±40%）
    private static final double AREA_PREFILTER_RANGE = 0.40;

    // 地址：路名＋段數（group1）、號數（group2）
    // 主 pattern：明確有「號」字
    private static final Pattern ADDR_PATTERN = Pattern.compile(
            "([\\u4e00-\\u9fff]+[路街道巷弄](?:[\\u4e00-\\u9fff一二三四五六七八九十百]+段)?).*?(\\d+)號");
    // 備用 pattern：無「號」字的地址（取路名後第一個數字，例：「國光街66之1三樓」→ 66）
    private static final Pattern ADDR_PATTERN_FALLBACK = Pattern.compile(
            "([\\u4e00-\\u9fff]+[路街道巷弄](?:[\\u4e00-\\u9fff一二三四五六七八九十百]+段)?)[^\\d]*(\\d+)");

    @Transactional(readOnly = true)
    public List<RealPriceMatchDto> findMatches(Long houseId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到房屋 ID: " + houseId));

        LocalDate since = LocalDate.now().minusYears(LOOKBACK_YEARS);

        // DB 層先用面積預篩，大幅縮小候選集
        List<RealPriceRecord> candidates;
        if (house.getBuildAreaPing() != null) {
            double ping = house.getBuildAreaPing().doubleValue();
            BigDecimal minPing = BigDecimal.valueOf(ping * (1 - AREA_PREFILTER_RANGE)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal maxPing = BigDecimal.valueOf(ping * (1 + AREA_PREFILTER_RANGE)).setScale(2, RoundingMode.HALF_UP);
            candidates = realPriceRecordRepository.findCandidates(since, minPing, maxPing);
        } else {
            candidates = realPriceRecordRepository.findCandidates(since,
                    BigDecimal.ZERO, BigDecimal.valueOf(9999));
        }

        if (candidates.isEmpty()) {
            log.warn("實價登錄無候選資料，請先同步或確認面積欄位");
            return List.of();
        }

        List<ScoredRecord> scored = new ArrayList<>();
        for (RealPriceRecord record : candidates) {
            // 排除透天（使用者只找公寓/大樓等集合住宅）
            String buildingType = record.getBuildingType();
            if (buildingType != null && buildingType.contains("透天")) continue;

            ScoreResult result = score(house, record);
            if (result.score > 0) {
                scored.add(new ScoredRecord(record, result));
            }
        }

        return scored.stream()
                .sorted(Comparator.comparingInt((ScoredRecord sr) -> sr.result.score).reversed())
                .limit(TOP_N)
                .map(sr -> toDto(sr.record, sr.result))
                .toList();
    }

    /**
     * 連續線性衰減評分，各維度各自計 0–100，再取加權平均。
     * 只計算雙方都有資料的維度（避免 null 拉低/拉高分數）。
     *
     * <pre>
     * 硬篩選（任一不符即排除）：
     *   - 地區不符
     *   - 路名不符（前提是雙方都能解析到路名）
     *   - 門牌號差 > 30
     *   - 屋齡差 > 10 年（雙方都有屋齡資料時）
     *
     * 維度        權重   說明
     * ──────────────────────────────
     * 門牌號距離    -    僅顯示於備註，不計分（由硬篩選把關）
     * 面積差       75%   衰減至 0 的閾值：35% 差異
     * 屋齡差       15%   衰減至 0 的閾值：10 年差異
     * 樓層差        7%   衰減至 0 的閾值：6 層差異
     * 房間數差      3%   衰減至 0 的閾值：2 間差異
     * </pre>
     */
    private ScoreResult score(House house, RealPriceRecord record) {
        // ── 硬篩選：地區 ──────────────────────────────────
        String houseAddress = house.getAddress() != null ? house.getAddress() : "";
        String district = record.getDistrict() != null ? record.getDistrict() : "";
        if (district.isBlank() || !houseAddress.contains(district)) {
            return new ScoreResult(0, "地區不符");
        }

        // ── 硬篩選：路名、段數、號差 ──────────────────────
        String recordAddress = record.getAddress() != null ? record.getAddress() : "";
        AddressInfo houseAddrInfo  = parseAddress(houseAddress);
        AddressInfo recordAddrInfo = parseAddress(recordAddress);
        boolean addressParsed = houseAddrInfo != null && recordAddrInfo != null;

        if (addressParsed) {
            // 路名不符 → 排除（contains 雙向比對，允許一方帶有縣市區前綴）
            // 不同段（如光復路一段 vs 光復路二段）互相不 contains，亦由此排除
            boolean roadMatch = houseAddrInfo.road.contains(recordAddrInfo.road)
                    || recordAddrInfo.road.contains(houseAddrInfo.road);
            if (!roadMatch) return new ScoreResult(0, "路名不符");

            // 號差 > 30 → 排除
            if (Math.abs(houseAddrInfo.houseNum - recordAddrInfo.houseNum) > 30) {
                return new ScoreResult(0, "門牌過遠");
            }
        } else if (houseAddrInfo != null) {
            // house 能解析但 record 不能（如「美之城」無路名結尾）
            // 退而求其次：record 地址文字必須包含 house 路名，否則排除
            if (!recordAddress.contains(houseAddrInfo.road)) {
                return new ScoreResult(0, "路段不符");
            }
        }

        List<String> notes = new ArrayList<>();
        double weightedSum = 0;
        double totalWeight = 0;

        // ── 門牌號距離：僅顯示於備註，不計入分數 ─────────
        if (addressParsed) {
            int numDiff = Math.abs(houseAddrInfo.houseNum - recordAddrInfo.houseNum);
            notes.add(String.format("門牌差%d號", numDiff));
        }

        // ── 面積（權重 75）─────────────────────────────────
        if (house.getBuildAreaPing() != null && record.getTotalAreaPing() != null) {
            double housePing = house.getBuildAreaPing().doubleValue();
            double recordPing = record.getTotalAreaPing().doubleValue();
            double diffPct = Math.abs(housePing - recordPing) / housePing;
            double dimScore = Math.max(0.0, 1.0 - diffPct / 0.35) * 100;
            weightedSum += dimScore * 75;
            totalWeight += 75;
            notes.add(String.format("面積差%.0f%%", diffPct * 100));
        }

        // ── 硬篩選：屋齡差 > 10 年 → 排除 ────────────────
        if (house.getHouseAgeYear() != null && record.getHouseAgeYear() != null) {
            if (Math.abs(house.getHouseAgeYear() - record.getHouseAgeYear()) > 10) {
                return new ScoreResult(0, "屋齡差距過大");
            }
        }

        // ── 屋齡（權重 15）─────────────────────────────────
        if (house.getHouseAgeYear() != null && record.getHouseAgeYear() != null) {
            int ageDiff = Math.abs(house.getHouseAgeYear() - record.getHouseAgeYear());
            double dimScore = Math.max(0.0, 1.0 - ageDiff / 10.0) * 100;
            weightedSum += dimScore * 15;
            totalWeight += 15;
            notes.add(String.format("屋齡差%d年", ageDiff));
        }

        // ── 樓層（權重 7）──────────────────────────────────
        if (house.getFloor() != null && record.getFloorNum() != null && record.getFloorNum() > 0) {
            int floorDiff = Math.abs(house.getFloor() - record.getFloorNum());
            double dimScore = Math.max(0.0, 1.0 - floorDiff / 6.0) * 100;
            weightedSum += dimScore * 7;
            totalWeight += 7;
            notes.add(String.format("樓層差%d層", floorDiff));
        }

        // ── 房間數（權重 3）────────────────────────────────
        if (house.getBedroomCount() != null && record.getBedroomCount() != null) {
            int bedroomDiff = Math.abs(house.getBedroomCount() - record.getBedroomCount());
            double dimScore = Math.max(0.0, 1.0 - bedroomDiff / 2.0) * 100;
            weightedSum += dimScore * 3;
            totalWeight += 3;
            if (bedroomDiff == 0) notes.add("格局相符");
        }

        if (totalWeight == 0) return new ScoreResult(0, "資料不足");

        int finalScore = (int) Math.round(weightedSum / totalWeight);
        return new ScoreResult(finalScore, String.join("、", notes));
    }

    /**
     * 解析地址，抽出路名（含段數）與門牌號。
     * 先將全形數字（０–９）轉為半形，再套用 regex。
     * 優先比對「號」字，無號則取路名後第一個數字（備用）。
     * 例："海濱路５５巷１４號" → road="海濱路", num=14
     * 例："國光街66之1三樓"   → road="國光街", num=66
     * 例："光復路一段123號"   → road="光復路一段", num=123
     */
    private AddressInfo parseAddress(String address) {
        if (address == null || address.isBlank()) return null;
        String normalized = normalizeFullWidthDigits(address);
        Matcher m = ADDR_PATTERN.matcher(normalized);
        if (!m.find()) {
            m = ADDR_PATTERN_FALLBACK.matcher(normalized);
            if (!m.find()) return null;
        }
        try {
            return new AddressInfo(m.group(1), Integer.parseInt(m.group(2)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 將全形數字（０–９）轉為半形（0–9） */
    private String normalizeFullWidthDigits(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            sb.append(c >= '０' && c <= '９' ? (char) (c - '０' + '0') : c);
        }
        return sb.toString();
    }

    private record AddressInfo(String road, int houseNum) {}

    private RealPriceMatchDto toDto(RealPriceRecord r, ScoreResult result) {
        return RealPriceMatchDto.builder()
                .recordId(r.getRecordId())
                .district(r.getDistrict())
                .address(r.getAddress())
                .transactionDate(r.getTransactionDate())
                .buildingType(r.getBuildingType())
                .totalAreaPing(r.getTotalAreaPing())
                .floorDesc(r.getFloorDesc())
                .totalFloor(r.getTotalFloor())
                .bedroomCount(r.getBedroomCount())
                .livingRoomCount(r.getLivingRoomCount())
                .bathroomCount(r.getBathroomCount())
                .houseAgeYear(r.getHouseAgeYear())
                .hasElevator(r.getHasElevator())
                .hasManagement(r.getHasManagement())
                .totalPriceWan(r.getTotalPriceWan())
                .parkingPriceWan(r.getParkingPriceWan())
                .parkingAreaPing(r.getParkingAreaPing())
                .pricePerPingWan(r.getPricePerPingWan())
                .similarityScore(result.score)
                .similarityNote(result.note)
                .build();
    }

    private record ScoreResult(int score, String note) {}
    private record ScoredRecord(RealPriceRecord record, ScoreResult result) {}
}
