package com.wallet.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class WalletResponse {

    private Long walletId;
    private BigDecimal balance;
}