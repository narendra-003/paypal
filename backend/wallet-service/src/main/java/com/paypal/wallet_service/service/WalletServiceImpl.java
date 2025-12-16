package com.paypal.wallet_service.service;

import com.paypal.wallet_service.dto.*;
import com.paypal.wallet_service.entity.Wallet;
import com.paypal.wallet_service.entity.WalletHold;
import com.paypal.wallet_service.enums.WalletHoldStatus;
import com.paypal.wallet_service.exception.InsufficientFundsException;
import com.paypal.wallet_service.exception.NotFoundException;
import com.paypal.wallet_service.repository.WalletHoldRepository;
import com.paypal.wallet_service.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WalletServiceImpl implements WalletService{
    private static final Logger logger = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final WalletRepository walletRepository;
    private final WalletHoldRepository walletHoldRepository;

    public WalletServiceImpl(WalletRepository walletRepository, WalletHoldRepository walletHoldRepository) {
        this.walletRepository = walletRepository;
        this.walletHoldRepository = walletHoldRepository;
    }

    @Override
    @Transactional
    public WalletResponse createWallet(CreateWalletRequest walletRequest) {
        Wallet wallet = new Wallet(walletRequest.getUserId(), walletRequest.getCurrency());
        Wallet savedWallet = walletRepository.save(wallet);
        WalletResponse walletResponse = new WalletResponse(savedWallet.getId(), savedWallet.getUserId(), savedWallet.getCurrency(), savedWallet.getBalance(), savedWallet.getAvailableBalance());
        return walletResponse;
    }

    @Override
    @Transactional
    public WalletResponse credit(CreditRequest creditRequest) {
        logger.info("CREDIT request received - UserId: {}, Currency: {}, Amount: {}",
                creditRequest.getUserId(), creditRequest.getCurrency(), creditRequest.getAmount());

        Wallet wallet = walletRepository.findByUserIdAndCurrency(creditRequest.getUserId(), "INR")
                .orElseThrow(() -> new NotFoundException("Wallet not found for user: " + creditRequest.getUserId()));

        wallet.setBalance(wallet.getBalance() + creditRequest.getAmount());
        wallet.setAvailableBalance(wallet.getAvailableBalance() + creditRequest.getAmount());
        // TODO should update updatedAt field
        Wallet savedWallet = walletRepository.save(wallet);
        Long amount = creditRequest.getAmount();

//        Transaction transaction = new Transaction(wallet.getId(), "CREDIT", amount, "SUCCESS");
//        transactionRepository.save(transaction);

        logger.info("CREDIT completed - WalletId: {}, NewBalance: {}, AvailableBalance: {}",
                savedWallet.getId(), savedWallet.getBalance(), savedWallet.getAvailableBalance());
        WalletResponse walletResponse = new WalletResponse(savedWallet.getId(), savedWallet.getUserId(),
                savedWallet.getCurrency(), savedWallet.getBalance(), savedWallet.getAvailableBalance());
        return walletResponse;
    }

    @Override
    @Transactional
    public WalletResponse debit(DebitRequest debitRequest) {
        logger.info("DEBIT request received - UserId: {}, Currency: {}, Amount: {}",
                debitRequest.getUserId(), debitRequest.getCurrency(), debitRequest.getAmount());

        Wallet wallet = walletRepository.findByUserIdAndCurrency(debitRequest.getUserId(), "INR")
                .orElseThrow(() -> new NotFoundException("Wallet not found for user: " + debitRequest.getUserId()));

        if (wallet.getAvailableBalance() < debitRequest.getAmount()) throw new InsufficientFundsException("Not enough balance");

        wallet.setBalance(wallet.getBalance() - debitRequest.getAmount());
        wallet.setAvailableBalance(wallet.getAvailableBalance() - debitRequest.getAmount());
        // TODO should update updatedAt field
        Wallet savedWallet = walletRepository.save(wallet);

        logger.info("DEBIT completed - WalletId: {}, NewBalance: {}, AvailableBalance: {}",
                savedWallet.getId(), savedWallet.getBalance(), savedWallet.getAvailableBalance());

        WalletResponse walletResponse = new WalletResponse(savedWallet.getId(), savedWallet.getUserId(),
                savedWallet.getCurrency(), savedWallet.getBalance(), savedWallet.getAvailableBalance());
        return walletResponse;
    }

    @Override
    public WalletResponse getWallet(Long userId) {
        Wallet savedWallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Wallet not found for user: " + userId));

        WalletResponse walletResponse = new WalletResponse(savedWallet.getId(), savedWallet.getUserId(),
                savedWallet.getCurrency(), savedWallet.getBalance(), savedWallet.getAvailableBalance());
        return walletResponse;
    }

    @Override
    @Transactional
    public HoldResponse placeHold(HoldRequest holdRequest) {
        Wallet wallet = walletRepository.findByUserIdAndCurrency(holdRequest.getUserId(), "INR")
                .orElseThrow(() -> new NotFoundException("Wallet not found for user: " + holdRequest.getUserId()));

        if (wallet.getAvailableBalance() < holdRequest.getAmount()) throw new InsufficientFundsException("Not enough balance");
        wallet.setAvailableBalance(wallet.getAvailableBalance() - holdRequest.getAmount());

        WalletHold walletHold = new WalletHold();
        walletHold.setWallet(wallet);
        walletHold.setAmount(holdRequest.getAmount());
        walletHold.setHoldReference("HOLD_" + System.currentTimeMillis());
        walletHold.setStatus(WalletHoldStatus.ACTIVE);

        walletRepository.save(wallet);
        walletHoldRepository.save(walletHold);

        HoldResponse holdResponse = new HoldResponse(walletHold.getHoldReference(), walletHold.getAmount(), walletHold.getStatus());
        return holdResponse;
    }

    @Override
    @Transactional
    public WalletResponse captureHold(CaptureRequest captureRequest) {
        WalletHold walletHold = walletHoldRepository.findByHoldReference(captureRequest.getHoldReference())
                .orElseThrow(() -> new NotFoundException("Hold not found"));

        if (walletHold.getStatus() != WalletHoldStatus.ACTIVE) {
            throw new IllegalStateException("Hold is not active");
        }

        Wallet wallet = walletHold.getWallet();
        wallet.setBalance(wallet.getBalance() - walletHold.getAmount());

        walletHold.setStatus(WalletHoldStatus.CAPTURED);
        walletRepository.save(wallet);
        walletHoldRepository.save(walletHold);

        WalletResponse walletResponse = new WalletResponse(wallet.getId(), wallet.getUserId(),
                wallet.getCurrency(), wallet.getBalance(), wallet.getAvailableBalance());
        return walletResponse;
    }

    @Override
    @Transactional
    public HoldResponse releaseHold(ReleaseRequest releaseRequest) {
        WalletHold walletHold = walletHoldRepository.findByHoldReference(releaseRequest.getHoldReference())
                .orElseThrow(() -> new NotFoundException("Hold not found"));

        if (walletHold.getStatus() != WalletHoldStatus.ACTIVE) {
            throw new IllegalStateException("Hold is not active");
        }

        Wallet wallet = walletHold.getWallet();
        wallet.setBalance(wallet.getAvailableBalance() + walletHold.getAmount());

        walletHold.setStatus(WalletHoldStatus.RELEASED);
        walletRepository.save(wallet);
        walletHoldRepository.save(walletHold);

        HoldResponse holdResponse = new HoldResponse(walletHold.getHoldReference(), walletHold.getAmount(), walletHold.getStatus());
        return holdResponse;
    }


}
