package com.paypal.transaction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TransferRequest {

    private String senderId;
    private String receiverId;
    private Double amount;
}
