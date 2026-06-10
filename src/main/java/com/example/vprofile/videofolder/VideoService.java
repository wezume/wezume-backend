package com.example.vprofile.videofolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.assemblyai.api.AssemblyAI;
import com.assemblyai.api.resources.transcripts.types.Transcript;
import com.assemblyai.api.resources.transcripts.types.TranscriptOptionalParams;
import com.assemblyai.api.resources.transcripts.types.TranscriptStatus;
import com.example.vprofile.ffmpeg.FFmpegService;
import com.example.vprofile.likefolder.Like;
import com.example.vprofile.likefolder.LikeRepository;
import com.example.vprofile.logincredentials.User;
import com.example.vprofile.logincredentials.UserRepository;
import com.example.vprofile.placementLogin.PlacementRepository;

@Service
@Lazy
public class VideoService {

    private final String uploadDir = "uploads/videos/";

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlacementRepository placementRepository;
    @Autowired
    private FFmpegService ffmpegService;

    @Value("${assemblyai.api.key}")
    private String assemblyAiApiKey;

    @SuppressWarnings({"UseSpecificCatch", "CallToPrintStackTrace"})
    public Video saveVideo(MultipartFile file, Long userId, String jobId, String college, String roleCode) throws IOException {
        validateUser(userId);

        String userPrefix = "user" + userId + "_";
        Path rawFilePath = saveUploadedFile(file, userPrefix + file.getOriginalFilename());
        Path rawAbs = rawFilePath.toAbsolutePath();

        String compressedName = "compressed_" + userPrefix + file.getOriginalFilename();
        File compressedFile = rawAbs.resolveSibling(compressedName).toFile();
        String videoUrl = "https://wezume.in/uploads/videos/" + compressedName;

        // Remove any existing video for this user (1-video-per-user constraint)
        // Also delete the original raw file so we don't accumulate stale sources
        videoRepository.findAllByUserId(userId).forEach(existing -> {
            if (existing.getFilePath() != null) {
                try {
                    Path oldCompressed = Paths.get(existing.getFilePath());
                    Files.deleteIfExists(oldCompressed);
                    // delete the original (same path with "original_" prefix instead of "compressed_")
                    String oldOriginal = oldCompressed.toString().replace("/compressed_", "/original_");
                    Files.deleteIfExists(Paths.get(oldOriginal));
                } catch (Exception ignored) {}
            }
            videoRepository.delete(existing);
        });

        // Save immediately so the app can navigate to status screen without waiting for FFmpeg
        Video video = new Video();
        video.setFileName(compressedName);
        video.setUserId(userId);
        video.setFilePath(null);   // set after FFmpeg completes
        video.setUrl(null);        // set after FFmpeg completes
        video.setJobId(jobId);
        video.setCollege(college);
        video.setRoleCode(roleCode);
        video.setProcessingStatus("PROCESSING");
        Video saved = videoRepository.save(video);
        final Long videoId = saved.getId();

        // Compress async — scheduler waits for filePath != null before transcribing
        CompletableFuture.runAsync(() -> {
            try {
                // Keep the original upload as "original_" so we can always re-watermark cleanly in future
                Path originalFile = rawAbs.resolveSibling("original_" + userPrefix + file.getOriginalFilename());
                Files.move(rawAbs, originalFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                ffmpegService.compressVideo(originalFile.toFile(), compressedFile);
                Video v = videoRepository.findById(videoId).orElseThrow();
                v.setFilePath(compressedFile.getAbsolutePath());
                v.setUrl(videoUrl);
                videoRepository.save(v);
            } catch (Exception e) {
                System.err.println("Async FFmpeg failed for video " + videoId + ": " + e.getMessage());
                videoRepository.findById(videoId).ifPresent(v -> {
                    v.setProcessingStatus("ERROR");
                    videoRepository.save(v);
                });
            }
        });

        return saved;
    }
    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User with ID " + userId + " does not exist.");
        }
    }
    private Path saveUploadedFile(MultipartFile file, String fileName) throws IOException {
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        Path filePath = Paths.get(uploadDir).resolve(fileName);
        file.transferTo(filePath.toAbsolutePath().toFile());

        return filePath;
    }
    private String extractAudioWithFfmpeg(Path videoFilePath) throws IOException, InterruptedException {
        String originalFileName = videoFilePath.getFileName().toString();
        String audioFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.')) + ".mp3";
        Path audioDirectoryPath = Paths.get(uploadDir, "audio");
        if (!Files.exists(audioDirectoryPath)) {
            Files.createDirectories(audioDirectoryPath);
        }

        Path audioOutputPath = audioDirectoryPath.resolve(audioFileName);
        ProcessBuilder processBuilder = new ProcessBuilder(
                "/usr/bin/ffmpeg",
                "-i", videoFilePath.toAbsolutePath().toString(),
                "-vn",
                "-q:a", "0",
                "-map", "a",
                "-y",
                audioOutputPath.toAbsolutePath().toString()
        );

        processBuilder.redirectErrorStream(true); // Combine stdout and stderr for easier logging

        System.out.println("Executing FFmpeg command: " + String.join(" ", processBuilder.command()));
        Process process = processBuilder.start();

        // Read the output from the process for debugging
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("FFMPEG: " + line); // Use a logger in a real app
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("FFmpeg process failed with exit code: " + exitCode);
        }

        if (!Files.exists(audioOutputPath) || Files.size(audioOutputPath) == 0) {
            throw new IOException("Audio extraction failed: output file is missing or empty.");
        }

        return audioOutputPath.toString();
    }
    private String generateThumbnailWithFfmpeg(File videoFile) {
        try {
            String videoName = videoFile.getName();
            String baseName = videoName.substring(0, videoName.lastIndexOf('.'));
            String thumbnailName = "thumbnail_" + baseName + ".jpg";
            File thumbnailFile = new File(uploadDir, thumbnailName);

            ProcessBuilder pb = new ProcessBuilder(
                "/usr/bin/ffmpeg",
                "-i", videoFile.getAbsolutePath(),
                "-ss", "00:00:01",
                "-vframes", "1",
                "-y",
                thumbnailFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {}
            }
            int exitCode = process.waitFor();
            if (exitCode != 0 || !thumbnailFile.exists() || thumbnailFile.length() == 0) {
                System.err.println("Thumbnail generation failed for: " + videoName);
                return null;
            }
            return "https://wezume.in/uploads/videos/" + thumbnailName;
        } catch (Exception e) {
            System.err.println("Thumbnail generation exception: " + e.getMessage());
            return null;
        }
    }

    private String convertAudioToText(String audioFilePath) throws IOException {
        AssemblyAI client = AssemblyAI.builder()
                .apiKey(assemblyAiApiKey)
                .build();

        TranscriptOptionalParams params = TranscriptOptionalParams.builder()
                .speakerLabels(true)
                .build();

        Transcript transcript = client.transcripts().transcribe(new File(audioFilePath), params);

        if (transcript.getStatus().equals(TranscriptStatus.ERROR)) {
            throw new IOException("Transcription error: " + transcript.getError().orElse("Unknown error"));
        }
        return transcript.getText().orElse("No transcription text available");
    }

    public void transcribeVideo(Long videoId) throws IOException, InterruptedException {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + videoId));
        if (video.getTranscription() != null) return;

        // Extract audio from compressed video if not done yet
        if (video.getAudioFilePath() == null) {
            Path compressedPath = Paths.get(video.getFilePath());
            String extractedAudioPath = extractAudioWithFfmpeg(compressedPath);
            String audioFileName = Paths.get(extractedAudioPath).getFileName().toString();
            video.setAudioFilePath("https://wezume.in/uploads/videos/audio/" + audioFileName);
            videoRepository.save(video);
        }

        // Generate thumbnail from compressed video if not done yet
        if (video.getThumbnailUrl() == null) {
            String thumbUrl = generateThumbnailWithFfmpeg(new File(video.getFilePath()));
            if (thumbUrl != null) {
                video.setThumbnailUrl(thumbUrl);
                videoRepository.save(video);
            }
        }

        String audioFileName = video.getAudioFilePath().substring(video.getAudioFilePath().lastIndexOf('/') + 1);
        String localAudioPath = uploadDir + "audio/" + audioFileName;

        String transcription = convertAudioToText(localAudioPath);
        video.setTranscription(transcription);
        video.setProcessingStatus("SCORING");
        videoRepository.save(video);
    }

    public Optional<Video> getLatestVideoByUserId(Long userId) {
        return videoRepository.findTopByUserIdOrderByIdDesc(userId);
    }
    public String getTranscriptionByUserId(Long userId) {
        Optional<Video> videoOptional = videoRepository.findByUserId(userId);

        if (videoOptional.isEmpty()) {
            throw new IllegalArgumentException("Video not found for the user");
        }

        Video video = videoOptional.get();
        return video.getTranscription();
    }

    // Fetch transcription of a video by videoId
    public String getTranscriptionByVideoId(Long videoId) {
        // Fetch the video by its ID
        Optional<Video> videoOptional = videoRepository.findById(videoId);

        if (videoOptional.isEmpty()) {
            throw new IllegalArgumentException("Video not found for the given ID");
        }

        Video video = videoOptional.get();
        return video.getTranscription();
    }

    public Video updateTranscriptionByUserId(Long userId, String transcriptionContent) {
        // Fetch user by ID
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid user ID");
        }

        // Fetch video for the user
        Optional<Video> videoOptional = videoRepository.findByUserId(userId);
        if (videoOptional.isEmpty()) {
            throw new IllegalArgumentException("Video not found for the user");
        }

        Video video = videoOptional.get();
        video.setTranscription(transcriptionContent);

        // Save updated video
        return videoRepository.save(video);
    }

    public boolean deleteVideoByUserId(Long userId) {
        // Fetch the video associated with the userId
        Optional<Video> videoOptional = videoRepository.findByUserId(userId);

        // Check if the video exists
        if (videoOptional.isPresent()) {
            Video video = videoOptional.get();

            // Delete the video from the repository
            videoRepository.delete(video);
            return true; // Return true if deletion is successful
        }

        return false; // Return false if video is not found
    }

    public List<Video> getAllVideos() {
        return videoRepository.findAll();
    }

    String generateSRT(String transcription) {
        System.out.println("Starting SRT generation for transcription: " + transcription);

        // Split transcription into lines
        String[] lines = transcription.split("\\.\\s+");
        System.out.println("Split transcription into " + lines.length + " lines.");

        StringBuilder srtBuilder = new StringBuilder();
        int startTime = 0;
        double wordsPerSecond = 3; // Average speaking rate in words per second

        for (int i = 0; i < lines.length; i++) {
            int wordCount = lines[i].split("\\s+").length;
            int duration = (int) Math.ceil(wordCount / wordsPerSecond); // Calculate duration dynamically
            int endTime = startTime + duration;

            // Append SRT entry
            srtBuilder.append(i + 1).append("\n")
                    .append(formatTime(startTime)).append(" --> ").append(formatTime(endTime)).append("\n")
                    .append(lines[i]).append("\n\n");

            System.out.println("Added SRT entry " + (i + 1) + ":");
            System.out.println("Start time: " + formatTime(startTime));
            System.out.println("End time: " + formatTime(endTime));
            System.out.println("Text: " + lines[i]);

            startTime = endTime; // Move to the next start time
        }

        String srtContent = srtBuilder.toString();
        System.out.println("Generated SRT content:\n" + srtContent);

        return srtContent;
    }

    private String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        String formattedTime = String.format("%02d:%02d:%02d,000", hours, minutes, secs);
        System.out.println("Formatted time for " + seconds + " seconds: " + formattedTime);
        return formattedTime;
    }

    public Page<Video> filterVideos(
            String keySkills,
            String experience,
            String industry,
            String city,
            String jobId,
            String college,
            Pageable pageable
    ) {
        String keySkillsLower = (keySkills == null || keySkills.isBlank()) ? null : keySkills.toLowerCase();

        List<String> experienceList = nullIfEmpty(toList(experience));
        List<String> industryList = nullIfEmpty(toList(industry));
        List<String> cityList = nullIfEmpty(toList(city));

        List<Long> userIds = videoRepository.findUserIdsByFilters(
                keySkillsLower, experienceList, industryList, cityList, jobId, college
        );

        Specification<Video> spec = VideoSpecification.filterByUserIds(userIds);

        if (jobId != null && !jobId.isBlank()) {
            spec = spec.and(VideoSpecification.hasJobId(jobId));
        }

        if (college != null && !college.isBlank()) {
            spec = spec.and(VideoSpecification.hasCollege(college));
        }

        return videoRepository.findAll(spec, pageable);
    }

    private List<String> toList(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> nullIfEmpty(List<String> list) {
        return (list == null || list.isEmpty()) ? null : list;
    }

    public void addLike(Long userId, Long videoId) {
        if (likeRepository.existsByUserIdAndVideoId(userId, videoId)) {
            throw new IllegalArgumentException("User has already liked this video");
        }

        // Add the like to the likes table
        Like like = new Like();
        like.setUserId(userId);
        like.setVideoId(videoId);
        likeRepository.save(like);
    }

    public void addDislike(Long userId, Long videoId) {
        Optional<Like> existingLike = likeRepository.findByUserIdAndVideoId(userId, videoId);

        if (existingLike.isPresent()) {
            Like like = existingLike.get();
            if (like.getIsLike()) {
                likeRepository.delete(like);
                Like dislike = new Like(userId, videoId, videoId, null, false);
                likeRepository.save(dislike);
            } else {
                likeRepository.delete(like);
            }
        } else {
            Like dislike = new Like(userId, videoId, videoId, null, false);
            likeRepository.save(dislike);
        }
    }

    public Long getLikeCount(Long videoId) {
        return likeRepository.countByVideoIdAndIsLikeTrue(videoId);
    }

    public Video getVideoById(Long videoId) {
        return videoRepository.findById(videoId).orElse(null);
    }

    public List<Long> getVideoIdsByUserId(Long userId) {
        return videoRepository.findVideoIdsByUserId(userId);
    }

    public List<Video> getLikedVideosByUserId(Long userId) {
        return likeRepository.findLikedVideosByUserId(userId);
    }

    public List<Video> getTrendingVideos() {
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);
        return videoRepository.findTrendingVideos(startOfWeek);
    }

    public String getVideoPathByUserId(Long userId) {
        Optional<Video> videoOptional = videoRepository.findByUserId(userId);
        return videoOptional.map(Video::getFilePath).orElse(null);
    }

    public String getVideoPathById(Long videoId) {
        return videoRepository.findById(videoId)
                .map(Video::getFilePath)
                .orElse(null);
    }

    public List<Video> getVideosByJobId(String jobid) {
        // Fetch videos based on roleCode from Video repository
        return videoRepository.findByJobId(jobid);  // This should be in the Video repository
    }


    public Page<Video> getVideosByJobId(String jobId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return videoRepository.findByJobId(jobId, pageable);
    }

    public Page<Video> getAllVideos(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return videoRepository.findAll(pageable);
    }

     public Map<String, Long> getCountsByJobId(String jobid) {
        long totalUsers = userRepository.countByJobid(jobid);
        long totalVideos = videoRepository.countByJobId(jobid);

        Map<String, Long> result = new HashMap<>();
        result.put("totalUsers", totalUsers);
        result.put("totalVideos", totalVideos);

        return result;
    }

}
