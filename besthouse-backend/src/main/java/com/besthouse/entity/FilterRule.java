package com.besthouse.entity;

import com.besthouse.entity.enums.FilterRuleType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "FILTER_RULE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RULE_ID")
    private Long ruleId;

    @Column(name = "RULE_NAME", nullable = false, length = 200)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "RULE_TYPE", nullable = false, columnDefinition = "VARCHAR(50)")
    private FilterRuleType ruleType;

    @Column(name = "NUM_VALUE", precision = 12, scale = 2)
    private BigDecimal numValue;

    @Column(name = "STR_VALUE", length = 500)
    private String strValue;

    @Column(name = "IS_ACTIVE", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
