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

        // 先全部還原 ACTIVE
        allHouses.forEach(h -> {
            h.setStatus(HouseStatus.ACTIVE);
            h.setEliminatedReason(null);
        });

        List<ApplyFilterResultDto.EliminatedHouseDto> eliminatedList = new ArrayList<>();

        for (House house : allHouses) {
            List<String> failedReasons = new ArrayList<>();

            for (FilterRule rule : activeRules) {
                String reason = checkRule(house, rule);
                if (reason != null) {
                    failedReasons.add(reason);
                }
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

    /**
     * @return 不符合規則的說明，符合則回傳 null
     */
    private String checkRule(House house, FilterRule rule) {
        return switch (rule.getRuleType()) {
            case MAX_TOTAL_PRICE -> {
                if (house.getTotalPrice() != null && rule.getNumValue() != null
                        && house.getTotalPrice().compareTo(rule.getNumValue()) > 0) {
                    yield String.format("總價 %.0f 萬超過上限 %.0f 萬",
                            house.getTotalPrice(), rule.getNumValue());
                }
                yield null;
            }
            case MAX_PRICE_PER_PING -> {
                BigDecimal pricePerPing = calcPricePerPingWithoutParking(house);
                if (pricePerPing != null && rule.getNumValue() != null
                        && pricePerPing.compareTo(rule.getNumValue()) > 0) {
                    yield String.format("不含車位每坪 %.2f 萬超過上限 %.2f 萬",
                            pricePerPing, rule.getNumValue());
                }
                yield null;
            }
            case MAX_HOUSE_AGE -> {
                if (house.getHouseAgeYear() != null && rule.getNumValue() != null
                        && house.getHouseAgeYear() > rule.getNumValue().intValue()) {
                    yield String.format("屋齡 %d 年超過上限 %d 年",
                            house.getHouseAgeYear(), rule.getNumValue().intValue());
                }
                yield null;
            }
            case MIN_INDOOR_PING -> {
                if (house.getIndoorPing() != null && rule.getNumValue() != null
                        && house.getIndoorPing().compareTo(rule.getNumValue()) < 0) {
                    yield String.format("室內坪數 %.1f 坪低於下限 %.1f 坪",
                            house.getIndoorPing(), rule.getNumValue());
                }
                yield null;
            }
            case MIN_FLOOR -> {
                if (house.getFloor() != null && rule.getNumValue() != null
                        && house.getFloor() < rule.getNumValue().intValue()) {
                    yield String.format("樓層 %d F 低於下限 %d F",
                            house.getFloor(), rule.getNumValue().intValue());
                }
                yield null;
            }
            case EXCLUDE_PARKING_TYPE -> {
                if (house.getParkingType() != null && rule.getStrValue() != null) {
                    Set<String> excluded = Arrays.stream(rule.getStrValue().split(","))
                            .map(String::trim)
                            .collect(Collectors.toSet());
                    if (excluded.contains(house.getParkingType().name())) {
                        yield "車位類型「" + house.getParkingType().getDisplayName() + "」已被排除";
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
                    yield String.format("無車位，不符合車位坪數下限 %.2f 坪", rule.getNumValue());
                }
                if (house.getParkingPing() == null
                        || house.getParkingPing().compareTo(rule.getNumValue()) < 0) {
                    BigDecimal actual = house.getParkingPing() != null ? house.getParkingPing() : BigDecimal.ZERO;
                    yield String.format("車位坪數 %.2f 坪低於下限 %.2f 坪", actual, rule.getNumValue());
                }
                yield null;
            }
        };
    }

    private BigDecimal calcPricePerPingWithoutParking(House house) {
        if (house.getTotalPrice() == null || house.getBuildAreaPing() == null
                || house.getBuildAreaPing().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        boolean hasParking = house.getParkingType() != null && house.getParkingType() != ParkingType.NONE;
        BigDecimal effectiveParkingPrice = (hasParking && house.getParkingPrice() != null)
                ? house.getParkingPrice() : BigDecimal.ZERO;
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
