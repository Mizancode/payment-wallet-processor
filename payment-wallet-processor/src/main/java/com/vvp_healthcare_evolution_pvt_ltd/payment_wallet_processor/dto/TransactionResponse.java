package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.dto;

import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.TransactionStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransactionResponse {
    private UUID transactionId;
    private TransactionStatus status;
    private String message;
    private BigDecimal remainingBalance;
}
