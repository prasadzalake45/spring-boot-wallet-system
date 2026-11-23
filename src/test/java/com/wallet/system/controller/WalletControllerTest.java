
package com.wallet.system.controller;

import com.wallet.system.security.CustomUserDetailsService;
import com.wallet.system.security.JwtService;
import com.wallet.system.service.WalletService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

   @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;


    @BeforeEach
    void setUp() {

        UserDetails userDetails = User.withUsername("test@gmail.com")
                .password("password")
                .roles("USER")
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }


    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyWallet_shouldReturnOk() throws Exception {

        when(walletService.getMyWallet("test@gmail.com"))
                .thenReturn(null);

        mockMvc.perform(get("/wallet"))
                .andExpect(status().isOk());

        verify(walletService).getMyWallet("test@gmail.com");
    }


    @Test
    void addMoney_shouldReturnOk() throws Exception {

        when(walletService.addMoney(
                eq("test@gmail.com"),
                any(),
                eq("add-money-key")
        )).thenReturn(null);

        mockMvc.perform(post("/wallet/add")
                        .header("Idempotency-Key", "add-money-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 100.00
                                }
                                """))
                .andExpect(status().isOk());

        verify(walletService).addMoney(
                eq("test@gmail.com"),
                any(),
                eq("add-money-key")
        );
    }


    @Test
    void transferMoney_shouldReturnOk() throws Exception {

        when(walletService.transferMoney(
                eq("test@gmail.com"),
                any(),
                any(),
                eq("transfer-money-key")
        )).thenReturn(null);

        mockMvc.perform(post("/wallet/transfer")
                        .header("Idempotency-Key", "transfer-money-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "receiverWalletId": 2,
                                    "amount": 50.00
                                }
                                """))
                .andExpect(status().isOk());

        verify(walletService).transferMoney(
                eq("test@gmail.com"),
                any(),
                any(),
                eq("transfer-money-key")
        );
    }


    @Test
    void getMyTransactions_shouldReturnOk() throws Exception {

        when(walletService.getMyTransactions(
                eq("test@gmail.com"),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        mockMvc.perform(get("/wallet/transactions"))
                .andExpect(status().isOk());

        verify(walletService).getMyTransactions(
                eq("test@gmail.com"),
                any(Pageable.class)
        );
    }
}
