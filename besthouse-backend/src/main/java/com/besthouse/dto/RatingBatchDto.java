package com.besthouse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RatingBatchDto {

    @NotNull
    @Valid
    private List<RatingEntryDto> ratings;
}
