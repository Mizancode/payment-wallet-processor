package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.paymentprocessor;

import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.Wallet;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.repository.TransactionRepository;
import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
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

        mockMvc.perform(
                        post("/api/v1/transactions/process")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk());

        Wallet wallet =
                walletRepository.findByUserId(userId)
                        .orElseThrow();

        long transactionCount =
                transactionRepository.countByTransactionId(
                        transactionId
                );

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


}
