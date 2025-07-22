package com.example.wallet.service;

import com.example.wallet.dto.OperationType;
import com.example.wallet.dto.WalletOperationRequest;
import com.example.wallet.exception.InsufficientFundsException;
import com.example.wallet.exception.WalletNotFoundException;
import com.example.wallet.model.Wallet;
import com.example.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public Wallet getWalletBalance(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found: " + walletId));
    }

    @Transactional
    public Wallet performOperation(WalletOperationRequest request) {

        if (request.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Operation amount cannot be negative.");
        }


        Wallet wallet;
        Optional<Wallet> walletOpt = walletRepository.findById(request.walletId());

        if (walletOpt.isPresent()) {

            wallet = walletRepository.findByIdForUpdate(request.walletId()).get();
        } else {

            if (request.operationType() == OperationType.WITHDRAW) {
                throw new WalletNotFoundException("Cannot withdraw from a non-existent wallet: " + request.walletId());
            }

            wallet = new Wallet();
            wallet.setId(request.walletId());
            wallet.setBalance(BigDecimal.ZERO);
        }


        if (request.operationType() == OperationType.DEPOSIT) {
            wallet.setBalance(wallet.getBalance().add(request.amount()));
        } else {
            if (wallet.getBalance().compareTo(request.amount()) < 0) {
                throw new InsufficientFundsException("Insufficient funds for wallet: " + request.walletId());
            }
            wallet.setBalance(wallet.getBalance().subtract(request.amount()));
        }

        return walletRepository.save(wallet);
    }
}