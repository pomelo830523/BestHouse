package com.besthouse.controller;

import com.besthouse.dto.ApplyFilterResultDto;
import com.besthouse.dto.HouseCreateDto;
import com.besthouse.dto.HouseDto;
import com.besthouse.service.FilterService;
import com.besthouse.service.HouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
public class HouseController {

    private final HouseService houseService;
    private final FilterService filterService;

    @GetMapping
    public ResponseEntity<List<HouseDto>> getAll() {
        return ResponseEntity.ok(houseService.findAll());
    }

    @GetMapping("/{houseId}")
    public ResponseEntity<HouseDto> getById(@PathVariable Long houseId) {
        return ResponseEntity.ok(houseService.findById(houseId));
    }

    @GetMapping("/by-url")
    public ResponseEntity<HouseDto> getByUrl(@RequestParam String url) {
        return houseService.findByListingUrl(url)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<HouseDto> create(@Valid @RequestBody HouseCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(houseService.create(dto));
    }

    @PutMapping("/{houseId}")
    public ResponseEntity<HouseDto> update(
            @PathVariable Long houseId,
            @Valid @RequestBody HouseCreateDto dto) {
        return ResponseEntity.ok(houseService.update(houseId, dto));
    }

    @DeleteMapping("/{houseId}")
    public ResponseEntity<Void> delete(@PathVariable Long houseId) {
        houseService.delete(houseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{houseId}/restore")
    public ResponseEntity<HouseDto> restore(@PathVariable Long houseId) {
        return ResponseEntity.ok(houseService.restoreHouse(houseId));
    }

    @PostMapping("/apply-filters")
    public ResponseEntity<ApplyFilterResultDto> applyFilters() {
        return ResponseEntity.ok(filterService.applyFilters());
    }
}
