package com.example.vprofile.placementLogin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
public class PlacementController {

    @Autowired
    private PlacementService placementService;


    @PostMapping("/signup/placement")
    public ResponseEntity<String> signupPlacement(@RequestBody PlacementLogin placementLogin) {
        try {
            // 1. Check if the email already exists
            if (placementService.existsByEmail(placementLogin.getEmail())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Error: This email address is already registered.");
            }

            // 2. This single call handles the entire process correctly.
            placementService.registerNewPlacementAccount(placementLogin);

            // THE REDUNDANT LINE IS NOW REMOVED.
            // 3. Return a secure, generic success message.
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Registration successful. Please check your email to verify your account.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during registration.");
        }
    }

    // Login endpoint
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody PlacementLogin loginRequest) {
        // Call the service to validate login and get user details
        PlacementLogin user = placementService.login(loginRequest);

        Map<String, Object> response = new HashMap<>();

        if (user != null) {
            response.put("firstname", user.getFirstname());
            response.put("lastname", user.getLastname());
            response.put("jobOption", user.getJobOption());
            response.put("email", user.getEmail());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("jobid", user.getJobid());
            response.put("college", user.getCollege());

            return ResponseEntity.ok(response);
        } else {
            // If login fails, set error message
            response.put("status", "error");
            response.put("message", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @GetMapping("/details/{jobRole}")
    public ResponseEntity<Map<String, Object>> getDetailsByJobRole(@PathVariable String jobRole) {
        // Create a response map to hold the results
        Map<String, Object> response = new HashMap<>();

        // If the jobRole is "academy", fetch college details
        if ("academy".equalsIgnoreCase(jobRole)) {
            // Fetch all placements for the given jobRole as academy
            List<PlacementLogin> placements = placementService.getCollegeByJobRoleAcademy(jobRole);

            if (placements != null && !placements.isEmpty()) {
                // Create a list of colleges with their role codes
                List<Map<String, String>> collegeDetails = placements.stream()
                        .map(placement -> {
                            Map<String, String> collegeMap = new HashMap<>();
                            collegeMap.put("college", placement.getCollege()); // College name
                            collegeMap.put("jobid", placement.getJobid()); // Role code
                            return collegeMap;
                        })
                        .distinct() // Ensure no duplicates
                        .collect(Collectors.toList());

                // Return college details in the response
                response.put("status", "success");
                response.put("colleges", collegeDetails);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "No colleges found for the given job role 'academy'");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } // Otherwise, fetch placement drive details
        else {
            // Fetch all placements for the given jobRole as placement drive
            List<PlacementLogin> placements = placementService.getPlacementDriveDetailsByJobRole(jobRole);

            if (placements != null && !placements.isEmpty()) {
                // Create a list of colleges with their role codes for placement drive details
                List<Map<String, String>> collegeDetails = placements.stream()
                        .map(placement -> {
                            Map<String, String> collegeMap = new HashMap<>();
                            collegeMap.put("college", placement.getCollege()); // College name
                            collegeMap.put("jobid", placement.getJobid()); // Role code
                            return collegeMap;
                        })
                        .distinct() // Ensure no duplicates
                        .collect(Collectors.toList());

                // Return placement drive details in the response
                response.put("status", "success");
                response.put("colleges", collegeDetails);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "No placement drive details found for the given job role 'placementDrive'");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        }
    }

}
