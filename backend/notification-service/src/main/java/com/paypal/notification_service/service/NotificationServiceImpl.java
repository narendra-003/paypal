package com.paypal.notification_service.service;

import com.paypal.notification_service.entity.Notification;
import com.paypal.notification_service.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification sendNotification(Notification notification) {
        logger.info("Sending notification to user: {}", notification.getUserId());
        logger.debug("Notification message: {}", notification.getMessage());
        notification.setSentAt(LocalDateTime.now());
        Notification savedNotification = notificationRepository.save(notification);
        logger.info("Successfully sent notification with ID: {}", savedNotification.getId());
        return savedNotification;
    }

    @Override
    public List<Notification> getNotificationByUserId(Long userId) {
        logger.debug("Fetching notifications for user: {}", userId);
        List<Notification> notifications = notificationRepository.findByUserId(userId);
        logger.info("Found {} notifications for user: {}", notifications.size(), userId);
        return notifications;
    }
}
