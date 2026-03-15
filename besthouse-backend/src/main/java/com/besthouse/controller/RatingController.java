package com.besthouse.controller;

import com.besthouse.dto.RatingBatchDto;
import com.besthouse.dto.RatingViewDto;
import com.besthouse.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @GetMapping("/house/{houseId}")
    public ResponseEntity<List<RatingViewDto>> getByHouse(@PathVariable Long houseId) {
        return ResponseEntity.ok(ratingService.getRatingsByHouse(houseId));
    }

    @PostMapping("/house/{houseId}")
    public ResponseEntity<List<RatingViewDto>> saveRatings(
            @PathVariable Long houseId,
            @Valid @RequestBody RatingBatchDto dto) {
        return ResponseEntity.ok(ratingService.saveRatings(houseId, dto));
    }
}
