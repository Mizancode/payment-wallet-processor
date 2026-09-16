package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.paymentprocessor;

import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.Wallet;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.repository.TransactionRepository;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.repository.WalletRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userId = UUID.randomUUID();
    }

    private Wallet createWallet(UUID userId, BigDecimal balance) {
        Wallet wallet=new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(balance);
        return walletRepository.save(wallet);
    }

    private String buildRequest(UUID transactionId, UUID userId, BigDecimal amount) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("transactionId", transactionId);
        request.put("userId", userId);
        request.put("amount", amount);
        request.put("type", "DEBIT");
        return objectMapper.writeValueAsString(request);
    }

    // -------------------------------------------------------------------------------------------------
    // Test-1 Happy Path Test
    // -------------------------------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName(
            "Processes a single valid debit transaction successfully."
    )
    void processesSingleValidDebitSuccessfully() throws Exception {

        System.out.println();
        System.out.println("==============================================");
        System.out.println("TEST 1: Happy Path");
        System.out.println("Processes a single valid debit transaction successfully.");
        System.out.println("==============================================");

        createWallet(userId, new BigDecimal("500.00"));

        UUID transactionId = UUID.randomUUID();

        String request = buildRequest(transactionId, userId, new BigDecimal("250.00"));

        mockMvc.perform(
                        post("/api/v1/transactions/process")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk());

        Wallet wallet = walletRepository.findByUserIdWithoutLock(userId).orElseThrow();

        long transactionCount = transactionRepository.countByTransactionId(transactionId);

        assertThat(wallet.getBalance())
                .isEqualByComparingTo("250.00");

        assertThat(transactionCount)
                .isEqualTo(1);

        System.out.println("Initial balance: ₹500.00");
        System.out.println("Debit amount: ₹250.00");
        System.out.println("Final balance: ₹"
                + wallet.getBalance());
        System.out.println("Transactions persisted: "
                + transactionCount);
        System.out.println("RESULT: PASS");
        System.out.println("==============================================");
    }


    // -------------------------------------------------------------------------------------------------
    // Test-2 Idempotency Test
    // -------------------------------------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName(
            "Sends 3 identical transactionIDs simultaneously. Ensures the balance is only deducted once."
    )
    void processesDuplicateTransactionsOnlyOnce()
            throws Exception {

        System.out.println();
        System.out.println("============================================================");
        System.out.println("TEST 2: Idempotency");
        System.out.println(
                "Sends 3 identical transactionIDs simultaneously. "
                        + "Ensures the balance is only deducted once."
        );
        System.out.println("============================================================");

        createWallet(
                userId,
                new BigDecimal("500.00")
        );

        UUID transactionId = UUID.randomUUID();

        String request = buildRequest(
                transactionId,
                userId,
                new BigDecimal("250.00")
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(3);

        CountDownLatch ready =
                new CountDownLatch(3);

        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<Integer>> futures =
                new ArrayList<>();

        for (int i = 0; i < 3; i++) {

            futures.add(
                    executor.submit(() -> {

                        ready.countDown();

                        start.await();

                        return mockMvc.perform(
                                        post(
                                                "/api/v1/transactions/process"
                                        )
                                                .contentType(
                                                        MediaType.APPLICATION_JSON
                                                )
                                                .content(request)
                                )
                                .andReturn()
                                .getResponse()
                                .getStatus();
                    })
            );
        }

        ready.await();

        start.countDown();

        List<Integer> statuses =
                new ArrayList<>();

        for (Future<Integer> future : futures) {
            statuses.add(future.get());
        }

        executor.shutdown();

        long successfulRequests =
                statuses.stream()
                        .filter(status -> status == 200)
                        .count();

        long conflictRequests =
                statuses.stream()
                        .filter(status -> status == 409)
                        .count();

        Wallet wallet =
                walletRepository.findByUserIdWithoutLock(userId)
                        .orElseThrow();

        long transactionCount =
                transactionRepository.countByTransactionId(
                        transactionId
                );

        System.out.println("Requests sent: 3");
        System.out.println("HTTP statuses: " + statuses);
        System.out.println("Successful: " + successfulRequests);
        System.out.println("Conflicts: " + conflictRequests);
        System.out.println("Final balance: ₹" + wallet.getBalance());
        System.out.println("Persisted transactions: " + transactionCount);

        assertThat(successfulRequests)
                .isEqualTo(1);

        assertThat(conflictRequests)
                .isEqualTo(2);

        assertThat(wallet.getBalance())
                .isEqualByComparingTo("250.00");

        assertThat(transactionCount)
                .isEqualTo(1);

        System.out.println("RESULT: PASS");
        System.out.println("============================================================");;
    }

    // -------------------------------------------------------------------------------------------------
    // Test-3 Race condition test
    // -------------------------------------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName(
            "Sends 10 concurrent debit requests of ₹100 for a wallet with a ₹500 balance. Ensures the final balance is exactly ₹0 and 5 requests fail with insufficient funds."
    )
    void preventsNegativeBalanceUnderConcurrentDebits()
            throws Exception {

        System.out.println();
        System.out.println("================================================================");
        System.out.println("TEST 3: Race Condition");
        System.out.println(
                "Sends 10 concurrent debit requests of ₹100 "
                        + "for a wallet with a ₹500 balance."
        );
        System.out.println("================================================================");

        createWallet(
                userId,
                new BigDecimal("500.00")
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(10);

        CountDownLatch ready =
                new CountDownLatch(10);

        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<Integer>> futures =
                new ArrayList<>();

        for (int i = 0; i < 10; i++) {

            UUID transactionId =
                    UUID.randomUUID();

            String request = buildRequest(
                    transactionId,
                    userId,
                    new BigDecimal("100.00")
            );

            futures.add(
                    executor.submit(() -> {

                        ready.countDown();

                        start.await();

                        return mockMvc.perform(
                                        post(
                                                "/api/v1/transactions/process"
                                        )
                                                .contentType(
                                                        MediaType.APPLICATION_JSON
                                                )
                                                .content(request)
                                )
                                .andReturn()
                                .getResponse()
                                .getStatus();
                    })
            );
        }

        ready.await();

        start.countDown();

        List<Integer> statuses =
                new ArrayList<>();

        for (Future<Integer> future : futures) {
            statuses.add(future.get());
        }

        executor.shutdown();

        long successfulRequests =
                statuses.stream()
                        .filter(status -> status == 200)
                        .count();

        long insufficientFundsRequests =
                statuses.stream()
                        .filter(status -> status == 409)
                        .count();

        Wallet wallet =
                walletRepository.findByUserIdWithoutLock(userId)
                        .orElseThrow();

        assertThat(successfulRequests)
                .isEqualTo(5);

        assertThat(insufficientFundsRequests)
                .isEqualTo(5);

        assertThat(wallet.getBalance())
                .isEqualByComparingTo("0.00");

        System.out.println("Initial balance: ₹500.00");
        System.out.println("Concurrent requests: 10");
        System.out.println("Debit per request: ₹100.00");
        System.out.println("Successful requests: "
                + successfulRequests);
        System.out.println("Insufficient funds: "
                + insufficientFundsRequests);
        System.out.println("Final balance: ₹"
                + wallet.getBalance());
        System.out.println("RESULT: PASS");
        System.out.println("================================================================");
    }

}
