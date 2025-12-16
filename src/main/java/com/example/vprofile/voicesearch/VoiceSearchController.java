package com.example.vprofile.voicesearch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.vprofile.logincredentials.User;
import com.example.vprofile.logincredentials.UserService;
import com.example.vprofile.videofolder.Video;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/search")
public class VoiceSearchController {

    private final VoiceSearchService searchService;

    @Autowired
    private UserService userService;

    @Autowired
    private JDExtractionService jdExtractionService;

    public VoiceSearchController(VoiceSearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/voice")
    public ResponseEntity<?> voiceSearch(@RequestParam Long userId,
            @RequestParam String transcription) {
        try {
            List<Video> results = searchService.search(userId, transcription);

            List<Map<String, Object>> responseList = results.stream().map(video -> {

                User user = userService.getUserById(video.getUserId());

                Map<String, Object> videoMap = new HashMap<>();
                videoMap.put("id", video.getId());
                videoMap.put("videoUrl", video.getUrl());
                videoMap.put("userId", video.getUserId());
                videoMap.put("jobid", video.getJobId());
                videoMap.put("thumbnail", video.getThumbnailUrl());
                videoMap.put("firstName", user != null ? user.getFirstName() : null);

                // ⭐ ADD CONFIDENCE SCORE IN RESPONSE
                videoMap.put("confidence", video.getConfidence());

                return videoMap;

            }).toList();

            return ResponseEntity.ok(responseList);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Voice Search Failed"));
        }
    }

    @PostMapping("/jd")
    public ResponseEntity<?> extractJD(@RequestParam("file") MultipartFile jdFile) {

        try {
            // 1. Convert file to raw text
            String jdText = jdExtractionService.extractText(jdFile);

            // 2. Extract structured data: skills, description, combined query
            Map<String, String> structuredData = jdExtractionService.extractStructuredData(jdText);

            // 3. Return JSON object { skills, description, query }
            return ResponseEntity.ok(structuredData);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to read JD: " + e.getMessage());
        }
    }

    @PostMapping("/upload-voice-search")
    @SuppressWarnings("CallToPrintStackTrace")
    public ResponseEntity<?> uploadVoiceSearch(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {

        try {
            Path temp = Files.createTempFile("voice", ".wav");
            Files.write(temp, file.getBytes());

            WebClient webClient = WebClient.builder().build();
            JsonNode response = webClient.post()
                    .uri("http://app.wezume.in:8001/transcribe")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData("audio", new FileSystemResource(temp)))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || response.get("transcription") == null) {
                return ResponseEntity.internalServerError().body("Failed to transcribe audio");
            }

            String transcription = response.get("transcription").asText();

            // delete temp file
            Files.deleteIfExists(temp);

            return ResponseEntity.ok(transcription);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Voice Search Failed");
        }
    }

}
