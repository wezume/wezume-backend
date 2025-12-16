package com.example.vprofile.notification;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.vprofile.videofolder.Video;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public void saveNotification(Video video, String likerName) {
        Notification notification = new Notification();
        notification.setVideoId(video.getId());  // Extract videoId from Video object
        notification.setLikerName(likerName);
        notification.setUserId(video.getUserId());  // Assuming User ID is stored in Video
        
        notificationRepository.save(notification); // Save the notification
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        // Retrieve unread notifications for the specified userId
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }

    public void markNotificationsAsRead(List<Long> notificationIds) {
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        notifications.forEach(notification -> notification.setRead(true)); // Mark notifications as read
        notificationRepository.saveAll(notifications); // Save updated notifications
    }
}
