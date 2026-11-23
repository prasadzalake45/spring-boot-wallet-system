package com.wallet.system.service;

import com.wallet.system.dto.response.TransactionResponse;
import com.wallet.system.dto.response.WalletResponse;
import com.wallet.system.entity.Transaction;
import com.wallet.system.entity.User;
import com.wallet.system.entity.Wallet;
import com.wallet.system.enums.TransactionStatus;
import com.wallet.system.enums.TransactionType;
import com.wallet.system.exception.InsufficientBalanceException;
import com.wallet.system.exception.ResourceNotFoundException;
import com.wallet.system.repository.TransactionRepository;
import com.wallet.system.repository.UserRepository;
import com.wallet.system.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletService walletService;

    private User user;
    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@gmail.com");

        senderWallet = new Wallet();
        senderWallet.setId(101L);
        senderWallet.setUser(user);
        senderWallet.setBalance(new BigDecimal("1000.00"));

        User receiver = new User();
        receiver.setId(2L);
        receiver.setEmail("receiver@gmail.com");

        receiverWallet = new Wallet();
        receiverWallet.setId(102L);
        receiverWallet.setUser(receiver);
        receiverWallet.setBalance(new BigDecimal("500.00"));
    }


    @Test
    void getMyWallet_shouldReturnWalletDetails() {

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(walletRepository.findByUserId(1L))
                .thenReturn(Optional.of(senderWallet));

        WalletResponse response =
                walletService.getMyWallet(user.getEmail());

        assertEquals(101L, response.getWalletId());
        assertEquals(new BigDecimal("1000.00"), response.getBalance());

        verify(userRepository).findByEmail(user.getEmail());
        verify(walletRepository).findByUserId(1L);
    }

    @Test
    void getMyWallet_shouldThrowWhenUserNotFound() {

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> walletService.getMyWallet(user.getEmail()));

        verify(walletRepository, never()).findByUserId(anyLong());
    }

    @Test
    void getMyWallet_shouldThrowWhenWalletNotFound() {

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(walletRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> walletService.getMyWallet(user.getEmail()));
    }


    @Test
    void addMoney_shouldAddAmountSuccessfully() {

        BigDecimal amount = new BigDecimal("200.00");
        String key = "ADD-001";

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(transactionRepository
                .findByInitiatedByIdAndTypeAndIdempotencyKey(
                        1L, TransactionType.ADD, key))
                .thenReturn(Optional.empty());

        when(walletRepository.findByUserId(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByIdForUpdate(101L))
                .thenReturn(Optional.of(senderWallet));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction transaction = invocation.getArgument(0);
                    transaction.setId(1L);
                    transaction.setCreatedAt(LocalDateTime.now());
                    return transaction;
                });

        TransactionResponse response = walletService.addMoney(
                user.getEmail(), amount, key);

        assertEquals(new BigDecimal("1200.00"), senderWallet.getBalance());
        assertEquals(101L, response.getToWalletId());
        assertEquals(amount, response.getAmount());
        assertEquals(TransactionType.ADD, response.getType());
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());

        verify(walletRepository).save(senderWallet);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void addMoney_shouldThrowForZeroAmount() {

        assertThrows(IllegalArgumentException.class,
                () -> walletService.addMoney(
                        user.getEmail(),
                        BigDecimal.ZERO,
                        "ADD-002"));

        verifyNoInteractions(userRepository, walletRepository,
                transactionRepository);
    }

    @Test
    void addMoney_shouldThrowForNullAmount() {

        assertThrows(IllegalArgumentException.class,
                () -> walletService.addMoney(
                        user.getEmail(), null, "ADD-003"));
    }

    @Test
    void addMoney_shouldThrowForMissingIdempotencyKey() {

        assertThrows(IllegalArgumentException.class,
                () -> walletService.addMoney(
                        user.getEmail(),
                        new BigDecimal("100.00"),
                        " "));
    }

    @Test
    void addMoney_shouldThrowWhenUserNotFound() {

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> walletService.addMoney(
                        user.getEmail(),
                        new BigDecimal("100.00"),
                        "ADD-004"));
    }

    @Test
    void addMoney_shouldThrowWhenWalletNotFound() {

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(transactionRepository
                .findByInitiatedByIdAndTypeAndIdempotencyKey(
                        1L, TransactionType.ADD, "ADD-005"))
                .thenReturn(Optional.empty());

        when(walletRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> walletService.addMoney(
                        user.getEmail(),
                        new BigDecimal("100.00"),
                        "ADD-005"));
    }

    @Test
    void addMoney_shouldReturnExistingTransactionForDuplicateKey() {

        Transaction existing = createTransaction(
                10L, senderWallet, null,
                new BigDecimal("100.00"),
                TransactionType.ADD, "ADD-DUP");

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(transactionRepository
                .findByInitiatedByIdAndTypeAndIdempotencyKey(
                        1L, TransactionType.ADD, "ADD-DUP"))
                .thenReturn(Optional.of(existing));

        TransactionResponse response = walletService.addMoney(
                user.getEmail(),
                new BigDecimal("100.00"),
                "ADD-DUP");

        assertEquals(10L, response.getTransactionId());

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferMoney_shouldTransferSuccessfully() {

        BigDecimal amount = new BigDecimal("200.00");
        String key = "TRANSFER-001";

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(transactionRepository
                .findByInitiatedByIdAndTypeAndIdempotencyKey(
                        1L, TransactionType.TRANSFER, key))
                .thenReturn(Optional.empty());

        when(walletRepository.findByUserId(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findById(102L))
                .thenReturn(Optional.of(receiverWallet));

        when(walletRepository.findByIdForUpdate(101L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByIdForUpdate(102L))
                .thenReturn(Optional.of(receiverWallet));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction transaction = invocation.getArgument(0);
                    transaction.setId(20L);
                    transaction.setCreatedAt(LocalDateTime.now());
                    return transaction;
                });

        TransactionResponse response = walletService.transferMoney(
                user.getEmail(), 102L, amount, key);

        assertEquals(new BigDecimal("800.00"), senderWallet.getBalance());
        assertEquals(new BigDecimal("700.00"), receiverWallet.getBalance());

        assertEquals(20L, response.getTransactionId());
        assertEquals(101L, response.getFromWalletId());
        assertEquals(102L, response.getToWalletId());
        assertEquals(TransactionType.TRANSFER, response.getType());
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());

        verify(walletRepository).save(senderWallet);
        verify(walletRepository).save(receiverWallet);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void transferMoney_shouldThrowForInsufficientBalance() {

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(transactionRepository
                .findByInitiatedByIdAndTypeAndIdempotencyKey(
                        1L, TransactionType.TRANSFER, "TRANSFER-002"))
                .thenReturn(Optional.empty());

        when(walletRepository.findByUserId(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findById(102L))
                .thenReturn(Optional.of(receiverWallet));

        when(walletRepository.findByIdForUpdate(101L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findByIdForUpdate(102L))
                .thenReturn(Optional.of(receiverWallet));

        assertThrows(InsufficientBalanceException.class,
                () -> walletService.transferMoney(
                        user.getEmail(),
                        102L,
                        new BigDecimal("2000.00"),
                        "TRANSFER-002"));

        assertEquals(new BigDecimal("1000.00"), senderWallet.getBalance());
        assertEquals(new BigDecimal("500.00"), receiverWallet.getBalance());

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferMoney_shouldThrowForSelfTransfer() {

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(transactionRepository
                .findByInitiatedByIdAndTypeAndIdempotencyKey(
                        1L, TransactionType.TRANSFER, "TRANSFER-003"))
                .thenReturn(Optional.empty());

        when(walletRepository.findByUserId(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findById(101L))
                .thenReturn(Optional.of(senderWallet));

        assertThrows(IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        user.getEmail(),
                        101L,
                        new BigDecimal("100.00"),
                        "TRANSFER-003"));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void transferMoney_shouldThrowWhenReceiverWalletNotFound() {

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(transactionRepository
                .findByInitiatedByIdAndTypeAndIdempotencyKey(
                        1L, TransactionType.TRANSFER, "TRANSFER-004"))
                .thenReturn(Optional.empty());

        when(walletRepository.findByUserId(1L))
                .thenReturn(Optional.of(senderWallet));

        when(walletRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> walletService.transferMoney(
                        user.getEmail(),
                        999L,
                        new BigDecimal("100.00"),
                        "TRANSFER-004"));
    }

    @Test
    void transferMoney_shouldThrowForInvalidAmount() {

        assertThrows(IllegalArgumentException.class,
                () -> walletService.transferMoney(
                        user.getEmail(),
                        102L,
                        BigDecimal.ZERO,
                        "TRANSFER-005"));
    }

    @Test
    void transferMoney_shouldReturnExistingTransactionForDuplicateKey() {

        Transaction existing = createTransaction(
                30L, senderWallet, receiverWallet,
                new BigDecimal("100.00"),
                TransactionType.TRANSFER, "TRANSFER-DUP");

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(transactionRepository
                .findByInitiatedByIdAndTypeAndIdempotencyKey(
                        1L, TransactionType.TRANSFER, "TRANSFER-DUP"))
                .thenReturn(Optional.of(existing));

        TransactionResponse response = walletService.transferMoney(
                user.getEmail(),
                102L,
                new BigDecimal("100.00"),
                "TRANSFER-DUP");

        assertEquals(30L, response.getTransactionId());

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }


    @Test
    void getMyTransactions_shouldReturnTransactionHistory() {

        Transaction transaction = createTransaction(
                40L, senderWallet, receiverWallet,
                new BigDecimal("100.00"),
                TransactionType.TRANSFER, "HISTORY-001");

        PageRequest pageable = PageRequest.of(0, 10);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(transactionRepository
                .findByFromWalletUserIdOrToWalletUserId(
                        1L, 1L, pageable))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        Page<TransactionResponse> result =
                walletService.getMyTransactions(user.getEmail(), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(40L, result.getContent().get(0).getTransactionId());
        assertEquals(TransactionType.TRANSFER,
                result.getContent().get(0).getType());
    }

    @Test
    void getMyTransactions_shouldReturnEmptyPage() {

        PageRequest pageable = PageRequest.of(0, 10);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(transactionRepository
                .findByFromWalletUserIdOrToWalletUserId(
                        1L, 1L, pageable))
                .thenReturn(Page.empty());

        Page<TransactionResponse> result =
                walletService.getMyTransactions(user.getEmail(), pageable);

        assertTrue(result.isEmpty());
    }

    @Test
    void getMyTransactions_shouldThrowWhenUserNotFound() {

        PageRequest pageable = PageRequest.of(0, 10);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> walletService.getMyTransactions(
                        user.getEmail(), pageable));
    }


    private Transaction createTransaction(
            Long id,
            Wallet fromWallet,
            Wallet toWallet,
            BigDecimal amount,
            TransactionType type,
            String key) {

        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setInitiatedBy(user);
        transaction.setFromWallet(fromWallet);
        transaction.setToWallet(toWallet);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setIdempotencyKey(key);
        transaction.setCreatedAt(LocalDateTime.now());

        return transaction;
    }

}