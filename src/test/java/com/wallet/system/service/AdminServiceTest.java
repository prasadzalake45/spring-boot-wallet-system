package com.wallet.system.service;

import com.wallet.system.dto.response.AdminWalletResponse;
import com.wallet.system.dto.response.TransactionResponse;
import com.wallet.system.entity.Transaction;
import com.wallet.system.entity.User;
import com.wallet.system.entity.Wallet;
import com.wallet.system.enums.TransactionStatus;
import com.wallet.system.enums.TransactionType;
import com.wallet.system.repository.TransactionRepository;
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
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AdminService adminService;

    private User user;
    private Wallet wallet;
    private
    Transaction transaction;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);
        user.setEmail("admin-test@gmail.com");

        wallet = new Wallet();
        wallet.setId(101L);
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("1500.00"));

        transaction = new Transaction();
        transaction.setId(501L);
        transaction.setFromWallet(null);
        transaction.setToWallet(wallet);
        transaction.setAmount(new BigDecimal("500.00"));
        transaction.setType(TransactionType.ADD);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setCreatedAt(LocalDateTime.now());
    }

    // ---------------- GET ALL WALLETS ----------------

    @Test
    void getAllWallets_shouldReturnAllWallets() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Wallet> walletPage =
                new PageImpl<>(List.of(wallet));

        when(walletRepository.findAll(pageable))
                .thenReturn(walletPage);

        Page<AdminWalletResponse> result =
                adminService.getAllWallets(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        AdminWalletResponse response =
                result.getContent().get(0);

        assertEquals(101L, response.getWalletId());
        assertEquals(1L, response.getUserId());
        assertEquals("admin-test@gmail.com", response.getEmail());
        assertEquals(
                0,
                new BigDecimal("1500.00")
                        .compareTo(response.getBalance())
        );

        verify(walletRepository).findAll(pageable);
    }

    @Test
    void getAllWallets_shouldReturnEmptyPage() {

        Pageable pageable = PageRequest.of(0, 10);

        when(walletRepository.findAll(pageable))
                .thenReturn(Page.empty(pageable));

        Page<AdminWalletResponse> result =
                adminService.getAllWallets(pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(walletRepository).findAll(pageable);
    }

    @Test
    void getAllWallets_shouldPreservePaginationDetails() {

        Pageable pageable = PageRequest.of(1, 5);

        when(walletRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(
                        List.of(wallet),
                        pageable,
                        6
                ));

        Page<AdminWalletResponse> result =
                adminService.getAllWallets(pageable);

        assertEquals(1, result.getNumber());
        assertEquals(5, result.getSize());
        assertEquals(6, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
    }


    @Test
    void getAllTransactions_shouldReturnAllTransactions() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Transaction> transactionPage =
                new PageImpl<>(List.of(transaction));

        when(transactionRepository.findAll(pageable))
                .thenReturn(transactionPage);

        Page<TransactionResponse> result =
                adminService.getAllTransactions(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        TransactionResponse response =
                result.getContent().get(0);

        assertEquals(501L, response.getTransactionId());
        assertNull(response.getFromWalletId());
        assertEquals(101L, response.getToWalletId());

        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(response.getAmount())
        );

        assertEquals(TransactionType.ADD, response.getType());
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());

        verify(transactionRepository).findAll(pageable);
    }

    @Test
    void getAllTransactions_shouldReturnEmptyPage() {

        Pageable pageable = PageRequest.of(0, 10);

        when(transactionRepository.findAll(pageable))
                .thenReturn(Page.empty(pageable));

        Page<TransactionResponse> result =
                adminService.getAllTransactions(pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(transactionRepository).findAll(pageable);
    }

    @Test
    void getAllTransactions_shouldMapTransferTransaction() {

        Pageable pageable = PageRequest.of(0, 10);

        User receiverUser = new User();
        receiverUser.setId(2L);
        receiverUser.setEmail("receiver@gmail.com");

        Wallet receiverWallet = new Wallet();
        receiverWallet.setId(102L);
        receiverWallet.setUser(receiverUser);
        receiverWallet.setBalance(new BigDecimal("1000.00"));

        Transaction transfer = new Transaction();
        transfer.setId(502L);
        transfer.setFromWallet(wallet);
        transfer.setToWallet(receiverWallet);
        transfer.setAmount(new BigDecimal("300.00"));
        transfer.setType(TransactionType.TRANSFER);
        transfer.setStatus(TransactionStatus.SUCCESS);
        transfer.setCreatedAt(LocalDateTime.now());

        when(transactionRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(transfer)));

        Page<TransactionResponse> result =
                adminService.getAllTransactions(pageable);

        TransactionResponse response =
                result.getContent().get(0);

        assertEquals(502L, response.getTransactionId());
        assertEquals(101L, response.getFromWalletId());
        assertEquals(102L, response.getToWalletId());
        assertEquals(TransactionType.TRANSFER, response.getType());
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
    }

    @Test
    void getAllTransactions_shouldPreservePaginationDetails() {

        Pageable pageable = PageRequest.of(1, 5);

        when(transactionRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(
                        List.of(transaction),
                        pageable,
                        6
                ));

        Page<TransactionResponse> result =
                adminService.getAllTransactions(pageable);

        assertEquals(1, result.getNumber());
        assertEquals(5, result.getSize());
        assertEquals(6, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
    }
}
