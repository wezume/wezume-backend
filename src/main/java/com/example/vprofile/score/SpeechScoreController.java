package com.example.vprofile.score;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scores")
public class SpeechScoreController {

    private final SpeechScoreService service;

    public SpeechScoreController(SpeechScoreService service) {
        this.service = service;
    }

    @PostMapping
    public SpeechScore save(@RequestBody SpeechScore score) {
        return service.saveScore(score);
    }

    @GetMapping("/video/{videoId}")
    public SpeechScore getByVideoId(@PathVariable Long videoId) {
        return service.getScoreByVideoId(videoId);
    }
}

