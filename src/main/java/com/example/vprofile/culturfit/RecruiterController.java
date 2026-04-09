package com.example.vprofile.culturfit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recruiter")
public class RecruiterController {

    private static final List<String> BLOCKED_DOMAINS =
            List.of("mailinator.com", "tempmail.com", "guerrillamail.com", "throwam.com");

    /**
     * POST /api/recruiter/verify
     * Body: { "email": "...", "domain": "..." }
     * Returns: { "verified": true|false }
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyRecruiter(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String domain = body.get("domain");

        if (email == null || email.isBlank() || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid"));
        }
        if (domain == null || domain.length() < 3) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid"));
        }

        String emailDomain = email.substring(email.indexOf('@') + 1);
        boolean looksValid = domain.contains(".");
        boolean notDisposable = !BLOCKED_DOMAINS.contains(emailDomain.toLowerCase());
        boolean verified = looksValid && notDisposable;

        return ResponseEntity.ok(Map.of("verified", verified));
    }
}
