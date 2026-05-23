package com.besthouse.entity;

import com.besthouse.entity.enums.FloodRisk;
import com.besthouse.entity.enums.HouseStatus;
import com.besthouse.entity.enums.ParkingType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "HOUSE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class House {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HOUSE_ID")
    private Long houseId;

    @Column(name = "NICKNAME", nullable = false, length = 100)
    private String nickname;

    @Column(name = "ADDRESS", length = 500)
    private String address;

    @Column(name = "COMMUNITY_NAME", length = 200)
    private String communityName;

    @Column(name = "BUILDER", length = 200)
    private String builder;

    @Column(name = "HOUSE_AGE_YEAR")
    private Integer houseAgeYear;

    @Column(name = "FLOOR")
    private Integer floor;

    @Column(name = "TOTAL_FLOOR")
    private Integer totalFloor;

    /** 每層戶數 */
    @Column(name = "UNITS_PER_FLOOR")
    private Integer unitsPerFloor;

    /** 電梯數 */
    @Column(name = "ELEVATOR_COUNT")
    private Integer elevatorCount;

    /** 步行至最近站點公尺數（往竹北高鐵方向，含轉乘起點） */
    @Column(name = "WALK_METERS_TO_HSR_ZHUBEI")
    private Integer walkMetersToHsrZhubei;

    /** 對應 walkMetersToHsrZhubei 的最近站名，例：新莊火車站 */
    @Column(name = "NEAREST_STATION_TO_HSR_ZHUBEI", length = 100)
    private String nearestStationToHsrZhubei;

    /** 步行至最近站點公尺數（往新竹火車站方向，含轉乘起點） */
    @Column(name = "WALK_METERS_TO_FENGYUAN")
    private Integer walkMetersToFengyuan;

    /** 對應 walkMetersToFengyuan 的最近站名 */
    @Column(name = "NEAREST_STATION_TO_FENGYUAN", length = 100)
    private String nearestStationToFengyuan;

    /** 步行至最近國小公尺數 */
    @Column(name = "WALK_METERS_TO_ELEMENTARY")
    private Integer walkMetersToElementary;

    /** 最近國小校名 */
    @Column(name = "NEAREST_ELEMENTARY_SCHOOL", length = 100)
    private String nearestElementarySchool;

    /** 步行至最近國中公尺數 */
    @Column(name = "WALK_METERS_TO_JUNIOR_HIGH")
    private Integer walkMetersToJuniorHigh;

    /** 最近國中校名 */
    @Column(name = "NEAREST_JUNIOR_HIGH_SCHOOL", length = 100)
    private String nearestJuniorHighSchool;

    @Column(name = "BUILD_AREA_PING", precision = 10, scale = 2)
    private BigDecimal buildAreaPing;

    @Column(name = "INDOOR_PING", precision = 10, scale = 2)
    private BigDecimal indoorPing;

    @Column(name = "BEDROOM_COUNT")
    private Integer bedroomCount;

    @Column(name = "LIVING_ROOM_COUNT")
    private Integer livingRoomCount;

    @Column(name = "BATHROOM_COUNT")
    private Integer bathroomCount;

    @Column(name = "TOTAL_PRICE", precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "PARKING_TYPE", columnDefinition = "VARCHAR(30)")
    private ParkingType parkingType;

    @Column(name = "PARKING_PRICE", precision = 10, scale = 2)
    private BigDecimal parkingPrice;

    @Column(name = "PARKING_PING", precision = 10, scale = 2)
    private BigDecimal parkingPing;

    /** 有無機車位 */
    @Column(name = "HAS_MOTORCYCLE_PARKING")
    private Boolean hasMotorcycleParking;

    @Column(name = "MONTHLY_FEE", precision = 10, scale = 2)
    private BigDecimal monthlyFee;

    @Column(name = "MONTHLY_RENT", precision = 10, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "LISTING_URL", length = 1000)
    private String listingUrl;

    @Column(name = "NOTE", columnDefinition = "TEXT")
    private String note;

    @Column(name = "HAS_VISITED", nullable = false)
    @Builder.Default
    private Boolean hasVisited = false;

    @Column(name = "DISCOUNT_PERCENT", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    /** 過去一年實登不含車位每坪下限（萬/坪，使用者手填） */
    @Column(name = "REGISTRY_PRICE_PER_PING_MIN", precision = 10, scale = 2)
    private BigDecimal registryPricePerPingMin;

    /** 過去一年實登不含車位每坪上限（萬/坪，使用者手填） */
    @Column(name = "REGISTRY_PRICE_PER_PING_MAX", precision = 10, scale = 2)
    private BigDecimal registryPricePerPingMax;

    /** 最新一筆實登不含車位每坪（萬/坪，使用者手填） */
    @Column(name = "LATEST_REGISTRY_PRICE_PER_PING", precision = 10, scale = 2)
    private BigDecimal latestRegistryPricePerPing;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private HouseStatus status = HouseStatus.ACTIVE;

    @Column(name = "ELIMINATED_REASON", columnDefinition = "TEXT")
    private String eliminatedReason;

    // 看房評估欄位
    @Column(name = "HAS_MOLD_OR_LEAK")
    private Boolean hasMoldOrLeak;

    @Column(name = "IS_FLOOR_LEVEL_OK")
    private Boolean isFloorLevelOk;

    @Column(name = "IS_DOOR_WINDOW_OK")
    private Boolean isDoorWindowOk;

    @Column(name = "IS_WATER_PRESSURE_OK")
    private Boolean isWaterPressureOk;

    @Column(name = "ELECTRIC_CAPACITY")
    private Integer electricCapacity;

    @Column(name = "IS_HAUNTED")
    private Boolean isHaunted;

    @Column(name = "IS_SEA_SAND")
    private Boolean isSeaSand;

    @Column(name = "IS_RADIATION")
    private Boolean isRadiation;

    @Column(name = "HAS_ILLEGAL_CONSTRUCTION")
    private Boolean hasIllegalConstruction;

    @Column(name = "IS_PARKING_LOWEST_FLOOR")
    private Boolean isParkingLowestFloor;

    @Enumerated(EnumType.STRING)
    @Column(name = "FLOOD_RISK", columnDefinition = "VARCHAR(20)")
    private FloodRisk floodRisk;

    @Column(name = "HAS_NUISANCE_FACILITY")
    private Boolean hasNuisanceFacility;

    @Column(name = "NUISANCE_FACILITY_NOTE", columnDefinition = "TEXT")
    private String nuisanceFacilityNote;

    @Column(name = "IS_MANAGEMENT_OK")
    private Boolean isManagementOk;

    @Column(name = "MANAGEMENT_NOTE", columnDefinition = "TEXT")
    private String managementNote;

    @Column(name = "VISIT_DATE")
    private LocalDate visitDate;

    @Column(name = "VISIT_IMPRESSION", columnDefinition = "TEXT")
    private String visitImpression;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;
}
