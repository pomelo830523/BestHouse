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
    /** 每層戶數 */
    private Integer unitsPerFloor;
    /** 電梯數 */
    private Integer elevatorCount;
    /** 步行至最近站點公尺數（往竹北高鐵） */
    private Integer walkMetersToHsrZhubei;
    /** 對應的最近站名 */
    @Size(max = 100)
    private String nearestStationToHsrZhubei;
    /** 步行至最近站點公尺數（往新竹火車站） */
    private Integer walkMetersToFengyuan;
    /** 對應的最近站名 */
    @Size(max = 100)
    private String nearestStationToFengyuan;
    /** 步行至最近國小公尺數 */
    private Integer walkMetersToElementary;
    /** 最近國小校名 */
    @Size(max = 100)
    private String nearestElementarySchool;
    /** 步行至最近國中公尺數 */
    private Integer walkMetersToJuniorHigh;
    /** 最近國中校名 */
    @Size(max = 100)
    private String nearestJuniorHighSchool;

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

    /** 過去一年實登不含車位每坪下限（萬/坪，使用者手填） */
    private BigDecimal registryPricePerPingMin;

    /** 過去一年實登不含車位每坪上限（萬/坪，使用者手填） */
    private BigDecimal registryPricePerPingMax;

    /** 最新一筆實登不含車位每坪（萬/坪，使用者手填） */
    private BigDecimal latestRegistryPricePerPing;

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
    private Boolean isParkingLowestFloor;
    private FloodRisk floodRisk;
    private Boolean hasNuisanceFacility;
    private String nuisanceFacilityNote;
    private Boolean isManagementOk;
    private String managementNote;
    private LocalDate visitDate;
    private String visitImpression;
}
