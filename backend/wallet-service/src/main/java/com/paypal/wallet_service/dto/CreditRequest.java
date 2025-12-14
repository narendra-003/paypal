package com.paypal.wallet_service.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreditRequest {
    private Long userId;
    private String currency;
    private Long amount;
}
