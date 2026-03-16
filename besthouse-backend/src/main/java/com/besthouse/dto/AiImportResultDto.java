package com.besthouse.dto;

import com.besthouse.entity.enums.ParkingType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiImportResultDto {

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
    private BigDecimal monthlyFee;
    private String listingUrl;
}
