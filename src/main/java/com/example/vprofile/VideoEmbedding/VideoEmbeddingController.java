package com.example.vprofile.VideoEmbedding;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.vprofile.videofolder.Video;
import com.example.vprofile.videofolder.VideoRepository;

@RestController
@RequestMapping("/api/video")
public class VideoEmbeddingController {

    private final VideoRepository videoRepo;
    private final EmbeddingService embeddingService;

    public VideoEmbeddingController(VideoRepository videoRepo, EmbeddingService embeddingService) {
        this.videoRepo = videoRepo;
        this.embeddingService = embeddingService;
    }

   @Scheduled(cron = "0 * * * * *")
    public void autoGenerateEmbeddings() throws Exception {
        System.out.println("Scheduler triggered...");

        Video video = videoRepo.findFirstByEmbeddingVectorIsNullAndTranscriptionIsNotNull();

        if (video != null) {
            System.out.println("Embedding pending for video ID: " + video.getId());
            embeddingService.generateEmbeddingFor(video);
        } else {
            System.out.println("No videos pending embedding");
        }
    }
}
