package com.besthouse.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ScoreResultDto {
    private Long houseId;
    private String nickname;
    private String address;
    /** 加權總分，滿分 10 分 */
    private Double totalScore;
    /** key = memberDisplayName, value = 該成員的加權維度分 */
    private Map<String, Double> memberScores;
    private Integer rank;
}
