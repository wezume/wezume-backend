package com.example.vprofile.logincredentials;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bulk-users")
public class BulkUserController {

    @Autowired
    private BulkUserService bulkUserService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadUsers(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }

        try {
            List<User> savedUsers = bulkUserService.processFile(file);
            return ResponseEntity.ok(Map.of(
                    "message", "Successfully uploaded " + savedUsers.size() + " users.",
                    "count", savedUsers.size()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error processing file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An unexpected error occurred: " + e.getMessage());
        }
    }
}
