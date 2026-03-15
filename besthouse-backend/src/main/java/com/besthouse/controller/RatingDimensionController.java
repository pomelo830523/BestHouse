package com.besthouse.controller;

import com.besthouse.dto.RatingDimensionDto;
import com.besthouse.entity.RatingDimension;
import com.besthouse.repository.RatingDimensionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dimensions")
@RequiredArgsConstructor
public class RatingDimensionController {

    private final RatingDimensionRepository dimensionRepository;

    @GetMapping
    public ResponseEntity<List<RatingDimensionDto>> getAll() {
        List<RatingDimensionDto> result = dimensionRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(d -> RatingDimensionDto.builder()
                        .dimensionId(d.getDimensionId())
                        .dimensionName(d.getDimensionName())
                        .description(d.getDescription())
                        .weight(d.getWeight())
                        .sortOrder(d.getSortOrder())
                        .isActive(d.getIsActive())
                        .build())
                .toList();
        return ResponseEntity.ok(result);
    }
}
