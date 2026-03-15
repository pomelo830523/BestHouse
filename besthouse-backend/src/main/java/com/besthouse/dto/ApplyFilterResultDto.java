package com.besthouse.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ApplyFilterResultDto {
    private int totalHouses;
    private int eliminatedCount;
    private int activeCount;
    private List<EliminatedHouseDto> eliminatedHouses;

    @Data
    @Builder
    public static class EliminatedHouseDto {
        private Long houseId;
        private String nickname;
        private String reason;
    }
}
