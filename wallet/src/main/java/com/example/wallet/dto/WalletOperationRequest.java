package com.example.wallet.dto;
import java.math.BigDecimal;
import java.util.UUID;
public record WalletOperationRequest(
        UUID walletId,
        OperationType operationType,
        BigDecimal amount
) {
}
