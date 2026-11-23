package com.wallet.system.controller;

import com.wallet.system.dto.request.AddMoneyRequest;
import com.wallet.system.dto.request.TransferRequest;
import com.wallet.system.dto.response.TransactionResponse;
import com.wallet.system.dto.response.WalletResponse;
import com.wallet.system.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallet")
public class WalletController {


    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<WalletResponse> getMyWallet(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                walletService.getMyWallet(userDetails.getUsername())
        );
    }

    @PostMapping("/add")
    public ResponseEntity<TransactionResponse> addMoney(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AddMoneyRequest request) {

        TransactionResponse response = walletService.addMoney(
                userDetails.getUsername(),
                request.getAmount(),
                idempotencyKey
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transferMoney(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        TransactionResponse response = walletService.transferMoney(
                userDetails.getUsername(),
                request.getReceiverWalletId(),
                request.getAmount(),
                idempotencyKey
        );

        return ResponseEntity.ok(response);
    }


    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(
                walletService.getMyTransactions(
                        userDetails.getUsername(),
                        pageable
                )
        );
    }
}

