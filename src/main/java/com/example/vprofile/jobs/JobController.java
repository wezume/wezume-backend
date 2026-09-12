package com.example.vprofile.jobs;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private static final String ADMIN_EMAIL = "pitch@wezume.com";

    @Autowired
    private JobService jobService;

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @PostMapping("/add")
    public ResponseEntity<?> addJob(@RequestBody Job job) {
        if (!isAdmin()) {
            return forbidden();
        }
        return ResponseEntity.ok(jobService.addJob(job));
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<?> editJob(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        if (!isAdmin()) {
            return forbidden();
        }
        String field = payload.get("field");
        String value = payload.get("value");
        try {
            return ResponseEntity.ok(jobService.updateJobField(id, field, value));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        if (!isAdmin()) {
            return forbidden();
        }
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null && ADMIN_EMAIL.equalsIgnoreCase(auth.getName());
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "You don't have edit access to this list."));
    }
}
