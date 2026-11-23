package com.wallet.system.service;


import com.wallet.system.dto.response.TransactionResponse;
import com.wallet.system.dto.response.WalletResponse;
import com.wallet.system.entity.User;
import com.wallet.system.entity.Wallet;
import com.wallet.system.enums.TransactionStatus;
import com.wallet.system.enums.TransactionType;
import com.wallet.system.exception.InsufficientBalanceException;
import com.wallet.system.exception.ResourceNotFoundException;
import com.wallet.system.repository.TransactionRepository;
import com.wallet.system.repository.UserRepository;
import com.wallet.system.repository.WalletRepository;
import com.wallet.system.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;

@Service
public class WalletService {


    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository) {

        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }


    // 1. View logged-in user's wallet
    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(String email) {

        User user = findUser(email);

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Wallet not found"));

        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance()
        );
    }



    // 2. Add money to wallet
    @Transactional
    public TransactionResponse addMoney(
            String email,
            BigDecimal amount,
            String idempotencyKey) {

        validateAmount(amount);
        validateIdempotencyKey(idempotencyKey);

        User user = findUser(email);

        // Check whether this operation was already completed.
        var existing = transactionRepository
                .findByInitiatedByIdAndTypeAndIdempotencyKey(
                        user.getId(),
                        TransactionType.ADD,
                        idempotencyKey);

        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        Wallet userWallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Wallet not found"));

        // Lock the wallet before updating its balance.
        Wallet wallet = walletRepository
                .findByIdForUpdate(userWallet.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Wallet not found"));

        // Check again after acquiring the lock.
        existing = transactionRepository
                .findByInitiatedByIdAndTypeAndIdempotencyKey(
                        user.getId(),
                        TransactionType.ADD,
                        idempotencyKey);

        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        wallet.setBalance(wallet.getBalance().add(amount));

        Transaction transaction = new Transaction();
        transaction.setInitiatedBy(user);
        transaction.setFromWallet(null);
        transaction.setToWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.ADD);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setIdempotencyKey(idempotencyKey);

        walletRepository.save(wallet);

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        return toResponse(savedTransaction);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero");
        }
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key header is required");
        }
    }

    private TransactionResponse toResponse(Transaction transaction) {

        Long fromWalletId = transaction.getFromWallet() == null
                ? null
                : transaction.getFromWallet().getId();

        Long toWalletId = transaction.getToWallet() == null
                ? null
                : transaction.getToWallet().getId();

        return new TransactionResponse(
                transaction.getId(),
                fromWalletId,
                toWalletId,
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }

    // 3. Transfer money to another user's wallet
    @Transactional
    public TransactionResponse transferMoney(
            String email,
            Long receiverWalletId,
            BigDecimal amount,
            String idempotencyKey) {

        validateAmount(amount);
        validateIdempotencyKey(idempotencyKey);

        User sender = findUser(email);

        // Check for an already completed transfer.
        var existing = transactionRepository
                .findByInitiatedByIdAndTypeAndIdempotencyKey(
                        sender.getId(),
                        TransactionType.TRANSFER,
                        idempotencyKey);

        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        Wallet senderWallet = walletRepository
                .findByUserId(sender.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Sender wallet not found"));

        Wallet receiverWallet = walletRepository
                .findById(receiverWalletId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Receiver wallet not found"));

        if (senderWallet.getId().equals(receiverWallet.getId())) {
            throw new IllegalArgumentException(
                    "Cannot transfer money to your own wallet");
        }

        // Lock both wallets in ascending ID order.
        Long firstId = Math.min(senderWallet.getId(), receiverWallet.getId());
        Long secondId = Math.max(senderWallet.getId(), receiverWallet.getId());

        Wallet firstWallet = walletRepository.findByIdForUpdate(firstId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Wallet not found"));

        Wallet secondWallet = walletRepository.findByIdForUpdate(secondId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Wallet not found"));

        // Identify the locked sender and receiver.
        if (senderWallet.getId().equals(firstId)) {
            senderWallet = firstWallet;
            receiverWallet = secondWallet;
        } else {
            senderWallet = secondWallet;
            receiverWallet = firstWallet;
        }

        // Recheck idempotency after acquiring locks.
        existing = transactionRepository
                .findByInitiatedByIdAndTypeAndIdempotencyKey(
                        sender.getId(),
                        TransactionType.TRANSFER,
                        idempotencyKey);

        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        // Check available balance.
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient wallet balance");
        }

        // Deduct from sender.
        senderWallet.setBalance(
                senderWallet.getBalance().subtract(amount));

        // Credit receiver.
        receiverWallet.setBalance(
                receiverWallet.getBalance().add(amount));

        // Create transaction record.
        Transaction transaction = new Transaction();
        transaction.setInitiatedBy(sender);
        transaction.setFromWallet(senderWallet);
        transaction.setToWallet(receiverWallet);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setIdempotencyKey(idempotencyKey);

        // All three changes are committed together.
        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        return toResponse(savedTransaction);
    }

    // 4. View logged-in user's transaction history
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getMyTransactions(
            String email,
            Pageable pageable) {
        User user = findUser(email);
        return transactionRepository
                .findByFromWalletUserIdOrToWalletUserId(
                        user.getId(),
                        user.getId(),
                        pageable)
                .map(this::toResponse);
    }

}
