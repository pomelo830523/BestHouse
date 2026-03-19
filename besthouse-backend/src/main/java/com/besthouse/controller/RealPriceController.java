package com.besthouse.controller;

import com.besthouse.dto.RealPriceMatchDto;
import com.besthouse.service.RealPriceMatchService;
import com.besthouse.service.RealPriceSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/real-price")
@RequiredArgsConstructor
public class RealPriceController {

    private final RealPriceSyncService syncService;
    private final RealPriceMatchService matchService;

    /**
     * 手動觸發同步新竹縣市實價登錄資料
     * POST /api/real-price/sync          → 同步全部（新竹縣 + 新竹市）
     * POST /api/real-price/sync?city=J   → 只同步新竹縣
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> sync(
            @RequestParam(required = false) String city) {
        try {
            Map<String, Integer> result;
            if (city != null && !city.isBlank()) {
                int count = syncService.sync(city.toUpperCase());
                result = Map.of(city.toUpperCase(), count);
            } else {
                result = syncService.syncAll();
            }
            int total = result.values().stream().mapToInt(Integer::intValue).sum();
            log.info("實價登錄同步完成，共 {} 筆", total);
            return ResponseEntity.ok(Map.of("success", true, "counts", result, "total", total));
        } catch (Exception e) {
            log.error("實價登錄同步失敗", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 查詢與指定房屋最相近的實價登錄成交紀錄（最多 10 筆）
     * GET /api/real-price/matches/{houseId}
     */
    @GetMapping("/matches/{houseId}")
    public ResponseEntity<List<RealPriceMatchDto>> getMatches(@PathVariable Long houseId) {
        List<RealPriceMatchDto> matches = matchService.findMatches(houseId);
        return ResponseEntity.ok(matches);
    }
}
