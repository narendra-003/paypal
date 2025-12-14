package com.paypal.wallet_service.dto;

import com.paypal.wallet_service.enums.WalletHoldStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class HoldResponse {
    private String holdReference;
    private Long amount;
    private WalletHoldStatus status;
}
