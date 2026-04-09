package com.example.vprofile.culturfit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * POST /api/company/values
     * Body: { "domain": "example.com" }
     * Returns: { "dimensions": { mission, pace, collaboration, process, learning } }
     *
     * Mirrors the TypeScript company route: fetches the homepage and scans for keywords.
     */
    @PostMapping("/values")
    public ResponseEntity<?> getCompanyValues(@RequestBody Map<String, String> body) {
        String domain = body.get("domain");
        if (domain == null || domain.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid"));
        }

        String pageText = "";
        try {
            String url = (domain.startsWith("http") ? domain : "https://" + domain);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            pageText = response.body().toLowerCase();
        } catch (Exception ignored) {
            // If the request fails, use empty text — same behaviour as TypeScript
        }

        int mission       = pageText.contains("mission")  ? 4 : 3;
        int pace          = pageText.contains("fast")      ? 4 : 3;
        int collaboration = pageText.contains("team")      ? 4 : 3;
        int process       = pageText.contains("quality")   ? 4 : 3;
        int learning      = pageText.contains("learn")     ? 4 : 3;

        Map<String, Integer> dimensions = Map.of(
                "mission",       mission,
                "pace",          pace,
                "collaboration", collaboration,
                "process",       process,
                "learning",      learning
        );

        return ResponseEntity.ok(Map.of("dimensions", dimensions));
    }
}
