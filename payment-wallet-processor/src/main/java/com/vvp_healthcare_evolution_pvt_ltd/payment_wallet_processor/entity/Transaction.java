package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "transaction_id",nullable = false)
    private UUID transactionId;
    @Column(name = "user_id",nullable = false)
    private UUID userId;
    @Column(nullable = false,precision = 19,scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;
    @Column(nullable = false,name = "created_at")
    private LocalDateTime createdAt=LocalDateTime.now();
}
