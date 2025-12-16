package com.example.vprofile.notification;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Find notifications by userId and unread status (isRead = false)
    List<Notification> findByUserIdAndIsReadFalse(Long userId);
}
