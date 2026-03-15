package com.besthouse.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RatingViewDto {
    private Long ratingId;
    private Long memberId;
    private String memberName;
    private Long dimensionId;
    private String dimensionName;
    private Integer score;
}
