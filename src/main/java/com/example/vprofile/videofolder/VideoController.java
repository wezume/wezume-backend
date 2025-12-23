package com.example.vprofile.videofolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.vprofile.VideoEmbedding.EmbeddingService;
import com.example.vprofile.ffmpeg.FrameExtractor;
import com.example.vprofile.ffmpeg.VideoProcessingService;
import com.example.vprofile.likefolder.Like;
import com.example.vprofile.likefolder.LikeRepository;
import com.example.vprofile.logincredentials.User;
import com.example.vprofile.logincredentials.UserRepository;
import com.example.vprofile.logincredentials.UserService;
import com.example.vprofile.notification.NotificationService;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoProcessingService videoProcessingService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmbeddingService embeddingService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "jobId", required = false) String jobId,
            @RequestParam(value = "roleCode", required = false) String roleCode,
            @RequestParam(value = "college", required = false) String college) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID.");
        }

        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No file uploaded.");
        }

        try {
            // Save the video and process the file
            Video video = videoService.saveVideo(file, userId, jobId, college, roleCode); // Ensure this method handles
                                                                                          // the file and user
            return ResponseEntity.status(HttpStatus.CREATED).body(video);
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File upload failed: " + e.getMessage());
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserVideoUrl(@PathVariable Long userId) {
        Optional<Video> videoOptional = videoService.getLatestVideoByUserId(userId);

        if (videoOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No video found for this user"));
        }

        Video video = videoOptional.get();
        Map<String, Object> response = new HashMap<>();
        response.put("id", video.getId());
        response.put("videoUrl", video.getUrl());
        response.put("userId", video.getUserId());
        response.put("jobid", video.getJobId());
        response.put("tumbnail", video.getThumbnailUrl());
        response.put("audiourl", video.getAudioFilePath());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/video/{videoId}")
    public ResponseEntity<Map<String, Object>> getVideoDetailsById(@PathVariable Long videoId) {

        Video video = videoService.getVideoById(videoId);

        if (video == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Video not found"));
        }

        // 🔹 Fetch the user associated with this video
        User user = userService.getUserById(video.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("id", video.getId());
        response.put("videoUrl", video.getUrl());
        response.put("userId", video.getUserId());
        response.put("jobid", video.getJobId());
        response.put("thumbnail", video.getThumbnailUrl());

        // 🔹 Add firstName (if user exists)
        response.put("firstName", user != null ? user.getFirstName() : null);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<Map<String, Object>> getVideosByJobId(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "0") int page, // Page number (default is 0)
            @RequestParam(defaultValue = "20") int size // Page size (default is 20)
    ) {
        // Fetch the videos with pagination
        Page<Video> videoPage = videoService.getVideosByJobId(jobId, page, size);

        if (videoPage.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap(
                    "message", "No videos found for this job ID"));
        }

        List<Long> userIds = videoPage.stream()
                .map(Video::getUserId)
                .distinct()
                .collect(Collectors.toList());

        List<User> users = userRepository.findAllByIdIn(userIds);

        // Create a map of user IDs to user data for quick lookup
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<Map<String, Object>> videoResponses = new ArrayList<>();
        for (Video video : videoPage) {
            Map<String, Object> videoData = new HashMap<>();
            videoData.put("id", video.getId());
            videoData.put("videoUrl", video.getUrl());
            videoData.put("userId", video.getUserId());
            videoData.put("jobId", video.getJobId());
            videoData.put("thumbnail", video.getThumbnailUrl());
            videoData.put("audioUrl", video.getAudioFilePath());

            // Lookup the user information from the map
            User user = userMap.get(video.getUserId());
            if (user != null) {
                videoData.put("firstName", user.getFirstName());
                videoData.put("phoneNumber", user.getPhoneNumber());
                videoData.put("email", user.getEmail());
                videoData.put("profilePic", user.getProfilepicurl());
            } else {
                videoData.put("firstName", "Unknown");
                videoData.put("phoneNumber", "N/A");
                videoData.put("email", "N/A");
                videoData.put("profilePic", "https://wezume.in/uploads/videos/defaultpic.png");
            }

            videoResponses.add(videoData);
        }

        // Return paginated response with total pages and current page number
        Map<String, Object> response = new HashMap<>();
        response.put("videos", videoResponses);
        response.put("currentPage", videoPage.getNumber());
        response.put("totalPages", videoPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/transcription")
    public ResponseEntity<Map<String, String>> getTranscriptionByUserId(@PathVariable Long userId) {
        try {
            // Fetch transcription for the user
            String transcription = videoService.getTranscriptionByUserId(userId);

            if (transcription != null && !transcription.isEmpty()) {
                return ResponseEntity.ok(Map.of("transcription", transcription));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Transcription not found for the user"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error fetching transcription", "error", e.getMessage()));
        }
    }

    @PutMapping("/{userId}/transcription")
    public ResponseEntity<Map<String, String>> updateTranscriptionByUserId(
            @PathVariable Long userId, @RequestBody Map<String, String> requestBody) {
        String transcriptionContent = requestBody.get("transcription");

        if (transcriptionContent == null || transcriptionContent.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Transcription content is required"));
        }

        try {
            // Update transcription for the user
            Video updatedVideo = videoService.updateTranscriptionByUserId(userId, transcriptionContent);

            return ResponseEntity.ok(Map.of(
                    "message", "Transcription updated successfully",
                    "video", updatedVideo.toString()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error updating transcription", "error", e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<String> deleteVideo(@PathVariable Long userId) {
        Optional<Video> videoOptional = videoRepository.findByUserId(userId);

        if (!videoOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Video not found for userId: " + userId);
        }
        Video video = videoOptional.get();
        videoRepository.delete(video);
        return ResponseEntity.ok("Video deleted successfully for userId: " + userId);
    }

    @GetMapping("/videos")
    public ResponseEntity<Map<String, Object>> getAllVideos(
            @RequestParam(defaultValue = "0") int page, // Page number, default is 0
            @RequestParam(defaultValue = "20") int size // Size of the page, default is 20
    ) {
        // Fetch paginated videos from the service
        Page<Video> videoPage = videoService.getAllVideos(page, size);

        if (videoPage.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        List<Map<String, Object>> videoResponses = new ArrayList<>();
        String defaultProfilePic = "https://wezume.in/uploads/videos/defaultpic.png";

        for (Video videoEntity : videoPage) {
            Map<String, Object> videoData = new HashMap<>();
            videoData.put("id", videoEntity.getId());
            videoData.put("videoUrl", videoEntity.getUrl());
            videoData.put("userId", videoEntity.getUserId());
            videoData.put("thumbnail", videoEntity.getThumbnailUrl());

            // Retrieve user data
            User user = userRepository.findById(videoEntity.getUserId()).orElse(null);
            if (user != null) {
                videoData.put("firstname", user.getFirstName());
                videoData.put("email", user.getEmail());
                videoData.put("phonenumber", user.getPhoneNumber());
                videoData.put("profilepic",
                        user.getProfilepicurl() != null ? user.getProfilepicurl() : defaultProfilePic);
            } else {
                videoData.put("firstname", "User");
                videoData.put("profilepic", defaultProfilePic);
            }

            videoResponses.add(videoData);
        }

        // Return paginated data along with total pages info
        Map<String, Object> response = new HashMap<>();
        response.put("videos", videoResponses);
        response.put("currentPage", videoPage.getNumber());
        response.put("totalPages", videoPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/subtitles.srt")
    @SuppressWarnings("CallToPrintStackTrace")
    public ResponseEntity<Resource> generateSRTForUser(@PathVariable Long userId) {
        try {
            String transcription = videoService.getTranscriptionByUserId(userId);

            if (transcription == null || transcription.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }
            String srtContent = videoService.generateSRT(transcription);
            ByteArrayResource resource = new ByteArrayResource(srtContent.getBytes());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=subtitles.srt")
                    .contentType(MediaType.parseMediaType("application/x-subrip"))
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("user/{videoId}/subtitles.srt")
    @SuppressWarnings("CallToPrintStackTrace")
    public ResponseEntity<Resource> generateSRTForVideo(@PathVariable Long videoId) {
        if (videoId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        try {
            String transcription = videoService.getTranscriptionByVideoId(videoId);

            if (transcription == null || transcription.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }
            String srtContent = videoService.generateSRT(transcription);
            ByteArrayResource resource = new ByteArrayResource(srtContent.getBytes());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=subtitles.srt")
                    .contentType(MediaType.parseMediaType("application/x-subrip"))
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/filter")
    public ResponseEntity<Map<String, Object>> filterVideos(@RequestBody Map<String, Object> request) {

        // ✅ FIX: Read page and size from the request BODY, not URL parameters.
        // Provide default values if they are not included in the request.
        int page = request.get("page") != null ? Integer.parseInt(request.get("page").toString()) : 0;
        int size = request.get("size") != null ? Integer.parseInt(request.get("size").toString()) : 20;

        // Extract user filter fields
        String keySkills = (String) request.get("keySkills");
        String experience = (String) request.get("experience");
        String industry = (String) request.get("industry");
        String city = (String) request.get("city");
        String jobId = (String) request.get("jobId");
        String college = (String) request.get("college");
        String transcriptionKeywords = (String) request.get("transcriptionKeywords");

        // Run filtering logic
        List<Video> videos = videoService.filterVideos(keySkills, experience, industry, city, jobId, college);

        // Transcription keyword filtering (if provided)
        if (transcriptionKeywords != null && !transcriptionKeywords.isBlank()) {
            List<String> keywordList = Arrays.stream(transcriptionKeywords.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            videos = videos.stream()
                    .filter(video -> {
                        String transcription = video.getTranscription() == null ? ""
                                : video.getTranscription().toLowerCase();
                        return keywordList.stream().anyMatch(transcription::contains);
                    })
                    .collect(Collectors.toList());
        }

        // Get total count before pagination
        int totalVideos = videos.size();

        // ✅ FIX: Calculate total pages
        int totalPages = (int) Math.ceil((double) totalVideos / size);

        // Paginate the results
        int fromIndex = Math.min(page * size, totalVideos);
        int toIndex = Math.min((page + 1) * size, totalVideos);
        List<Video> paginatedVideos = videos.subList(fromIndex, toIndex);

        // Build the list of video responses
        List<Map<String, Object>> videoResponses = new ArrayList<>();
        for (Video video : paginatedVideos) {
            Map<String, Object> videoData = new HashMap<>();
            videoData.put("id", video.getId());
            videoData.put("userId", video.getUserId());
            videoData.put("videoUrl", video.getUrl());
            videoData.put("thumbnail", video.getThumbnailUrl());
            videoData.put("jobId", video.getJobId());

            User videoUser = userRepository.findById(video.getUserId()).orElse(null);

            if (videoUser != null) {
                videoData.put("firstName", videoUser.getFirstName());
                videoData.put("email", videoUser.getEmail());
                videoData.put("phoneNumber", videoUser.getPhoneNumber());
                videoData.put("profilePic", videoUser.getProfilepicurl());
                videoData.put("college", videoUser.getCollege());
            } else {
                videoData.put("firstName", "User");
                videoData.put("email", "");
                videoData.put("phoneNumber", "");
                videoData.put("profilePic", "https://wezume.in/uploads/videos/defaultpic.png");
                videoData.put("college", "Unknown");
            }

            videoResponses.add(videoData);
        }

        // ✅ FIX: Create the final response object with pagination info
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("videos", videoResponses);
        responseBody.put("currentPage", page);
        responseBody.put("totalPages", totalPages);

        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/user/{videoId}/details")
    public ResponseEntity<Map<String, String>> getUserDetailsByVideoId(@PathVariable Long videoId) {
        if (videoId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        Optional<Video> video = videoRepository.findById(videoId);
        if (video.isPresent()) {
            Long userId = video.get().getUserId();
            Optional<User> user = userRepository.findById(userId);
            if (user.isPresent()) {
                Map<String, String> userDetails = new HashMap<>();
                User userEntity = user.get();
                userDetails.put("firstName", Optional.ofNullable(userEntity.getFirstName()).orElse(""));
                userDetails.put("userId", Optional.ofNullable(userEntity.getId()).map(String::valueOf).orElse(""));
                byte[] profilePic = userEntity.getProfilePic();
                String profileImageBase64 = (profilePic != null)
                        ? Base64.getEncoder().encodeToString(profilePic)
                        : "";
                userDetails.put("profileImage", profileImageBase64);

                return ResponseEntity.ok(userDetails);
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PostMapping("/{videoId}/like")
    public ResponseEntity<String> likeVideo(
            @PathVariable Long videoId,
            @RequestParam Long userId,
            @RequestParam String firstName) {
        if (videoId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid video ID.");
        }
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Optional<Like> existingLike = likeRepository.findByUserIdAndVideoId(userId, videoId);

        if (existingLike.isPresent()) {
            Like like = existingLike.get();
            like.setIsLike(!like.getIsLike());
            like.setCreatedAt(LocalDateTime.now());
            likeRepository.save(like);
        } else {
            Like newLike = new Like();
            newLike.setUserId(userId);
            newLike.setVideoId(videoId);
            newLike.setIsLike(true);
            newLike.setCreatedAt(LocalDateTime.now());
            likeRepository.save(newLike);
        }
        notificationService.saveNotification(video, firstName);

        return ResponseEntity.ok("Video liked and notification sent.");
    }

    @PostMapping("/{videoId}/dislike")
    public ResponseEntity<String> dislikeVideo(
            @PathVariable Long videoId,
            @RequestParam Long userId,
            @RequestParam String firstName) {
        if (videoId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid video ID.");
        }
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Optional<Like> existingLike = likeRepository.findByUserIdAndVideoId(userId, videoId);

        if (existingLike.isPresent()) {
            Like like = existingLike.get();
            if (!like.getIsLike()) {
                likeRepository.delete(like); // Remove the dislike entry
            } else {
                likeRepository.delete(like); // Remove the like entry when disliked
            }
        } else {
            Like newDislike = new Like();
            newDislike.setUserId(userId);
            newDislike.setVideoId(videoId);
            newDislike.setIsLike(false);
            newDislike.setCreatedAt(LocalDateTime.now());
            likeRepository.save(newDislike);
        }
        return ResponseEntity.ok("Video disliked successfully.");
    }

    @GetMapping("/{videoId}/like-count")
    public ResponseEntity<Long> getLikeCount(@PathVariable Long videoId) {
        if (videoId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        Long likeCount = videoService.getLikeCount(videoId);
        return ResponseEntity.ok(likeCount);
    }

    @GetMapping("/likes/status")
    public Map<Long, Boolean> getLikeStatus(@RequestParam Long userId) {
        List<Video> allVideos = videoService.getAllVideos();
        Map<Long, Boolean> likeStatus = new HashMap<>();

        for (Video video : allVideos) {
            boolean isLiked = likeRepository.existsByUserIdAndVideoId(userId, video.getId());
            likeStatus.put(video.getId(), isLiked);
        }
        return likeStatus;
    }

    @GetMapping("/getOwnerByVideoId/{videoId}")
    public ResponseEntity<User> getOwnerByVideoId(@PathVariable("videoId") Long videoId) {
        if (videoId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        try {
            Video video = videoService.getVideoById(videoId);
            if (video == null) {
                return ResponseEntity.notFound().build();
            }
            Long userId = video.getUserId();
            User user = userService.getUserById(userId);

            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/getOwnerByUserId/{userId}")
    public ResponseEntity<User> getOwnerByUserId(@PathVariable("userId") Long userId) {
        try {
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/getVideoIdsByUserId/{userId}")
    public ResponseEntity<?> getVideoIdsByUserId(@PathVariable("userId") Long userId) {
        try {
            List<Long> videoIds = videoService.getVideoIdsByUserId(userId);
            if (videoIds.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(videoIds);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching video IDs: " + e.getMessage());
        }
    }

    @GetMapping("/liked")
    public ResponseEntity<List<Map<String, Object>>> getLikedVideosByUserId(
            @RequestParam("userId") Long userId,
            @RequestParam(defaultValue = "0") int page, // Page number, default to 0
            @RequestParam(defaultValue = "20") int size // Number of results per page, default to 20
    ) {
        List<Video> likedVideos = videoService.getLikedVideosByUserId(userId);

        if (likedVideos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        // Paginate the results
        int totalVideos = likedVideos.size();
        int fromIndex = Math.min(page * size, totalVideos);
        int toIndex = Math.min((page + 1) * size, totalVideos);
        List<Video> paginatedVideos = likedVideos.subList(fromIndex, toIndex);

        // Build the response
        List<Map<String, Object>> videoResponses = new ArrayList<>();
        for (Video video : paginatedVideos) {
            Map<String, Object> videoDataMap = new HashMap<>();
            videoDataMap.put("id", video.getId());
            videoDataMap.put("userId", video.getUserId());
            videoDataMap.put("videoUrl", video.getUrl());
            videoDataMap.put("thumbnail", video.getThumbnailUrl());

            // Add user details
            User user = userRepository.findById(video.getUserId()).orElse(null);
            if (user != null) {
                videoDataMap.put("firstName", user.getFirstName());
                videoDataMap.put("email", user.getEmail());
                videoDataMap.put("phoneNumber", user.getPhoneNumber());
                videoDataMap.put("profilePic", user.getProfilepicurl() != null ? user.getProfilepicurl()
                        : "https://wezume.in/uploads/videos/defaultpic.png");
            } else {
                videoDataMap.put("firstName", "User");
                videoDataMap.put("email", "");
                videoDataMap.put("phoneNumber", "");
                videoDataMap.put("profilePic", "https://wezume.in/uploads/videos/defaultpic.png");
            }

            videoResponses.add(videoDataMap);
        }

        return ResponseEntity.ok(videoResponses);
    }

    @GetMapping("/trending")
    public ResponseEntity<List<Map<String, Object>>> getTrendingVideos(
            @RequestParam(defaultValue = "0") int page, // Page number, default to 0
            @RequestParam(defaultValue = "20") int size // Number of results per page, default to 20
    ) {
        List<Video> trendingVideos = videoService.getTrendingVideos();

        if (trendingVideos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        // Paginate the results
        int totalVideos = trendingVideos.size();
        int fromIndex = Math.min(page * size, totalVideos);
        int toIndex = Math.min((page + 1) * size, totalVideos);
        List<Video> paginatedVideos = trendingVideos.subList(fromIndex, toIndex);

        List<Map<String, Object>> videoResponses = new ArrayList<>();

        for (Video video : paginatedVideos) {
            Map<String, Object> videoDataMap = new HashMap<>();
            videoDataMap.put("id", video.getId());
            videoDataMap.put("userId", video.getUserId());
            videoDataMap.put("videoUrl", video.getUrl());
            videoDataMap.put("thumbnail", video.getThumbnailUrl());

            // Fetch user details using userId
            Optional<User> userOptional = userRepository.findById(video.getUserId());

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                videoDataMap.put("firstName", user.getFirstName());
                videoDataMap.put("email", user.getEmail());
                videoDataMap.put("phoneNumber", user.getPhoneNumber());
                videoDataMap.put("profilePic",
                        user.getProfilepicurl() != null ? user.getProfilepicurl()
                                : "https://wezume.in/uploads/videos/defaultpic.png");
            } else {
                videoDataMap.put("firstName", "User");
                videoDataMap.put("email", "");
                videoDataMap.put("phoneNumber", "");
                videoDataMap.put("profilePic", "https://wezume.in/uploads/videos/defaultpic.png");
            }

            videoResponses.add(videoDataMap);
        }

        return ResponseEntity.ok(videoResponses);
    }

    @PostMapping("/check-profane")
    public ResponseEntity<?> checkForProfanity(@RequestBody Map<String, String> request) {
        String videoUri = request.get("file");
        if (videoUri == null || videoUri.isEmpty()) {
            return ResponseEntity.badRequest().body("Video URI is empty");
        }

        try {
            // 🔹 Step 1: Extract video file name from URI
            String videoFileName = new File(videoUri).getName();

            // 🔹 Step 2: Check if the video exists in the database
            Optional<Video> videoOptional = videoRepository.findByFileName(videoFileName);
            if (videoOptional.isPresent()) {
                Video video = videoOptional.get();

                // 🔹 Step 3: Skip processing if thumbnail already exists
                if (video.getThumbnailUrl() != null && !video.getThumbnailUrl().isEmpty()) {
                    return ResponseEntity.ok(Map.of(
                            "message", "Thumbnail already exists. Skipping profanity check.",
                            "thumbnailUrl", video.getThumbnailUrl()));
                }

                // 🔹 Step 4: Download video to a temp file
                File tempVideo = downloadVideo(videoUri);
                if (tempVideo == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to download video.");
                }

                // 🔹 Step 5: Check for profanity
                boolean hasProfanity = videoProcessingService.checkForVisualProfanity(tempVideo.getAbsolutePath());
                if (hasProfanity) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Profanity detected in the video.");
                }

                // 🔹 Step 6: Extract frames & save thumbnail
                String thumbnailUrl = extractAndSaveThumbnail(tempVideo, videoFileName);
                if (thumbnailUrl == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to generate thumbnail.");
                }

                // 🔹 Step 7: Update thumbnail URL in database
                video.setThumbnailUrl(thumbnailUrl);
                videoRepository.save(video);
                return ResponseEntity.ok(Map.of(
                        "message", "No profanity found.",
                        "thumbnailUrl", thumbnailUrl));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Video not found in the database.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing video: " + e.getMessage());
        }
    }

    private File downloadVideo(String videoUri) {
        try {
            URL url = new URL(videoUri);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() != 200) {
                return null;
            }

            File tempVideo = File.createTempFile("downloaded-video", ".mp4");
            try (InputStream inputStream = connection.getInputStream();
                    FileOutputStream fileOutputStream = new FileOutputStream(tempVideo)) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
            }
            return tempVideo;
        } catch (IOException e) {
            return null;
        }
    }

    private String extractAndSaveThumbnail(File tempVideo, String videoFileName) {
        try {
            File thumbnailDir = new File("uploads/videos/thumbnails/");
            if (!thumbnailDir.exists()) {
                thumbnailDir.mkdirs();
            }

            // Extract frames
            FrameExtractor frameExtractor = new FrameExtractor();
            String tempDirPath;
            try {
                tempDirPath = frameExtractor.extractFrames(tempVideo.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                return null; // Handle the exception and return null if extraction fails
            }
            File tempDir = new File(tempDirPath);

            File[] frames = tempDir.listFiles((dir, name) -> name.matches("frame_0001\\.jpg"));
            if (frames == null || frames.length == 0) {
                return null;
            }

            // Save the extracted frame with the desired name and ensure it's saved as an
            // image
            File firstFrame = frames[0];
            String thumbnailFileName = "thumbnail_" + videoFileName.replace(".mp4", ".jpg");
            File thumbnailFile = new File(thumbnailDir, thumbnailFileName);
            Files.copy(firstFrame.toPath(), thumbnailFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return "https://wezume.in/uploads/videos/thumbnails/" + thumbnailFileName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping("/video-count")
    public long getTotalUpload() {
        return videoRepository.countAllUpload();
    }

    @GetMapping("/counts/{jobid}")
    public ResponseEntity<Map<String, Long>> getCountsByJobId(@PathVariable String jobid) {
        Map<String, Long> counts = videoService.getCountsByJobId(jobid);
        return ResponseEntity.ok(counts);
    }

};
