
package com.wallet.system.controller;

import com.wallet.system.dto.response.AdminWalletResponse;
import com.wallet.system.dto.response.TransactionResponse;
import com.wallet.system.security.CustomUserDetailsService;
import com.wallet.system.security.JwtService;
import com.wallet.system.service.AdminService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;


    @Test
    void getAllWallets_shouldReturnOk() throws Exception {

        when(adminService.getAllWallets(any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/admin/wallets"))
                .andExpect(status().isOk());

        verify(adminService).getAllWallets(any(Pageable.class));
    }


    @Test
    void getAllTransactions_shouldReturnOk() throws Exception {

        when(adminService.getAllTransactions(any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/admin/transactions"))
                .andExpect(status().isOk());

        verify(adminService).getAllTransactions(any(Pageable.class));
    }

}
