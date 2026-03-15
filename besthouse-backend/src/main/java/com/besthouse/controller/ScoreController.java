package com.besthouse.controller;

import com.besthouse.dto.ScoreResultDto;
import com.besthouse.service.ScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    @GetMapping
    public ResponseEntity<List<ScoreResultDto>> getRanking() {
        return ResponseEntity.ok(scoreService.calculateRanking());
    }
}
