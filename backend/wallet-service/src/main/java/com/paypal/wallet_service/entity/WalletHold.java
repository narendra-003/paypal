package com.paypal.wallet_service.entity;

import com.paypal.wallet_service.enums.WalletHoldStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_holds")
@Data
public class WalletHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Column(nullable = false)
    private String holdReference; // unique ID for each hold

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletHoldStatus status = WalletHoldStatus.ACTIVE; // ACTIVE, CAPTURED, RELEASED

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime expiresAt;
}
