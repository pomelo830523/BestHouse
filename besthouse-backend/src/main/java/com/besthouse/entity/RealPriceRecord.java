package com.besthouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "REAL_PRICE_RECORD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealPriceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RECORD_ID")
    private Long recordId;

    @Column(name = "CITY_CODE", nullable = false, length = 2)
    private String cityCode;

    @Column(name = "DISTRICT", length = 50)
    private String district;

    @Column(name = "ADDRESS", length = 500)
    private String address;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "BUILDING_TYPE", length = 100)
    private String buildingType;

    @Column(name = "TOTAL_AREA_PING", precision = 10, scale = 2)
    private BigDecimal totalAreaPing;

    @Column(name = "FLOOR_DESC", length = 50)
    private String floorDesc;

    @Column(name = "FLOOR_NUM")
    private Integer floorNum;

    @Column(name = "TOTAL_FLOOR")
    private Integer totalFloor;

    @Column(name = "BEDROOM_COUNT")
    private Integer bedroomCount;

    @Column(name = "LIVING_ROOM_COUNT")
    private Integer livingRoomCount;

    @Column(name = "BATHROOM_COUNT")
    private Integer bathroomCount;

    @Column(name = "COMPLETED_YEAR")
    private Integer completedYear;

    @Column(name = "HOUSE_AGE_YEAR")
    private Integer houseAgeYear;

    @Column(name = "HAS_ELEVATOR")
    private Boolean hasElevator;

    @Column(name = "HAS_MANAGEMENT")
    private Boolean hasManagement;

    @Column(name = "TOTAL_PRICE_WAN", precision = 12, scale = 2)
    private BigDecimal totalPriceWan;

    @Column(name = "PARKING_PRICE_WAN", precision = 10, scale = 2)
    private BigDecimal parkingPriceWan;

    @Column(name = "PARKING_AREA_PING", precision = 8, scale = 2)
    private BigDecimal parkingAreaPing;

    @Column(name = "PRICE_PER_PING_WAN", precision = 10, scale = 2)
    private BigDecimal pricePerPingWan;

    @Column(name = "SYNCED_AT", nullable = false)
    private LocalDateTime syncedAt;
}
