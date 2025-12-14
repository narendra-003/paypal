package com.paypal.wallet_service.service;

import com.paypal.wallet_service.dto.*;
import com.paypal.wallet_service.entity.Transaction;
import com.paypal.wallet_service.entity.Wallet;
import com.paypal.wallet_service.entity.WalletHold;
import com.paypal.wallet_service.enums.WalletHoldStatus;
import com.paypal.wallet_service.exception.InsufficientFundsException;
import com.paypal.wallet_service.exception.NotFoundException;
import com.paypal.wallet_service.repository.TransactionRepository;
import com.paypal.wallet_service.repository.WalletHoldRepository;
import com.paypal.wallet_service.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class WalletServiceImpl implements WalletService{
    private final WalletRepository walletRepository;
    private final WalletHoldRepository walletHoldRepository;
    private final TransactionRepository transactionRepository;

    public WalletServiceImpl(WalletRepository walletRepository, WalletHoldRepository walletHoldRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.walletHoldRepository = walletHoldRepository;
        this.transactionRepository = transactionRepository;
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
        System.out.println("CREDIT Request received: " + creditRequest);

        Wallet wallet = walletRepository.findByUserIdAndCurrency(creditRequest.getUserId(), "INR")
                .orElseThrow(() -> new NotFoundException("Wallet not found for user: " + creditRequest.getUserId()));

        wallet.setBalance(wallet.getBalance() + creditRequest.getAmount());
        wallet.setAvailableBalance(wallet.getAvailableBalance() + creditRequest.getAmount());
        // TODO should update updatedAt field
        Wallet savedWallet = walletRepository.save(wallet);
        Long amount = creditRequest.getAmount();

//        Transaction transaction = new Transaction(wallet.getId(), "CREDIT", amount, "SUCCESS");
//        transactionRepository.save(transaction);

        System.out.println("CREDIT Done: walletID= " + savedWallet.getId() + ", newBalance= " + savedWallet.getBalance()
                + ", availableBalance= " + savedWallet.getAvailableBalance());
        WalletResponse walletResponse = new WalletResponse(savedWallet.getId(), savedWallet.getUserId(),
                savedWallet.getCurrency(), savedWallet.getBalance(), savedWallet.getAvailableBalance());
        return walletResponse;
    }

    @Override
    @Transactional
    public WalletResponse debit(DebitRequest debitRequest) {
        System.out.println("DEBIT request received: userId= " + debitRequest.getUserId() +
                ", amount= " + debitRequest.getAmount() +
                ", currency= " + debitRequest.getCurrency());

        Wallet wallet = walletRepository.findByUserIdAndCurrency(debitRequest.getUserId(), "INR")
                .orElseThrow(() -> new NotFoundException("Wallet not found for user: " + debitRequest.getUserId()));

        if (wallet.getAvailableBalance() < debitRequest.getAmount()) throw new InsufficientFundsException("Not enough balance");

        wallet.setBalance(wallet.getBalance() - debitRequest.getAmount());
        wallet.setAvailableBalance(wallet.getAvailableBalance() - debitRequest.getAmount());
        // TODO should update updatedAt field
        Wallet savedWallet = walletRepository.save(wallet);

        System.out.println("DEBIT done: walletId=" + savedWallet.getId() +
                ", newBalance=" + savedWallet.getBalance() +
                ", availableBalance=" + savedWallet.getAvailableBalance());

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
        wallet.setBalance(wallet.getBalance() + walletHold.getAmount());

        walletHold.setStatus(WalletHoldStatus.RELEASED);
        walletRepository.save(wallet);
        walletHoldRepository.save(walletHold);

        HoldResponse holdResponse = new HoldResponse(walletHold.getHoldReference(), walletHold.getAmount(), walletHold.getStatus());
        return holdResponse;
    }


}
