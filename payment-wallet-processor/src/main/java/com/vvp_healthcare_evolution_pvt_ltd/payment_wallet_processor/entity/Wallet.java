package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "wallet")
@Data
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false,name = "user_id")
    private UUID userId;
    @Column(nullable = false,precision = 19,scale = 2)
    private BigDecimal balance;
}
