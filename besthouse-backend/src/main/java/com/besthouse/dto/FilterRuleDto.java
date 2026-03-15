package com.besthouse.dto;

import com.besthouse.entity.enums.FilterRuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FilterRuleDto {
    private Long ruleId;

    @NotBlank(message = "規則名稱不可空白")
    private String ruleName;

    @NotNull(message = "規則類型不可為空")
    private FilterRuleType ruleType;

    private BigDecimal numValue;
    private String strValue;
    private Boolean isActive;
}
