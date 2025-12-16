package com.paypal.notification_service.controller;

import com.paypal.notification_service.entity.Notification;
import com.paypal.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notify")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public Notification sendNotification(@Valid @RequestBody Notification notification) {
        logger.info("Received request to send notification to user: {}", notification.getUserId());
        logger.debug("Notification message: {}", notification.getMessage());
        Notification sentNotification = notificationService.sendNotification(notification);
        logger.info("Successfully sent notification with ID: {}", sentNotification.getId());
        return sentNotification;
    }

    @GetMapping("/{userId}")
    public List<Notification> getNotificationByUserId(@PathVariable Long userId) {
        logger.info("Received request to fetch notifications for user: {}", userId);
        List<Notification> notifications = notificationService.getNotificationByUserId(userId);
        logger.info("Retrieved {} notifications for user: {}", notifications.size(), userId);
        return notifications;
    }
}
