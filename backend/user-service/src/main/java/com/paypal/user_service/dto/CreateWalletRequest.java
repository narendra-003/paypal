package com.paypal.user_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWalletRequest {

    private Long userId;
    private String currency;
}
