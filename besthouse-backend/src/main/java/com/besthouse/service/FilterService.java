package com.besthouse.service;

import com.besthouse.dto.ApplyFilterResultDto;
import com.besthouse.entity.FilterRule;
import com.besthouse.entity.House;
import com.besthouse.entity.enums.HouseStatus;
import com.besthouse.entity.enums.ParkingType;
import com.besthouse.repository.FilterRuleRepository;
import com.besthouse.repository.HouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilterService {

    private final HouseRepository houseRepository;
    private final FilterRuleRepository filterRuleRepository;

    /**
     * 套用所有啟用中的篩選規則到所有房屋，重新標記淘汰狀態。
     * 每次執行前先把全部房屋還原成 ACTIVE，再重新判斷。
     */
    @Transactional
    public ApplyFilterResultDto applyFilters() {
        List<House> allHouses = houseRepository.findAll();
        List<FilterRule> activeRules = filterRuleRepository.findByIsActiveTrueOrderByRuleId();

        // 先全部還原 ACTIVE、清空警告/淘汰原因
        allHouses.forEach(h -> {
            h.setStatus(HouseStatus.ACTIVE);
            h.setEliminatedReason(null);
            h.setWarningReason(null);
        });

        List<ApplyFilterResultDto.EliminatedHouseDto> eliminatedList = new ArrayList<>();

        for (House house : allHouses) {
            List<String> failedReasons = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            for (FilterRule rule : activeRules) {
                RuleCheckResult result = checkRule(house, rule);
                if (result == null) continue;
                if (result.warning()) {
                    warnings.add(result.message());
                } else {
                    failedReasons.add(result.message());
                }
            }

            if (!warnings.isEmpty()) {
                house.setWarningReason(String.join("；", warnings));
            }

            if (!failedReasons.isEmpty()) {
                house.setStatus(HouseStatus.ELIMINATED);
                house.setEliminatedReason(String.join("；", failedReasons));
                eliminatedList.add(ApplyFilterResultDto.EliminatedHouseDto.builder()
                        .houseId(house.getHouseId())
                        .nickname(house.getNickname())
                        .reason(house.getEliminatedReason())
                        .build());
            }
        }

        houseRepository.saveAll(allHouses);
        log.info("套用篩選規則完成: 共 {} 間，淘汰 {} 間", allHouses.size(), eliminatedList.size());

        return ApplyFilterResultDto.builder()
                .totalHouses(allHouses.size())
                .eliminatedCount(eliminatedList.size())
                .activeCount(allHouses.size() - eliminatedList.size())
                .eliminatedHouses(eliminatedList)
                .build();
    }

    /** 規則檢查結果：message + 是否為警告（true = 警告不淘汰，false = 淘汰） */
    private record RuleCheckResult(String message, boolean warning) {
        static RuleCheckResult eliminate(String msg) { return new RuleCheckResult(msg, false); }
        static RuleCheckResult warn(String msg)      { return new RuleCheckResult(msg, true); }
    }

    /**
     * @return 不符合規則的結果，符合則回傳 null
     */
    private RuleCheckResult checkRule(House house, FilterRule rule) {
        return switch (rule.getRuleType()) {
            case MAX_TOTAL_PRICE -> {
                if (house.getTotalPrice() != null && rule.getNumValue() != null
                        && house.getTotalPrice().compareTo(rule.getNumValue()) > 0) {
                    yield RuleCheckResult.eliminate(String.format("總價 %.0f 萬超過上限 %.0f 萬",
                            house.getTotalPrice(), rule.getNumValue()));
                }
                yield null;
            }
            case MAX_PRICE_PER_PING -> {
                BigDecimal pricePerPing = calcPricePerPingWithoutParking(house);
                if (pricePerPing != null && rule.getNumValue() != null
                        && pricePerPing.compareTo(rule.getNumValue()) > 0) {
                    yield RuleCheckResult.eliminate(String.format("不含車位每坪 %.2f 萬超過上限 %.2f 萬",
                            pricePerPing, rule.getNumValue()));
                }
                yield null;
            }
            case MAX_HOUSE_AGE -> {
                if (house.getHouseAgeYear() != null && rule.getNumValue() != null
                        && house.getHouseAgeYear() > rule.getNumValue().intValue()) {
                    yield RuleCheckResult.eliminate(String.format("屋齡 %d 年超過上限 %d 年",
                            house.getHouseAgeYear(), rule.getNumValue().intValue()));
                }
                yield null;
            }
            case MIN_INDOOR_PING -> {
                if (house.getIndoorPing() != null && rule.getNumValue() != null
                        && house.getIndoorPing().compareTo(rule.getNumValue()) < 0) {
                    yield RuleCheckResult.eliminate(String.format("室內坪數 %.1f 坪低於下限 %.1f 坪",
                            house.getIndoorPing(), rule.getNumValue()));
                }
                yield null;
            }
            case MIN_FLOOR -> {
                if (house.getFloor() != null && rule.getNumValue() != null
                        && house.getFloor() < rule.getNumValue().intValue()) {
                    yield RuleCheckResult.eliminate(String.format("樓層 %d F 低於下限 %d F",
                            house.getFloor(), rule.getNumValue().intValue()));
                }
                yield null;
            }
            case MAX_FLOOR -> {
                if (house.getFloor() != null && rule.getNumValue() != null
                        && house.getFloor() > rule.getNumValue().intValue()) {
                    yield RuleCheckResult.eliminate(String.format("樓層 %d F 超過上限 %d F",
                            house.getFloor(), rule.getNumValue().intValue()));
                }
                yield null;
            }
            case EXCLUDE_PARKING_TYPE -> {
                if (house.getParkingType() != null && rule.getStrValue() != null) {
                    Set<String> excluded = Arrays.stream(rule.getStrValue().split(","))
                            .map(String::trim)
                            .collect(Collectors.toSet());
                    if (excluded.contains(house.getParkingType().name())) {
                        yield RuleCheckResult.eliminate("車位類型「" + house.getParkingType().getDisplayName() + "」已被排除");
                    }
                }
                yield null;
            }
            case MIN_PARKING_PING -> {
                if (rule.getNumValue() == null) {
                    yield null;
                }
                boolean hasParking = house.getParkingType() != null
                        && house.getParkingType() != ParkingType.NONE;
                if (!hasParking) {
                    yield RuleCheckResult.eliminate(String.format("無車位，不符合車位坪數下限 %.2f 坪", rule.getNumValue()));
                }
                if (house.getParkingPing() == null
                        || house.getParkingPing().compareTo(rule.getNumValue()) < 0) {
                    BigDecimal actual = house.getParkingPing() != null ? house.getParkingPing() : BigDecimal.ZERO;
                    yield RuleCheckResult.eliminate(String.format("車位坪數 %.2f 坪低於下限 %.2f 坪", actual, rule.getNumValue()));
                }
                yield null;
            }
            case MAX_WALK_METERS_TO_HSR_ZHUBEI -> {
                if (rule.getNumValue() == null || house.getWalkMetersToHsrZhubei() == null) {
                    yield null;
                }
                int actual = house.getWalkMetersToHsrZhubei();
                if (BigDecimal.valueOf(actual).compareTo(rule.getNumValue()) > 0) {
                    yield RuleCheckResult.eliminate(String.format("步行至竹北高鐵 %d 公尺超過上限 %d 公尺",
                            actual, rule.getNumValue().intValue()));
                }
                yield null;
            }
            case MAX_WALK_METERS_TO_FENGYUAN -> {
                if (rule.getNumValue() == null || house.getWalkMetersToFengyuan() == null) {
                    yield null;
                }
                int actual = house.getWalkMetersToFengyuan();
                if (BigDecimal.valueOf(actual).compareTo(rule.getNumValue()) > 0) {
                    yield RuleCheckResult.eliminate(String.format("步行至新竹火車站 %d 公尺超過上限 %d 公尺",
                            actual, rule.getNumValue().intValue()));
                }
                yield null;
            }
            case MAX_WALK_METERS_TO_ELEMENTARY -> {
                if (rule.getNumValue() == null || house.getWalkMetersToElementary() == null) {
                    yield null;
                }
                int actual = house.getWalkMetersToElementary();
                if (BigDecimal.valueOf(actual).compareTo(rule.getNumValue()) > 0) {
                    String name = house.getNearestElementarySchool() != null
                            ? house.getNearestElementarySchool() : "最近國小";
                    yield RuleCheckResult.eliminate(String.format("步行至%s %d 公尺超過上限 %d 公尺",
                            name, actual, rule.getNumValue().intValue()));
                }
                yield null;
            }
            case MAX_WALK_METERS_TO_JUNIOR_HIGH -> {
                if (rule.getNumValue() == null || house.getWalkMetersToJuniorHigh() == null) {
                    yield null;
                }
                int actual = house.getWalkMetersToJuniorHigh();
                if (BigDecimal.valueOf(actual).compareTo(rule.getNumValue()) > 0) {
                    String name = house.getNearestJuniorHighSchool() != null
                            ? house.getNearestJuniorHighSchool() : "最近國中";
                    yield RuleCheckResult.eliminate(String.format("步行至%s %d 公尺超過上限 %d 公尺",
                            name, actual, rule.getNumValue().intValue()));
                }
                yield null;
            }
            case EXCLUDE_VISIT_ISSUES -> {
                // 舊規則：所有看房問題（含致命+可修）一律淘汰；保留供向下相容
                if (!Boolean.TRUE.equals(house.getHasVisited())) {
                    yield null;
                }
                List<String> issues = new ArrayList<>();
                issues.addAll(collectFatalVisitIssues(house));
                issues.addAll(collectRepairableVisitIssues(house));
                if (issues.isEmpty()) {
                    yield null;
                }
                yield RuleCheckResult.eliminate("看房問題：" + String.join("、", issues));
            }
            case EXCLUDE_FATAL_VISIT_ISSUES -> {
                if (!Boolean.TRUE.equals(house.getHasVisited())) {
                    yield null;
                }
                List<String> issues = collectFatalVisitIssues(house);
                if (issues.isEmpty()) {
                    yield null;
                }
                yield RuleCheckResult.eliminate("致命缺陷：" + String.join("、", issues));
            }
            case WARN_REPAIRABLE_VISIT_ISSUES -> {
                if (!Boolean.TRUE.equals(house.getHasVisited())) {
                    yield null;
                }
                List<String> issues = collectRepairableVisitIssues(house);
                if (issues.isEmpty()) {
                    yield null;
                }
                yield RuleCheckResult.warn("可修缮缺陷：" + String.join("、", issues));
            }
            case MAX_HOUSEHOLD_PER_ELEVATOR_RATIO -> {
                if (rule.getNumValue() == null) {
                    yield null;
                }
                Integer units = house.getUnitsPerFloor();
                Integer elevators = house.getElevatorCount();
                // 任一欄位未填則跳過判斷（避免誤殺資料不全的房屋）
                if (units == null || elevators == null || elevators <= 0) {
                    yield null;
                }
                BigDecimal ratio = BigDecimal.valueOf(units)
                        .divide(BigDecimal.valueOf(elevators), 2, RoundingMode.HALF_UP);
                if (ratio.compareTo(rule.getNumValue()) > 0) {
                    yield RuleCheckResult.eliminate(String.format("戶/梯比 %.2f（%d 戶 / %d 梯）超過上限 %.2f",
                            ratio, units, elevators, rule.getNumValue()));
                }
                yield null;
            }
        };
    }

    /** 致命缺陷：本質無法改變，觸發即淘汰 */
    private List<String> collectFatalVisitIssues(House house) {
        List<String> issues = new ArrayList<>();
        if (Boolean.TRUE.equals(house.getIsHaunted())) issues.add("凶宅");
        if (Boolean.TRUE.equals(house.getIsSeaSand())) issues.add("海砂屋");
        if (Boolean.TRUE.equals(house.getIsRadiation())) issues.add("輻射屋");
        if (Boolean.TRUE.equals(house.getHasIllegalConstruction())) issues.add("違建");
        if (Boolean.TRUE.equals(house.getHasNuisanceFacility())) issues.add("嫌惡設施");
        if (house.getFloodRisk() == com.besthouse.entity.enums.FloodRisk.HIGH) issues.add("淹水高風險");
        return issues;
    }

    /** 可修缮缺陷：裝潢/維修/磨合可解，只警告不淘汰 */
    private List<String> collectRepairableVisitIssues(House house) {
        List<String> issues = new ArrayList<>();
        if (Boolean.TRUE.equals(house.getHasMoldOrLeak())) issues.add("發霉/漏水");
        if (Boolean.FALSE.equals(house.getIsFloorLevelOk())) issues.add("地板不平");
        if (Boolean.FALSE.equals(house.getIsDoorWindowOk())) issues.add("門窗異常");
        if (Boolean.FALSE.equals(house.getIsWaterPressureOk())) issues.add("水壓異常");
        if (Boolean.TRUE.equals(house.getIsParkingLowestFloor())) issues.add("車位最低層");
        if (Boolean.FALSE.equals(house.getIsManagementOk())) issues.add("管理委員會NG");
        return issues;
    }

    private BigDecimal calcPricePerPingWithoutParking(House house) {
        if (house.getTotalPrice() == null || house.getBuildAreaPing() == null
                || house.getBuildAreaPing().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        boolean hasParking = house.getParkingType() != null && house.getParkingType() != ParkingType.NONE;
        BigDecimal effectiveParkingPrice = house.getParkingPing().multiply(BigDecimal.valueOf(30));
        BigDecimal effectiveParkingPing  = (hasParking && house.getParkingPing()  != null)
                ? house.getParkingPing()  : BigDecimal.ZERO;
        BigDecimal netPrice = house.getTotalPrice().subtract(effectiveParkingPrice);
        BigDecimal netArea  = house.getBuildAreaPing().subtract(effectiveParkingPing);
        if (netArea.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return netPrice.divide(netArea, 2, RoundingMode.HALF_UP);
    }
}
