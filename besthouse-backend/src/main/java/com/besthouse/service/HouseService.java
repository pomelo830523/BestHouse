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
        house.setBuildAreaPing(dto.getBuildAreaPing());
        house.setIndoorPing(dto.getIndoorPing());
        house.setBedroomCount(dto.getBedroomCount());
        house.setLivingRoomCount(dto.getLivingRoomCount());
        house.setBathroomCount(dto.getBathroomCount());
        house.setTotalPrice(dto.getTotalPrice());
        house.setParkingType(dto.getParkingType());
        house.setParkingPrice(dto.getParkingPrice());
        house.setParkingPing(dto.getParkingPing());
        house.setMonthlyFee(dto.getMonthlyFee());
        house.setMonthlyRent(dto.getMonthlyRent());
        house.setListingUrl(dto.getListingUrl());
        house.setNote(dto.getNote());
        house.setHasVisited(dto.getHasVisited() != null ? dto.getHasVisited() : false);
        house.setDiscountPercent(dto.getDiscountPercent());
        house.setEstimatedRegistryPrice(dto.getEstimatedRegistryPrice());
        house.setHasMoldOrLeak(dto.getHasMoldOrLeak());
        house.setIsFloorLevelOk(dto.getIsFloorLevelOk());
        house.setIsDoorWindowOk(dto.getIsDoorWindowOk());
        house.setIsWaterPressureOk(dto.getIsWaterPressureOk());
        house.setElectricCapacity(dto.getElectricCapacity());
        house.setIsHaunted(dto.getIsHaunted());
        house.setIsSeaSand(dto.getIsSeaSand());
        house.setIsRadiation(dto.getIsRadiation());
        house.setHasIllegalConstruction(dto.getHasIllegalConstruction());
        house.setFloodRisk(dto.getFloodRisk());
        house.setHasNuisanceFacility(dto.getHasNuisanceFacility());
        house.setNuisanceFacilityNote(dto.getNuisanceFacilityNote());
        house.setIsManagementOk(dto.getIsManagementOk());
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
                .buildAreaPing(house.getBuildAreaPing())
                .indoorPing(house.getIndoorPing())
                .bedroomCount(house.getBedroomCount())
                .livingRoomCount(house.getLivingRoomCount())
                .bathroomCount(house.getBathroomCount())
                .totalPrice(house.getTotalPrice())
                .parkingType(house.getParkingType())
                .parkingPrice(house.getParkingPrice())
                .parkingPing(house.getParkingPing())
                .monthlyFee(house.getMonthlyFee())
                .monthlyRent(house.getMonthlyRent())
                .monthlyMortgage(monthlyMortgage)
                .monthlyInterest(monthlyInterest)
                .interestToRentRatio(interestToRentRatio)
                .listingUrl(house.getListingUrl())
                .note(house.getNote())
                .hasVisited(house.getHasVisited())
                .discountPercent(house.getDiscountPercent())
                .estimatedRegistryPrice(house.getEstimatedRegistryPrice())
                .status(house.getStatus())
                .eliminatedReason(house.getEliminatedReason())
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
                .floodRisk(house.getFloodRisk())
                .hasNuisanceFacility(house.getHasNuisanceFacility())
                .nuisanceFacilityNote(house.getNuisanceFacilityNote())
                .isManagementOk(house.getIsManagementOk())
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
                .buildAreaPing(dto.getBuildAreaPing())
                .indoorPing(dto.getIndoorPing())
                .bedroomCount(dto.getBedroomCount())
                .livingRoomCount(dto.getLivingRoomCount())
                .bathroomCount(dto.getBathroomCount())
                .totalPrice(dto.getTotalPrice())
                .parkingType(dto.getParkingType())
                .parkingPrice(dto.getParkingPrice() != null ? dto.getParkingPrice() : BigDecimal.ZERO)
                .parkingPing(dto.getParkingPing())
                .monthlyFee(dto.getMonthlyFee())
                .monthlyRent(dto.getMonthlyRent())
                .listingUrl(dto.getListingUrl())
                .note(dto.getNote())
                .hasVisited(dto.getHasVisited() != null ? dto.getHasVisited() : false)
                .discountPercent(dto.getDiscountPercent())
                .estimatedRegistryPrice(dto.getEstimatedRegistryPrice())
                .hasMoldOrLeak(dto.getHasMoldOrLeak())
                .isFloorLevelOk(dto.getIsFloorLevelOk())
                .isDoorWindowOk(dto.getIsDoorWindowOk())
                .isWaterPressureOk(dto.getIsWaterPressureOk())
                .electricCapacity(dto.getElectricCapacity())
                .isHaunted(dto.getIsHaunted())
                .isSeaSand(dto.getIsSeaSand())
                .isRadiation(dto.getIsRadiation())
                .hasIllegalConstruction(dto.getHasIllegalConstruction())
                .floodRisk(dto.getFloodRisk())
                .hasNuisanceFacility(dto.getHasNuisanceFacility())
                .nuisanceFacilityNote(dto.getNuisanceFacilityNote())
                .isManagementOk(dto.getIsManagementOk())
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
        BigDecimal effectiveParkingPrice = (hasParking && parkingPrice != null) ? parkingPrice : BigDecimal.ZERO;
        BigDecimal effectiveParkingPing  = (hasParking && parkingPing  != null) ? parkingPing  : BigDecimal.ZERO;
        BigDecimal netPrice = totalPrice.subtract(effectiveParkingPrice);
        BigDecimal netArea  = buildAreaPing.subtract(effectiveParkingPing);
        if (netArea.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return netPrice.divide(netArea, 2, RoundingMode.HALF_UP);
    }
}
