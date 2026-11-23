package com.wallet.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AdminWalletResponse {

    Long walletId;
    Long userId;
    String email;
    BigDecimal balance;
}
