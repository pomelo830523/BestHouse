package com.besthouse.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MemberDto {
    private Long memberId;
    private String displayName;
    private String role;
    private BigDecimal weight;
    private Integer sortOrder;
}
