package com.paypal.wallet_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaptureRequest {
    private String holdReference;
}
