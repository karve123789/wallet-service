package com.example.wallet.controller;

import com.example.wallet.dto.OperationType;
import com.example.wallet.dto.WalletOperationRequest;
import com.example.wallet.model.Wallet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletControllerTest {


    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:14-alpine");


    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testWalletLifecycle() {
        UUID walletId = UUID.randomUUID();

        // 1. Попытка получить несуществующий кошелек - ожидаем 404
        ResponseEntity<String> notFoundResponse = restTemplate.getForEntity("/api/v1/wallets/" + walletId, String.class);
        assertThat(notFoundResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 2. Делаем первый депозит
        WalletOperationRequest depositRequest = new WalletOperationRequest(walletId, OperationType.DEPOSIT, new BigDecimal("1000.50"));
        ResponseEntity<Wallet> depositResponse = restTemplate.postForEntity("/api/v1/wallet", depositRequest, Wallet.class);

        assertThat(depositResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(depositResponse.getBody()).isNotNull();
        assertThat(depositResponse.getBody().getId()).isEqualTo(walletId);
        // Сравниваем BigDecimal через compareTo
        assertThat(depositResponse.getBody().getBalance()).isEqualByComparingTo("1000.50");

        // 3. Проверяем баланс через GET-запрос
        ResponseEntity<Wallet> getResponse = restTemplate.getForEntity("/api/v1/wallets/" + walletId, Wallet.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getBalance()).isEqualByComparingTo("1000.50");

        // 4. Снимаем часть средств
        WalletOperationRequest withdrawRequest = new WalletOperationRequest(walletId, OperationType.WITHDRAW, new BigDecimal("500.25"));
        ResponseEntity<Wallet> withdrawResponse = restTemplate.postForEntity("/api/v1/wallet", withdrawRequest, Wallet.class);
        assertThat(withdrawResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(withdrawResponse.getBody().getBalance()).isEqualByComparingTo("500.25");

        // 5. Пытаемся снять больше, чем есть на счете - ожидаем 400
        WalletOperationRequest overdraftRequest = new WalletOperationRequest(walletId, OperationType.WITHDRAW, new BigDecimal("1000"));
        ResponseEntity<String> errorResponse = restTemplate.exchange(
                "/api/v1/wallet",
                HttpMethod.POST,
                new HttpEntity<>(overdraftRequest),
                String.class);

        assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorResponse.getBody()).contains("Insufficient funds");
    }
}
