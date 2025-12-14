package com.paypal.wallet_service.scheduler;

import com.paypal.wallet_service.dto.ReleaseRequest;
import com.paypal.wallet_service.entity.WalletHold;
import com.paypal.wallet_service.repository.WalletHoldRepository;
import com.paypal.wallet_service.service.WalletService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class HoldExpiryScheduler {
    private final WalletHoldRepository walletHoldRepository;
    private final WalletService walletService;


    public HoldExpiryScheduler(WalletHoldRepository walletHoldRepository, WalletService walletService) {
        this.walletHoldRepository = walletHoldRepository;
        this.walletService = walletService;
    }

    // runs every minute by default; make configurable if needed
    @Scheduled(fixedRateString = "${wallet.hold.expiry.scan-rate-ms:60000}")
    public void expireHolds() {
        LocalDateTime now = LocalDateTime.now();

        // simple: fetch expired active holds (OK for small data sets)
        List<WalletHold> expiredHolds = walletHoldRepository.findByStatusAndExpiresAtBefore("ACTIVE", now);

        for(WalletHold expiredHold : expiredHolds) {
            String holdReference = expiredHold.getHoldReference();
            ReleaseRequest releaseRequest = new ReleaseRequest(holdReference);
            try {
                // reuse existing release logic (locks, audit, idempotency)
                walletService.releaseHold(releaseRequest);
                System.out.println("Expired hold released: " + releaseRequest);
            } catch (Exception e) {
                // log and continue - don't block the sweep
                System.err.println("Failed to release expired hold " + releaseRequest + ": " + e.getMessage());
            }
        }
    }
}
