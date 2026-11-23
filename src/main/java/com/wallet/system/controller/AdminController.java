package com.wallet.system.controller;

import com.wallet.system.dto.response.AdminWalletResponse;
import com.wallet.system.dto.response.TransactionResponse;
import com.wallet.system.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/wallets")
    public Page<AdminWalletResponse> getAllWallets(
            @PageableDefault(
                    size = 10,
                    sort = "id",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return adminService.getAllWallets(pageable);
    }

    @GetMapping("/transactions")
    public Page<TransactionResponse> getAllTransactions(
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return adminService.getAllTransactions(pageable);
    }
}
