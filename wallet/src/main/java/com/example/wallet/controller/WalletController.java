package com.example.wallet.controller;

import com.example.wallet.dto.WalletOperationRequest;
import com.example.wallet.model.Wallet;
import com.example.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WalletController {


    private final WalletService walletService;

    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<Wallet> getWallet(@PathVariable UUID walletId) {
        return ResponseEntity.ok(walletService.getWalletBalance(walletId));
    }

    @PostMapping("/wallet")
    public ResponseEntity<Wallet> updateWallet(@RequestBody WalletOperationRequest request) {

        return ResponseEntity.ok(walletService.performOperation(request));
    }
}
