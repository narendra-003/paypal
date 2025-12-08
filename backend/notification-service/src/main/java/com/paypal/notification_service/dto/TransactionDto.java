package com.paypal.notification_service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransactionDto {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private Double amount;
    private LocalDateTime timestamp;
    private String status;
}
