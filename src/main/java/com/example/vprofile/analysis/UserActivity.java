package com.example.vprofile.analysis;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_activity")
public class UserActivity {

    @Id
    private Long userId;

    private String name;
    private String jobOption;

    private LocalDateTime lastActiveTime;
    private String totalActiveTime = "00:00:00";

    private LocalDateTime lastResetTime;

    public UserActivity() {
    }

    public UserActivity(Long userId, String name, String jobOption, LocalDateTime lastActiveTime,
            String totalActiveTime, LocalDateTime lastResetTime) {
        this.userId = userId;
        this.name = name;
        this.jobOption = jobOption;
        this.lastActiveTime = lastActiveTime;
        this.totalActiveTime = totalActiveTime;
        this.lastResetTime = lastResetTime;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJobOption() {
        return jobOption;
    }

    public void setJobOption(String jobOption) {
        this.jobOption = jobOption;
    }

    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(LocalDateTime lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public String getTotalActiveTime() {
        return (totalActiveTime == null ? "00:00:00" : totalActiveTime);
    }

    public void setTotalActiveTime(String totalActiveTime) {
        this.totalActiveTime = (totalActiveTime == null ? "00:00:00" : totalActiveTime);
    }

    public LocalDateTime getLastResetTime() {
        return lastResetTime;
    }

    public void setLastResetTime(LocalDateTime lastResetTime) {
        this.lastResetTime = lastResetTime;
    }

    public static String formatSecondsToHHMMSS(long seconds) {
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long secs = seconds % 60;
    return String.format("%02d:%02d:%02d", hours, minutes, secs);
}

}
