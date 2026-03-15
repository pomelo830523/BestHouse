package com.besthouse.controller;

import com.besthouse.dto.FilterRuleDto;
import com.besthouse.service.FilterRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/filter-rules")
@RequiredArgsConstructor
public class FilterRuleController {

    private final FilterRuleService filterRuleService;

    @GetMapping
    public ResponseEntity<List<FilterRuleDto>> getAll() {
        return ResponseEntity.ok(filterRuleService.findAll());
    }

    @PostMapping
    public ResponseEntity<FilterRuleDto> create(@Valid @RequestBody FilterRuleDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(filterRuleService.create(dto));
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<FilterRuleDto> update(
            @PathVariable Long ruleId,
            @Valid @RequestBody FilterRuleDto dto) {
        return ResponseEntity.ok(filterRuleService.update(ruleId, dto));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> delete(@PathVariable Long ruleId) {
        filterRuleService.delete(ruleId);
        return ResponseEntity.noContent().build();
    }
}
