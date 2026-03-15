package com.besthouse.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RatingDimensionDto {
    private Long dimensionId;
    private String dimensionName;
    private String description;
    private BigDecimal weight;
    private Integer sortOrder;
    private Boolean isActive;
}
