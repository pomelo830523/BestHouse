package com.besthouse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RatingEntryDto {

    @NotNull
    private Long memberId;

    @NotNull
    private Long dimensionId;

    @NotNull
    @Min(value = 1, message = "評分最低 1 分")
    @Max(value = 10, message = "評分最高 10 分")
    private Integer score;
}
