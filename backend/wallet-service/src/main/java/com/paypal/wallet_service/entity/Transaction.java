package com.paypal.wallet_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// TODO need to change - we have two transaction entity now
@Entity
@Table(name = "transactions")
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long walletId;

    @Column(nullable = false)
    private String type; // CREDIT, DEBIT, HOLD, RELEASE, CAPTURE

    @Column(nullable = false)
    private Long amount; // stored in paise/cents

    @Column(nullable = false)
    private String status; // SUCCESS, FAILED, EXPIRED

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Transaction() {}

    public Transaction(Long walletId, String type, Long amount, String status) {
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }
}
