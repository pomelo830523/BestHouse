package com.besthouse.dto;

import com.besthouse.entity.enums.FloodRisk;
import com.besthouse.entity.enums.HouseStatus;
import com.besthouse.entity.enums.ParkingType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class HouseDto {
    private Long houseId;
    private String nickname;
    private String address;
    private String communityName;
    private String builder;
    private Integer houseAgeYear;
    private Integer floor;
    private Integer totalFloor;
    private BigDecimal buildAreaPing;
    private BigDecimal indoorPing;
    private Integer bedroomCount;
    private Integer livingRoomCount;
    private Integer bathroomCount;
    private BigDecimal totalPrice;
    private ParkingType parkingType;
    private BigDecimal parkingPrice;
    private BigDecimal parkingPing;
    private BigDecimal monthlyFee;
    private String listingUrl;
    private String note;
    private Boolean hasVisited;
    private BigDecimal discountPercent;
    private BigDecimal estimatedRegistryPrice;
    private HouseStatus status;
    private String eliminatedReason;
    /** 含車位每坪售價（自動計算） */
    private BigDecimal pricePerPingWithParking;
    /** 不含車位每坪售價（自動計算） */
    private BigDecimal pricePerPingWithoutParking;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 看房評估欄位
    private Boolean hasMoldOrLeak;
    private Boolean isFloorLevelOk;
    private Boolean isDoorWindowOk;
    private Boolean isWaterPressureOk;
    private Integer electricCapacity;
    private Boolean isHaunted;
    private Boolean isSeaSand;
    private Boolean isRadiation;
    private Boolean hasIllegalConstruction;
    private FloodRisk floodRisk;
    private Boolean hasNuisanceFacility;
    private String nuisanceFacilityNote;
    private Boolean isManagementOk;
    private LocalDate visitDate;
    private String visitImpression;
}
