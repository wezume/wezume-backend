package com.example.vprofile.score;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

