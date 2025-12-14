package com.paypal.wallet_service.controller;

import com.paypal.wallet_service.dto.*;
import com.paypal.wallet_service.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@RequestBody CreateWalletRequest createWalletRequest) {
        return ResponseEntity.ok(walletService.createWallet(createWalletRequest));
    }

    @PostMapping("/credit")
    public ResponseEntity<WalletResponse> credit(@RequestBody CreditRequest creditRequest) {
        return ResponseEntity.ok(walletService.credit(creditRequest));
    }

    @PostMapping("/debit")
    public ResponseEntity<WalletResponse> debit(@RequestBody DebitRequest debitRequest) {
        return ResponseEntity.ok(walletService.debit(debitRequest));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getWallet(userId));
    }

    @PostMapping("/hold")
    public ResponseEntity<HoldResponse> placeHold(@RequestBody HoldRequest holdRequest) {
        return ResponseEntity.ok(walletService.placeHold(holdRequest));
    }

    @PostMapping("/capture")
    public ResponseEntity<WalletResponse> capture(@RequestBody CaptureRequest captureRequest) {
        return ResponseEntity.ok(walletService.captureHold(captureRequest));
    }

    @PostMapping("/release")
    public ResponseEntity<HoldResponse> release(@RequestBody ReleaseRequest releaseRequest) {
        return ResponseEntity.ok(walletService.releaseHold(releaseRequest));
    }
}
