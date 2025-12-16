package com.example.vprofile.logincredentials;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity; // Import MediaType here
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users") // Base URL for this controller
public class UserController {

    private final UserRepository userRepository;

    @Autowired
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Autowired
    private UserService userService;

    @PostMapping("/signup/user")
    public ResponseEntity<String> signupUser(@RequestParam("firstName") String firstName,
            @RequestParam("email") String email,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("password") String password,
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestParam(value = "jobOption", required = false) String jobOption,
            @RequestParam(value = "currentRole", required = false) String currentRole,
            @RequestParam(value = "experience", required = false) String experience,
            @RequestParam(value = "industry", required = false) String industry,
            @RequestParam(value = "currentEmployer", required = false) String currentEmployer,
            @RequestParam(value = "keySkills", required = false) String keySkills,
            @RequestParam(value = "college", required = false) String college,
            @RequestParam(value = "jobId", required = false) String jobId,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "establishedYear", required = false) Integer establishedYear,
            @RequestParam(value = "enabled", defaultValue = "true") boolean enabled,
            @RequestParam(value = "profilePic", required = false) MultipartFile profilePic) {
        try {
            // 1. Check if the email already exists
            if (userService.existsByEmail(email)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Error: This email address is already registered.");
            }

            // 2. Check if the phone number already exists
            if (userService.existsByPhoneNumber(phoneNumber)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Error: This phone number is already registered.");
            }

            // 3. Save the profile picture and get the file URL if present
            String profilePicUrl = null;
            if (profilePic != null && !profilePic.isEmpty()) {
                // Create a new User object to pass to the saveProfilePic method
                User user = new User();
                user.setFirstName(firstName);
                user.setEmail(email);
                user.setPhoneNumber(phoneNumber);
                user.setPassword(password);
                // Set other user fields...

                // Save the profile picture and get the URL
                profilePicUrl = saveProfilePic(profilePic, user);
            }

            // 4. Create the user object and set values
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPhoneNumber(phoneNumber);
            user.setPassword(password);
            user.setJobOption(jobOption);
            user.setCurrentRole(currentRole);
            user.setExperience(experience);
            user.setIndustry(industry);
            user.setCurrentEmployer(currentEmployer);
            user.setKeySkills(keySkills);
            user.setCollege(college);
            user.setJobId(jobId);
            user.setCity(city);
            user.setEstablishedYear(establishedYear);
            user.setEnabled(enabled);

            // Set the profile picture URL
            if (profilePicUrl != null) {
                user.setProfilepicurl(profilePicUrl);
            }

            // 5. Register the new user
            userService.registerNewUser(user);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Registration successful. Please check your email to verify your account.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error during signup: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during registration.");
        }
    }

    // Method to save profile picture in the "profilepic" folder and return the URL
    private String saveProfilePic(MultipartFile profilePic, User user) throws IOException {
        // Absolute path to the uploads/profilepic directory
        String uploadDir = System.getProperty("user.dir") + "/uploads/profilepic";  // Absolute path

        // Create the directory if it doesn't exist
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();  // Create the directory
            if (!created) {
                throw new IOException("Failed to create directory for file uploads.");
            }
        }

        // Get user-specific information for file naming
        String firstName = user.getFirstName();  // User's first name

        // Get the original file extension
        String originalFileName = profilePic.getOriginalFilename();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));

        // Generate a base file name using the user's first name
        String baseFileName = firstName + extension;

        // Check if the file already exists, if yes, append a number to the name
        File destFile = new File(uploadDir + File.separator + baseFileName);
        int counter = 1;
        while (destFile.exists()) {
            // If the file exists, create a new name with the counter
            String newFileName = firstName + "_" + counter + extension;
            destFile = new File(uploadDir + File.separator + newFileName);
            counter++;
        }

        // Save the file to the "profilepic" directory
        profilePic.transferTo(destFile);

        // Return the URL where the profile picture can be accessed
        return "https://wezume.in/uploads/profilepic/" + destFile.getName();
    }

    @PostMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestBody Map<String, String> emailPayload) {
        String email = emailPayload.get("email");
        boolean exists = userRepository.existsByEmail(email);
        return ResponseEntity.ok(Collections.singletonMap("exists", exists));
    }

    @PostMapping("/check-Recruteremail")
    public ResponseEntity<Map<String, Object>> checkRecEmail(@RequestBody Map<String, String> emailPayload) {
        String email = emailPayload.get("email");

        // List of restricted domains
        List<String> restrictedDomains = List.of("gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "example.com");

        // Extract the domain from the email
        String[] emailParts = email.split("@");
        if (emailParts.length != 2) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Invalid email format"));
        }
        String emailDomain = emailParts[1];

        // Check if the domain is restricted
        if (restrictedDomains.contains(emailDomain.toLowerCase())) {
            return ResponseEntity.ok(Collections.singletonMap("error", "Public email domains are not allowed"));
        }

        // Check if the email already exists in the database
        boolean exists = userRepository.existsByEmail(email);
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);

        if (!exists) {
            response.put("message", "Email is valid and can be used.");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/check-phone")
    public ResponseEntity<Boolean> checkPhone(@RequestBody String phoneNumber) {
        boolean phoneExists = userService.isPhoneExists(phoneNumber);
        return ResponseEntity.ok(phoneExists);
    }

    @GetMapping("/user/{userId}/profilepic")
    public ResponseEntity<byte[]> getUserProfilePic(@PathVariable Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        User user = userOptional.get();
        if (user.getProfilePic() != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG) // Set the appropriate content type
                    .body(user.getProfilePic());        // Send the byte array (image) in the response body
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PutMapping("/update/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable Long userId,
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "jobOption", required = false) String jobOption,
            @RequestParam(value = "currentRole", required = false) String currentRole,
            @RequestParam(value = "experience", required = false) String experience,
            @RequestParam(value = "industry", required = false) String industry,
            @RequestParam(value = "currentEmployer", required = false) String currentEmployer,
            @RequestParam(value = "keySkills", required = false) String keySkills,
            @RequestParam(value = "college", required = false) String college,
            @RequestParam(value = "jobId", required = false) String jobId,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "establishedYear", required = false) Integer establishedYear,
            @RequestParam(value = "enabled", required = false) boolean enabled,
            @RequestParam(value = "profilePic", required = false) MultipartFile profilePic) {
        try {
            // Fetch the existing user to be updated
            User existingUser = userService.getUserById(userId);
            if (existingUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Update the user fields
            if (firstName != null) {
                existingUser.setFirstName(firstName);
            }
            if (lastName != null) {
                existingUser.setLastName(lastName);
            }
            if (email != null) {
                existingUser.setEmail(email);
            }
            if (phoneNumber != null) {
                existingUser.setPhoneNumber(phoneNumber);
            }
            if (password != null) {
                existingUser.setPassword(password);
            }
            if (jobOption != null) {
                existingUser.setJobOption(jobOption);
            }
            if (currentRole != null) {
                existingUser.setCurrentRole(currentRole);
            }
            if (experience != null) {
                existingUser.setExperience(experience);
            }
            if (industry != null) {
                existingUser.setIndustry(industry);
            }
            if (currentEmployer != null) {
                existingUser.setCurrentEmployer(currentEmployer);
            }
            if (keySkills != null) {
                existingUser.setKeySkills(keySkills);
            }
            if (college != null) {
                existingUser.setCollege(college);
            }
            if (jobId != null) {
                existingUser.setJobId(jobId);
            }
            if (city != null) {
                existingUser.setCity(city);
            }
            if (establishedYear != null) {
                existingUser.setEstablishedYear(establishedYear);
            }
            if (enabled) {
                existingUser.setEnabled(enabled);
            }

            // Handle the profile picture update
            if (profilePic != null && !profilePic.isEmpty()) {
                // Save the profile picture and get the URL
                String profilePicUrl = saveProfilePic(profilePic, existingUser);
                existingUser.setProfilepicurl(profilePicUrl);  // Update the user's profilePic URL
            }

            // Update the user in the database
            User updatedUser = userService.updateUser(userId, existingUser);

            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/get/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable Long userId) {
        Optional<User> user = userRepository.findById(userId);

        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkUserExists(@RequestParam String email) {
        Optional<User> user = userService.findByEmail(email);

        if (user.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("exists", true);
            response.put("jobOption", user.get().getJobOption());
            response.put("email", user.get().getEmail());
            response.put("userId", user.get().getId());
            response.put("firstName", user.get().getFirstName());
            response.put("phoneNumber", user.get().getPhoneNumber());
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("exists", false);
            return ResponseEntity.ok(response);
        }
    }

    @PutMapping("/update-password")
    public ResponseEntity<String> updatePassword(@RequestParam String email, @RequestParam String newPassword) {
        boolean isUpdated = userService.updatePasswordByEmail(email, newPassword);
        if (isUpdated) {
            return ResponseEntity.ok("Password updated successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }
    }

    @GetMapping("/share")
    public ResponseEntity<String> handleDeepLink(@RequestParam String target) {
        String fallbackUrlAndroid = "https://play.google.com/store/apps/details?id=com.vprofile";
        String fallbackUrlIOS = "https://apps.apple.com/in/app/wezume/id6740565222 ";

        String htmlResponse = "<html><head>"
                + "<script>"
                + "  function openApp() {"
                + "    window.location.href ='" + target + "'; "
                + "    setTimeout(function() {"
                + "        var userAgent = navigator.userAgent || navigator.vendor || window.opera;"
                + "        if (/android/i.test(userAgent)) {"
                + "          window.location.href = '" + fallbackUrlAndroid + "';"
                + "        } else if (/iPad|iPhone|iPod/.test(userAgent) && !window.MSStream) {"
                + "          window.location.href = '" + fallbackUrlIOS + "';"
                + "        }"
                + "    }, 5000);"
                + "  }"
                + "  openApp();"
                + "</script>"
                + "</head><body>"
                + "Redirecting..."
                + "<p>If nothing happens, <a href='" + fallbackUrlAndroid + "'>click here to download the app</a>.</p>"
                + "</body></html>";

        System.out.println("htmlResponse: " + htmlResponse);

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(htmlResponse);
    }

    @GetMapping("/signup-count")
    public long getTotalUsers() {
        return userRepository.countAllUsers();
    }

    @GetMapping("/recruiters-count")
    public long getRecruitersCount() {
        return userRepository.countRecruiters(); // from above
    }

    @GetMapping("/recruiters")
    public List<User> getRecruiters() {
        return userRepository.findByJobOption("Employer");
    }

}
