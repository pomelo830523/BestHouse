package com.besthouse.service;

import com.besthouse.dto.FilterRuleDto;
import com.besthouse.entity.FilterRule;
import com.besthouse.exception.ResourceNotFoundException;
import com.besthouse.repository.FilterRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilterRuleService {

    private final FilterRuleRepository filterRuleRepository;

    @Transactional(readOnly = true)
    public List<FilterRuleDto> findAll() {
        return filterRuleRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public FilterRuleDto create(FilterRuleDto dto) {
        FilterRule rule = FilterRule.builder()
                .ruleName(dto.getRuleName())
                .ruleType(dto.getRuleType())
                .numValue(dto.getNumValue())
                .strValue(dto.getStrValue())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
        FilterRule saved = filterRuleRepository.save(rule);
        log.info("新增篩選規則: id={}, type={}", saved.getRuleId(), saved.getRuleType());
        return toDto(saved);
    }

    @Transactional
    public FilterRuleDto update(Long ruleId, FilterRuleDto dto) {
        FilterRule rule = filterRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到篩選規則 ID: " + ruleId));
        rule.setRuleName(dto.getRuleName());
        rule.setRuleType(dto.getRuleType());
        rule.setNumValue(dto.getNumValue());
        rule.setStrValue(dto.getStrValue());
        if (dto.getIsActive() != null) {
            rule.setIsActive(dto.getIsActive());
        }
        return toDto(filterRuleRepository.save(rule));
    }

    @Transactional
    public void delete(Long ruleId) {
        if (!filterRuleRepository.existsById(ruleId)) {
            throw new ResourceNotFoundException("找不到篩選規則 ID: " + ruleId);
        }
        filterRuleRepository.deleteById(ruleId);
        log.info("刪除篩選規則: id={}", ruleId);
    }

    private FilterRuleDto toDto(FilterRule rule) {
        return FilterRuleDto.builder()
                .ruleId(rule.getRuleId())
                .ruleName(rule.getRuleName())
                .ruleType(rule.getRuleType())
                .numValue(rule.getNumValue())
                .strValue(rule.getStrValue())
                .isActive(rule.getIsActive())
                .build();
    }
}
