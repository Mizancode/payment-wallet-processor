package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.dto;

import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransactionRequest {

    @NotNull(message = "transactionId is required")
    private UUID transactionId;
    @NotNull(message = "userId is required")
    private UUID userId;
    @NotNull(message = "amount is required")
    @DecimalMin(
            value = "0.01",
            message = "amount must be greater than zero"
    )
    private BigDecimal amount;
    @NotNull(message = "type is required")
    private TransactionType type;
}
