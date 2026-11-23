package com.wallet.system.repository;

import com.wallet.system.entity.Transaction;
import com.wallet.system.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

//    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
//
//    Page<Transaction> findByFromWalletUserIdOrToWalletUserId(
//            Long senderUserId, Long receiverUserId, Pageable pageable);

    Optional<Transaction> findByInitiatedByIdAndTypeAndIdempotencyKey(
            Long userId,
            TransactionType type,
            String idempotencyKey);

    Page<Transaction> findByFromWalletUserIdOrToWalletUserId(
            Long senderUserId,
            Long receiverUserId,
            Pageable pageable);


}