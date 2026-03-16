package com.besthouse.controller;

import com.besthouse.dto.AiImportResultDto;
import com.besthouse.service.AiImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiImportController {

    private final AiImportService aiImportService;

    @PostMapping("/extract-house")
    public ResponseEntity<AiImportResultDto> extractHouse(
            @RequestParam("image") MultipartFile image) {
        try {
            AiImportResultDto result = aiImportService.extractFromImage(image);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("AI 解析圖片失敗", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
