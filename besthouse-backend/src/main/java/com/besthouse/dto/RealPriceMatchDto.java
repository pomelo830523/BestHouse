package com.besthouse.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class RealPriceMatchDto {
    private Long recordId;
    private String district;
    private String address;
    private LocalDate transactionDate;
    private String buildingType;
    private BigDecimal totalAreaPing;
    private String floorDesc;
    private Integer totalFloor;
    private Integer bedroomCount;
    private Integer livingRoomCount;
    private Integer bathroomCount;
    private Integer houseAgeYear;
    private Boolean hasElevator;
    private Boolean hasManagement;
    private BigDecimal totalPriceWan;
    private BigDecimal parkingPriceWan;
    private BigDecimal parkingAreaPing;
    private BigDecimal pricePerPingWan;
    /** 相似度分數（0–100） */
    private int similarityScore;
    /** 相似度說明 */
    private String similarityNote;
}
