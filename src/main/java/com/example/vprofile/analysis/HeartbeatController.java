package com.example.vprofile.analysis;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.vprofile.logincredentials.User;
import com.example.vprofile.logincredentials.UserRepository;

@RestController
@RequestMapping("/api")
public class HeartbeatController {

    @Autowired
    private UserActivityRepository activityRepo;

    @Autowired
    private UserRepository userRepo;

    @PostMapping("/heartbeat")
    public ResponseEntity<String> heartbeat(@RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        if (userId == null || !userRepo.existsById(userId)) {
            return ResponseEntity.badRequest().body("Invalid user ID");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = userRepo.findById(userId).get();

        UserActivity activity = activityRepo.findById(userId).orElseGet(() -> {
            UserActivity ua = new UserActivity();
            ua.setUserId(userId);
            ua.setName(user.getFirstName());
            ua.setJobOption(user.getJobOption());
            ua.setLastActiveTime(now);
            ua.setLastResetTime(now);
            ua.setTotalActiveTime("00:00:00");
            return ua;
        });

        // Check if 7 days have passed since last reset
        if (activity.getLastResetTime() != null &&
                Duration.between(activity.getLastResetTime(), now).toDays() >= 7) {
            activity.setTotalActiveTime("00:00:00");
            activity.setLastResetTime(now);
        }

        // Calculate and update active time
        LocalDateTime lastTime = activity.getLastActiveTime();
        if (lastTime != null) {
            long seconds = Duration.between(lastTime, now).getSeconds();
            if (seconds < 600) { // only count if less than 10 mins
                String[] timeParts = activity.getTotalActiveTime().split(":");
                long hours = Long.parseLong(timeParts[0]);
                long minutes = Long.parseLong(timeParts[1]);
                long secs = Long.parseLong(timeParts[2]);
                long currentTotalSeconds = hours * 3600 + minutes * 60 + secs;

                currentTotalSeconds += seconds;
                activity.setTotalActiveTime(UserActivity.formatSecondsToHHMMSS(currentTotalSeconds));
            }
        }

        activity.setLastActiveTime(now);
        activityRepo.save(activity);

        return ResponseEntity.ok("Activity tracked");
    }

    @GetMapping("/weekly-active-users")
    public List<Map<String, Object>> getWeeklyActiveUsers() {
        List<UserActivity> activities = activityRepo.findAll();

        return activities.stream().map(activity -> {
            String[] timeParts = activity.getTotalActiveTime().split(":");
            long hours = Long.parseLong(timeParts[0]);
            long minutes = Long.parseLong(timeParts[1]);
            long seconds = Long.parseLong(timeParts[2]);
            long totalSeconds = hours * 3600 + minutes * 60 + seconds;
            Duration d = Duration.ofSeconds(totalSeconds);

            Map<String, Object> map = new HashMap<>();
            map.put("userId", activity.getUserId());
            map.put("name", activity.getName());
            map.put("jobOption", activity.getJobOption());
            map.put("formattedTime", String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart()));
            map.put("seconds", totalSeconds);
            return map;
        }).collect(Collectors.toList());
    }
}
