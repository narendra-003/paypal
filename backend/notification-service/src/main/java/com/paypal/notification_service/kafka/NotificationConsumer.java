package com.paypal.notification_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.notification_service.dto.TransactionDto;
import com.paypal.notification_service.entity.Notification;
import com.paypal.notification_service.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NotificationConsumer {
    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;


    public NotificationConsumer(NotificationRepository notificationRepository, ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "txn-initiated", groupId = "notification-group")
    public void listener(TransactionDto transaction) throws JsonProcessingException {
        logger.info("Received transaction from Kafka - TransactionId: {}, Sender: {}, Receiver: {}, Amount: {}",
                transaction.getId(), transaction.getSenderId(), transaction.getReceiverId(), transaction.getAmount());

        Notification notification = new Notification();

        Long senderUserId = transaction.getSenderId();
        notification.setUserId(senderUserId);
        String notifyMessage = "$ " + transaction.getAmount() + " received from user " + senderUserId;
        notification.setMessage(notifyMessage);
        notification.setSentAt(LocalDateTime.now());

        notificationRepository.save(notification);
        logger.info("Notification saved - UserId: {}, Message: {}", senderUserId, notifyMessage);
    }
}
