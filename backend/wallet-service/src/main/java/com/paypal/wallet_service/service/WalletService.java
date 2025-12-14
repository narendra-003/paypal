package com.paypal.wallet_service.service;

import com.paypal.wallet_service.dto.*;

public interface WalletService {

    WalletResponse createWallet(CreateWalletRequest walletRequest);
    WalletResponse credit(CreditRequest creditRequest);
    WalletResponse debit(DebitRequest debitRequest);
    WalletResponse getWallet(Long userId);
    HoldResponse placeHold(HoldRequest holdRequest);
    WalletResponse captureHold(CaptureRequest captureRequest);
    HoldResponse releaseHold(ReleaseRequest releaseRequest);
}
