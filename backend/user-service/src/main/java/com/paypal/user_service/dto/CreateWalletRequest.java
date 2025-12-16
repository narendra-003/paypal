package com.paypal.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWalletRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Currency is required")
    private String currency;
}
