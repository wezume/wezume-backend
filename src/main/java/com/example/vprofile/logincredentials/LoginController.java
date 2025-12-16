package com.example.vprofile.logincredentials;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.vprofile.jwttoken.JwtUtil;
import com.example.vprofile.placementLogin.PlacementLogin;
import com.example.vprofile.placementLogin.PlacementRepository;
import com.example.vprofile.videofolder.Video;
import com.example.vprofile.videofolder.VideoRepository;

@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlacementRepository placementRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private VideoRepository videoRepository;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        // Try fetching user from the User table
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getPassword().equals(password)) {
                // Generate JWT token
                String token = jwtUtil.generateToken(user.getFirstName(), user.getEmail());
                Map<String, Object> response = new HashMap<>();
                response.put("token", token); 
                response.put("jobOption",user.getJobOption()); // Return only the token
                return ResponseEntity.ok(response);
            }
        }

        // Try fetching user from the PlacementLogin table
        Optional<PlacementLogin> placementLoginOptional = placementRepository.findByEmail(email);

        if (placementLoginOptional.isPresent()) {
            PlacementLogin placementLogin = placementLoginOptional.get();
            if (placementLogin.getPassword().equals(password)) {
                // Generate JWT token
                String token = jwtUtil.generateToken(placementLogin.getFirstname(), placementLogin.getEmail());
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("jobOption",placementLogin.getJobOption());  // Return only the token
                return ResponseEntity.ok(response);
            }
        }

        // If email and password don't match in either table
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", "Invalid email or password!");
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @GetMapping("/user-detail")
    public ResponseEntity<Map<String, Object>> getUserDetails(@RequestHeader("Authorization") String token) {
        // Extract email from the token
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));

        // Try fetching user from the User table
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        // Check if user exists in the User table
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // Fetch video details
            List<Map<String, Object>> videoDetails = getVideoDetailsByUserId(user.getId());
            // Prepare response with user data and video details
            return prepareResponse(user, videoDetails);
        }

        // If not found in User table, check PlacementLogin table
        Optional<PlacementLogin> placementLoginOptional = placementRepository.findByEmail(email);
        
        // Check if user exists in the PlacementLogin table
        if (placementLoginOptional.isPresent()) {
            PlacementLogin placementLogin = placementLoginOptional.get();
            // Fetch video details (if any) associated with the placement login
            List<Map<String, Object>> videoDetails = getVideoDetailsByUserId(placementLogin.getId());
            // Prepare response with placement login data and video details
            return prepareResponseFromPlacementLogin(placementLogin, videoDetails);
        }

        // If email and password don't match in either table
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", "User not found!");
        return ResponseEntity.status(404).body(errorResponse);
    }

    // Helper method to prepare the response for the User table
    private ResponseEntity<Map<String, Object>> prepareResponse(User user, List<Map<String, Object>> videoDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("firstName", user.getFirstName());
        response.put("jobOption", user.getJobOption());
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("industry", user.getIndustry());
        response.put("currentEmployer", user.getCurrentEmployer());
        response.put("establishedYear", user.getEstablishedYear());
        response.put("keySkills", user.getKeySkills());
        response.put("currentRole", user.getCurrentRole());
        response.put("roleCode", user.getJobId());
        response.put("college", user.getCollege());
        response.put("phoneNumber", user.getPhoneNumber()); // Added phoneNumber
        response.put("videos", videoDetails);
        response.put("profileUrl",user.getProfilepicurl());

        return ResponseEntity.ok(response);
    }

    // Helper method to prepare the response for the PlacementLogin table
    private ResponseEntity<Map<String, Object>> prepareResponseFromPlacementLogin(PlacementLogin placementLogin, List<Map<String, Object>> videoDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("firstName", placementLogin.getFirstname());
        response.put("jobOption", placementLogin.getJobOption());
        response.put("userId", placementLogin.getId());
        response.put("email", placementLogin.getEmail());
        response.put("profilePic", placementLogin.getEmail()); // You can set default if not available
        response.put("college", placementLogin.getCollege());
        response.put("jobid", placementLogin.getJobid());
        response.put("phoneNumber", placementLogin.getPhoneNumber()); // Added phoneNumber
        response.put("videos", videoDetails);

        return ResponseEntity.ok(response);
    }

    // Helper method to fetch video details by user ID
    private List<Map<String, Object>> getVideoDetailsByUserId(Long userId) {
        Optional<Video> userVideos = videoRepository.findByUserId(userId);
        return userVideos.stream().map(video -> {
            Map<String, Object> videoMap = new HashMap<>();
            videoMap.put("videoId", video.getId());
            videoMap.put("videoUrl", video.getUrl());
            videoMap.put("thumbnail", video.getThumbnailUrl());
            return videoMap;
        }).toList();
    }
}
