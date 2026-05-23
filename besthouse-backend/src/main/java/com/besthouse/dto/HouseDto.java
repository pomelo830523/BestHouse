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
    /** 每層戶數 */
    private Integer unitsPerFloor;
    /** 電梯數 */
    private Integer elevatorCount;
    /** 步行至最近站點公尺數（往竹北高鐵） */
    private Integer walkMetersToHsrZhubei;
    /** 對應的最近站名 */
    private String nearestStationToHsrZhubei;
    /** 步行至最近站點公尺數（往新竹火車站） */
    private Integer walkMetersToFengyuan;
    /** 對應的最近站名 */
    private String nearestStationToFengyuan;
    /** 步行至最近國小公尺數 */
    private Integer walkMetersToElementary;
    /** 最近國小校名 */
    private String nearestElementarySchool;
    /** 步行至最近國中公尺數 */
    private Integer walkMetersToJuniorHigh;
    /** 最近國中校名 */
    private String nearestJuniorHighSchool;
    private BigDecimal buildAreaPing;
    private BigDecimal indoorPing;
    private Integer bedroomCount;
    private Integer livingRoomCount;
    private Integer bathroomCount;
    private BigDecimal totalPrice;
    private ParkingType parkingType;
    private BigDecimal parkingPrice;
    private BigDecimal parkingPing;
    /** 有無機車位 */
    private Boolean hasMotorcycleParking;
    private BigDecimal monthlyFee;
    private BigDecimal monthlyRent;
    /** 每月房貸月付（元，貸款8成 年利率2.6% 本息平均攤還30年，自動計算） */
    private BigDecimal monthlyMortgage;
    /** 每月純利息（月付 - 本金/360，元，自動計算） */
    private BigDecimal monthlyInterest;
    /** 買房月息是租金的百分比（monthlyInterest / monthlyRent × 100，自動計算） */
    private BigDecimal interestToRentRatio;
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
    private HouseStatus status;
    private String eliminatedReason;
    /** 可修缮缺陷觸發的警告訊息（status 仍 ACTIVE） */
    private String warningReason;
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
    private Boolean isParkingLowestFloor;
    private FloodRisk floodRisk;
    private Boolean hasNuisanceFacility;
    private String nuisanceFacilityNote;
    private Boolean isManagementOk;
    private String managementNote;
    private LocalDate visitDate;
    private String visitImpression;
}
