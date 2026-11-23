package com.wallet.system.dto.response;

import com.wallet.system.enums.TransactionStatus;
import com.wallet.system.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TransactionResponse {

    private Long transactionId;
    private Long fromWalletId;
    private Long toWalletId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private LocalDateTime createdAt;
}
