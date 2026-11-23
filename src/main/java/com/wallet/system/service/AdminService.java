package com.wallet.system.service;

import com.wallet.system.dto.response.AdminWalletResponse;
import com.wallet.system.dto.response.TransactionResponse;
import com.wallet.system.entity.Transaction;
import com.wallet.system.repository.TransactionRepository;
import com.wallet.system.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;


    public AdminService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }


    @Transactional(readOnly = true)
    public Page<AdminWalletResponse> getAllWallets(Pageable pageable) {

        return walletRepository.findAll(pageable)
                .map(wallet -> new AdminWalletResponse(
                        wallet.getId(),
                        wallet.getUser().getId(),
                        wallet.getUser().getEmail(),
                        wallet.getBalance()
                ));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactions(Pageable pageable) {

        return transactionRepository.findAll(pageable)
                .map(this::convertToResponse);
    }

    private TransactionResponse convertToResponse(Transaction transaction) {

        return new TransactionResponse(
                transaction.getId(),

                transaction.getFromWallet() != null
                        ? transaction.getFromWallet().getId()
                        : null,

                transaction.getToWallet() != null
                        ? transaction.getToWallet().getId()
                        : null,

                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }
}
