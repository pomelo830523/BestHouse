package com.besthouse.dto;

import com.besthouse.entity.enums.FloodRisk;
import com.besthouse.entity.enums.ParkingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class HouseCreateDto {

    @NotBlank(message = "房屋代號不可空白")
    @Size(max = 100)
    private String nickname;

    @Size(max = 500)
    private String address;

    @Size(max = 200)
    private String communityName;

    @Size(max = 200)
    private String builder;

    private Integer houseAgeYear;
    private Integer floor;
    private Integer totalFloor;

    @Positive(message = "總坪必須大於 0")
    private BigDecimal buildAreaPing;

    @Positive(message = "室內坪數必須大於 0")
    private BigDecimal indoorPing;

    private Integer bedroomCount;
    private Integer livingRoomCount;
    private Integer bathroomCount;

    @NotNull(message = "總價不可為空")
    @Positive(message = "總價必須大於 0")
    private BigDecimal totalPrice;

    private ParkingType parkingType;

    private BigDecimal parkingPrice;

    private BigDecimal parkingPing;

    private BigDecimal monthlyFee;

    private BigDecimal monthlyRent;

    private String listingUrl;
    private String note;

    private Boolean hasVisited;

    private BigDecimal discountPercent;

    private BigDecimal estimatedRegistryPrice;

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
