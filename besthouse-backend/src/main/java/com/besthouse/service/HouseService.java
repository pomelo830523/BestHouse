package com.besthouse.service;

import com.besthouse.dto.HouseCreateDto;
import com.besthouse.dto.HouseDto;
import com.besthouse.entity.House;
import com.besthouse.entity.enums.HouseStatus;
import com.besthouse.entity.enums.ParkingType;
import com.besthouse.exception.ResourceNotFoundException;
import com.besthouse.repository.HouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HouseService {

    private final HouseRepository houseRepository;

    @Transactional(readOnly = true)
    public List<HouseDto> findAll() {
        return houseRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<HouseDto> findByListingUrl(String url) {
        return houseRepository.findByListingUrl(url).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public HouseDto findById(Long houseId) {
        return houseRepository.findById(houseId)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("找不到房屋 ID: " + houseId));
    }

    @Transactional
    public HouseDto create(HouseCreateDto dto) {
        House house = fromCreateDto(dto);
        House saved = houseRepository.save(house);
        log.info("新增房屋: id={}, nickname={}", saved.getHouseId(), saved.getNickname());
        return toDto(saved);
    }

    @Transactional
    public HouseDto update(Long houseId, HouseCreateDto dto) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到房屋 ID: " + houseId));

        house.setNickname(dto.getNickname());
        house.setAddress(dto.getAddress());
        house.setCommunityName(dto.getCommunityName());
        house.setBuilder(dto.getBuilder());
        house.setHouseAgeYear(dto.getHouseAgeYear());
        house.setFloor(dto.getFloor());
        house.setTotalFloor(dto.getTotalFloor());
        house.setUnitsPerFloor(dto.getUnitsPerFloor());
        house.setElevatorCount(dto.getElevatorCount());
        house.setWalkMetersToHsrZhubei(dto.getWalkMetersToHsrZhubei());
        house.setNearestStationToHsrZhubei(dto.getNearestStationToHsrZhubei());
        house.setWalkMetersToFengyuan(dto.getWalkMetersToFengyuan());
        house.setNearestStationToFengyuan(dto.getNearestStationToFengyuan());
        house.setWalkMetersToElementary(dto.getWalkMetersToElementary());
        house.setNearestElementarySchool(dto.getNearestElementarySchool());
        house.setWalkMetersToJuniorHigh(dto.getWalkMetersToJuniorHigh());
        house.setNearestJuniorHighSchool(dto.getNearestJuniorHighSchool());
        house.setBuildAreaPing(dto.getBuildAreaPing());
        house.setIndoorPing(dto.getIndoorPing());
        house.setBedroomCount(dto.getBedroomCount());
        house.setLivingRoomCount(dto.getLivingRoomCount());
        house.setBathroomCount(dto.getBathroomCount());
        house.setTotalPrice(dto.getTotalPrice());
        house.setParkingType(dto.getParkingType());
        house.setParkingPrice(dto.getParkingPrice());
        house.setParkingPing(dto.getParkingPing());
        house.setHasMotorcycleParking(dto.getHasMotorcycleParking());
        house.setMonthlyFee(dto.getMonthlyFee());
        house.setMonthlyRent(dto.getMonthlyRent());
        house.setListingUrl(dto.getListingUrl());
        house.setNote(dto.getNote());
        house.setHasVisited(dto.getHasVisited() != null ? dto.getHasVisited() : false);
        house.setDiscountPercent(dto.getDiscountPercent());
        house.setRegistryPricePerPingMin(dto.getRegistryPricePerPingMin());
        house.setRegistryPricePerPingMax(dto.getRegistryPricePerPingMax());
        house.setLatestRegistryPricePerPing(dto.getLatestRegistryPricePerPing());
        house.setHasMoldOrLeak(dto.getHasMoldOrLeak());
        house.setIsFloorLevelOk(dto.getIsFloorLevelOk());
        house.setIsDoorWindowOk(dto.getIsDoorWindowOk());
        house.setIsWaterPressureOk(dto.getIsWaterPressureOk());
        house.setElectricCapacity(dto.getElectricCapacity());
        house.setIsHaunted(dto.getIsHaunted());
        house.setIsSeaSand(dto.getIsSeaSand());
        house.setIsRadiation(dto.getIsRadiation());
        house.setHasIllegalConstruction(dto.getHasIllegalConstruction());
        house.setIsParkingLowestFloor(dto.getIsParkingLowestFloor());
        house.setFloodRisk(dto.getFloodRisk());
        house.setHasNuisanceFacility(dto.getHasNuisanceFacility());
        house.setNuisanceFacilityNote(dto.getNuisanceFacilityNote());
        house.setIsManagementOk(dto.getIsManagementOk());
        house.setManagementNote(dto.getManagementNote());
        house.setVisitDate(dto.getVisitDate());
        house.setVisitImpression(dto.getVisitImpression());

        log.info("更新房屋: id={}", houseId);
        return toDto(houseRepository.save(house));
    }

    @Transactional
    public void delete(Long houseId) {
        if (!houseRepository.existsById(houseId)) {
            throw new ResourceNotFoundException("找不到房屋 ID: " + houseId);
        }
        houseRepository.deleteById(houseId);
        log.info("刪除房屋: id={}", houseId);
    }

    @Transactional
    public HouseDto restoreHouse(Long houseId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到房屋 ID: " + houseId));
        house.setStatus(HouseStatus.ACTIVE);
        house.setEliminatedReason(null);
        log.info("恢復房屋: id={}", houseId);
        return toDto(houseRepository.save(house));
    }

    public HouseDto toDto(House house) {
        BigDecimal priceWithParking = calculatePricePerPing(house.getTotalPrice(), house.getBuildAreaPing());
        BigDecimal priceWithoutParking = calculatePricePerPingWithoutParking(
                house.getTotalPrice(), house.getParkingPrice(), house.getParkingPing(),
                house.getBuildAreaPing(), house.getParkingType());
        BigDecimal monthlyMortgage = calculateMonthlyMortgage(house.getTotalPrice());
        BigDecimal loanAmount = house.getTotalPrice() != null
                ? house.getTotalPrice().multiply(BigDecimal.valueOf(10000)).multiply(new BigDecimal("0.8"))
                : null;
        BigDecimal monthlyInterest = calculateMonthlyInterest(monthlyMortgage, loanAmount);
        BigDecimal interestToRentRatio = calculateInterestToRentRatio(monthlyInterest, house.getMonthlyRent());

        return HouseDto.builder()
                .houseId(house.getHouseId())
                .nickname(house.getNickname())
                .address(house.getAddress())
                .communityName(house.getCommunityName())
                .builder(house.getBuilder())
                .houseAgeYear(house.getHouseAgeYear())
                .floor(house.getFloor())
                .totalFloor(house.getTotalFloor())
                .unitsPerFloor(house.getUnitsPerFloor())
                .elevatorCount(house.getElevatorCount())
                .walkMetersToHsrZhubei(house.getWalkMetersToHsrZhubei())
                .nearestStationToHsrZhubei(house.getNearestStationToHsrZhubei())
                .walkMetersToFengyuan(house.getWalkMetersToFengyuan())
                .nearestStationToFengyuan(house.getNearestStationToFengyuan())
                .walkMetersToElementary(house.getWalkMetersToElementary())
                .nearestElementarySchool(house.getNearestElementarySchool())
                .walkMetersToJuniorHigh(house.getWalkMetersToJuniorHigh())
                .nearestJuniorHighSchool(house.getNearestJuniorHighSchool())
                .buildAreaPing(house.getBuildAreaPing())
                .indoorPing(house.getIndoorPing())
                .bedroomCount(house.getBedroomCount())
                .livingRoomCount(house.getLivingRoomCount())
                .bathroomCount(house.getBathroomCount())
                .totalPrice(house.getTotalPrice())
                .parkingType(house.getParkingType())
                .parkingPrice(house.getParkingPrice())
                .parkingPing(house.getParkingPing())
                .hasMotorcycleParking(house.getHasMotorcycleParking())
                .monthlyFee(house.getMonthlyFee())
                .monthlyRent(house.getMonthlyRent())
                .monthlyMortgage(monthlyMortgage)
                .monthlyInterest(monthlyInterest)
                .interestToRentRatio(interestToRentRatio)
                .listingUrl(house.getListingUrl())
                .note(house.getNote())
                .hasVisited(house.getHasVisited())
                .discountPercent(house.getDiscountPercent())
                .registryPricePerPingMin(house.getRegistryPricePerPingMin())
                .registryPricePerPingMax(house.getRegistryPricePerPingMax())
                .latestRegistryPricePerPing(house.getLatestRegistryPricePerPing())
                .status(house.getStatus())
                .eliminatedReason(house.getEliminatedReason())
                .warningReason(house.getWarningReason())
                .pricePerPingWithParking(priceWithParking)
                .pricePerPingWithoutParking(priceWithoutParking)
                .createdAt(house.getCreatedAt())
                .updatedAt(house.getUpdatedAt())
                .hasMoldOrLeak(house.getHasMoldOrLeak())
                .isFloorLevelOk(house.getIsFloorLevelOk())
                .isDoorWindowOk(house.getIsDoorWindowOk())
                .isWaterPressureOk(house.getIsWaterPressureOk())
                .electricCapacity(house.getElectricCapacity())
                .isHaunted(house.getIsHaunted())
                .isSeaSand(house.getIsSeaSand())
                .isRadiation(house.getIsRadiation())
                .hasIllegalConstruction(house.getHasIllegalConstruction())
                .isParkingLowestFloor(house.getIsParkingLowestFloor())
                .floodRisk(house.getFloodRisk())
                .hasNuisanceFacility(house.getHasNuisanceFacility())
                .nuisanceFacilityNote(house.getNuisanceFacilityNote())
                .isManagementOk(house.getIsManagementOk())
                .managementNote(house.getManagementNote())
                .visitDate(house.getVisitDate())
                .visitImpression(house.getVisitImpression())
                .build();
    }

    private House fromCreateDto(HouseCreateDto dto) {
        return House.builder()
                .nickname(dto.getNickname())
                .address(dto.getAddress())
                .communityName(dto.getCommunityName())
                .builder(dto.getBuilder())
                .houseAgeYear(dto.getHouseAgeYear())
                .floor(dto.getFloor())
                .totalFloor(dto.getTotalFloor())
                .unitsPerFloor(dto.getUnitsPerFloor())
                .elevatorCount(dto.getElevatorCount())
                .walkMetersToHsrZhubei(dto.getWalkMetersToHsrZhubei())
                .nearestStationToHsrZhubei(dto.getNearestStationToHsrZhubei())
                .walkMetersToFengyuan(dto.getWalkMetersToFengyuan())
                .nearestStationToFengyuan(dto.getNearestStationToFengyuan())
                .walkMetersToElementary(dto.getWalkMetersToElementary())
                .nearestElementarySchool(dto.getNearestElementarySchool())
                .walkMetersToJuniorHigh(dto.getWalkMetersToJuniorHigh())
                .nearestJuniorHighSchool(dto.getNearestJuniorHighSchool())
                .buildAreaPing(dto.getBuildAreaPing())
                .indoorPing(dto.getIndoorPing())
                .bedroomCount(dto.getBedroomCount())
                .livingRoomCount(dto.getLivingRoomCount())
                .bathroomCount(dto.getBathroomCount())
                .totalPrice(dto.getTotalPrice())
                .parkingType(dto.getParkingType())
                .parkingPrice(dto.getParkingPrice() != null ? dto.getParkingPrice() : BigDecimal.ZERO)
                .parkingPing(dto.getParkingPing())
                .hasMotorcycleParking(dto.getHasMotorcycleParking())
                .monthlyFee(dto.getMonthlyFee())
                .monthlyRent(dto.getMonthlyRent())
                .listingUrl(dto.getListingUrl())
                .note(dto.getNote())
                .hasVisited(dto.getHasVisited() != null ? dto.getHasVisited() : false)
                .discountPercent(dto.getDiscountPercent())
                .registryPricePerPingMin(dto.getRegistryPricePerPingMin())
                .registryPricePerPingMax(dto.getRegistryPricePerPingMax())
                .latestRegistryPricePerPing(dto.getLatestRegistryPricePerPing())
                .hasMoldOrLeak(dto.getHasMoldOrLeak())
                .isFloorLevelOk(dto.getIsFloorLevelOk())
                .isDoorWindowOk(dto.getIsDoorWindowOk())
                .isWaterPressureOk(dto.getIsWaterPressureOk())
                .electricCapacity(dto.getElectricCapacity())
                .isHaunted(dto.getIsHaunted())
                .isSeaSand(dto.getIsSeaSand())
                .isRadiation(dto.getIsRadiation())
                .hasIllegalConstruction(dto.getHasIllegalConstruction())
                .isParkingLowestFloor(dto.getIsParkingLowestFloor())
                .floodRisk(dto.getFloodRisk())
                .hasNuisanceFacility(dto.getHasNuisanceFacility())
                .nuisanceFacilityNote(dto.getNuisanceFacilityNote())
                .isManagementOk(dto.getIsManagementOk())
                .managementNote(dto.getManagementNote())
                .visitDate(dto.getVisitDate())
                .visitImpression(dto.getVisitImpression())
                .status(HouseStatus.ACTIVE)
                .build();
    }

    private BigDecimal calculatePricePerPing(BigDecimal totalPrice, BigDecimal buildAreaPing) {
        if (totalPrice == null || buildAreaPing == null || buildAreaPing.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return totalPrice.divide(buildAreaPing, 2, RoundingMode.HALF_UP);
    }

    /**
     * 計算每月房貸月付（本息平均攤還，30年，年利率2.6%，貸款8成）
     * 公式：M = P × r(1+r)^n / ((1+r)^n - 1)，n = 360
     */
    private BigDecimal calculateMonthlyMortgage(BigDecimal totalPrice) {
        if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        double principal = totalPrice.doubleValue() * 10000 * 0.8;
        double monthlyRate = 0.026 / 12;
        int months = 360;
        double factor = Math.pow(1 + monthlyRate, months);
        double monthly = principal * monthlyRate * factor / (factor - 1);
        return BigDecimal.valueOf(monthly).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * 計算每月純利息：月付 - 貸款總額/360
     */
    private BigDecimal calculateMonthlyInterest(BigDecimal monthlyMortgage, BigDecimal loanAmount) {
        if (monthlyMortgage == null || loanAmount == null) {
            return null;
        }
        BigDecimal monthlyPrincipal = loanAmount.divide(BigDecimal.valueOf(360), 0, RoundingMode.HALF_UP);
        return monthlyMortgage.subtract(monthlyPrincipal);
    }

    /**
     * 計算買房月息是租金的百分比：monthlyInterest / monthlyRent × 100
     */
    private BigDecimal calculateInterestToRentRatio(BigDecimal monthlyInterest, BigDecimal monthlyRent) {
        if (monthlyInterest == null || monthlyRent == null || monthlyRent.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return monthlyInterest.divide(monthlyRent, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal calculatePricePerPingWithoutParking(
            BigDecimal totalPrice, BigDecimal parkingPrice, BigDecimal parkingPing,
            BigDecimal buildAreaPing, ParkingType parkingType) {
        if (totalPrice == null || buildAreaPing == null || buildAreaPing.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        boolean hasParking = parkingType != null && parkingType != ParkingType.NONE;
        if (!hasParking || parkingPing == null || parkingPing.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        // 車位價未填時，以 車位坪 × 30萬 估算
        BigDecimal effectiveParkingPrice = (parkingPrice != null && parkingPrice.compareTo(BigDecimal.ZERO) > 0)
                ? parkingPrice
                : parkingPing.multiply(new BigDecimal("30"));
        BigDecimal netPrice = totalPrice.subtract(effectiveParkingPrice);
        BigDecimal netArea  = buildAreaPing.subtract(parkingPing);
        if (netArea.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return netPrice.divide(netArea, 2, RoundingMode.HALF_UP);
    }
}
