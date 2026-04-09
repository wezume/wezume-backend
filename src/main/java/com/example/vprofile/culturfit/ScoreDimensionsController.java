package com.example.vprofile.culturfit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/score")
public class ScoreDimensionsController {

    // ─── Culture Fit endpoint ────────────────────────────────────────────────

    /**
     * POST /api/score/culturefit
     * Body:
     * {
     *   "company":   { "mission":4, "pace":4, "collaboration":4, "process":5, "learning":4 },
     *   "candidate": { "mission":3, "pace":4, "collaboration":5, "process":4, "learning":4 }
     * }
     * Returns: { "percent": 85, "insights": { "strong": "pace, learning", "gap": "process" } }
     *
     * Mirrors the TypeScript score service: rank-based 1-4 max-diff scoring.
     */
    @PostMapping("/culturefit")
    public ResponseEntity<?> cultureFit(@RequestBody Map<String, Map<String, Number>> body) {
        Map<String, Number> company   = body.get("company");
        Map<String, Number> candidate = body.get("candidate");

        if (company == null || candidate == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid"));
        }

        String[] keys = {"mission", "pace", "collaboration", "process", "learning"};

        // Validate all keys are present
        for (String k : keys) {
            if (!company.containsKey(k) || !candidate.containsKey(k)) {
                return ResponseEntity.badRequest().body(Map.of("error", "invalid"));
            }
        }

        // Calculate percent (same formula as TS)
        double sum = 0;
        for (String k : keys) {
            double a = company.get(k).doubleValue();
            double b = candidate.get(k).doubleValue();
            double diff = Math.abs(a - b);
            sum += (1.0 - diff / 4.0);
        }
        int percent = (int) Math.round((sum / keys.length) * 100);

        // Build insights: sort by diff, report 2 best (smallest diff = most aligned)
        List<double[]> diffs = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            double diff = Math.abs(company.get(keys[i]).doubleValue() - candidate.get(keys[i]).doubleValue());
            diffs.add(new double[]{i, diff});
        }
        diffs.sort(Comparator.comparingDouble(d -> d[1]));

        String strong = keys[(int) diffs.get(0)[0]] + ", " + keys[(int) diffs.get(1)[0]];
        String gap    = keys[(int) diffs.get(diffs.size() - 1)[0]];

        Map<String, String> insights = Map.of("strong", strong, "gap", gap);
        return ResponseEntity.ok(Map.of("percent", percent, "insights", insights));
    }

    // ─── Candidate dimensions endpoint ──────────────────────────────────────

    /**
     * POST /api/score/candidate-dimensions
     * Body: { "text": "..." }
     * Returns: { "dimensions": { mission, pace, collaboration, process, learning } }
     *
     * Mirrors the TypeScript score route: keyword frequency → score 1-5.
     */
    @PostMapping("/candidate-dimensions")
    public ResponseEntity<?> candidateDimensions(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid"));
        }

        String t = text.toLowerCase();

        int mission       = Math.min(5, 1 + count(t, "mission|impact|customer|purpose"));
        int pace          = Math.min(5, 1 + count(t, "fast|speed|scrappy|autonomy|ownership"));
        int collaboration = Math.min(5, 1 + count(t, "team|collaborat|communicat|async|feedback"));
        int process       = Math.min(5, 1 + count(t, "quality|craft|rigor|testing|documentation"));
        int learning      = Math.min(5, 1 + count(t, "learn|growth|experiment|innovat|share"));

        Map<String, Integer> dimensions = Map.of(
                "mission",       mission,
                "pace",          pace,
                "collaboration", collaboration,
                "process",       process,
                "learning",      learning
        );

        return ResponseEntity.ok(Map.of("dimensions", dimensions));
    }

    /** Count how many times any alternative in the regex pattern appears in text. */
    private int count(String text, String pattern) {
        Matcher m = Pattern.compile(pattern).matcher(text);
        int count = 0;
        while (m.find()) count++;
        return count;
    }
}
