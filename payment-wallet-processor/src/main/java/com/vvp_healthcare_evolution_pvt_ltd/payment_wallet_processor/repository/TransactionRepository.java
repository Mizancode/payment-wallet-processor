package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.repository;

import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.Transaction;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Long> {

    Optional<Transaction> findByTransactionId(@NotNull(message = "transactionId is required") UUID transactionId);

    long countByTransactionId(UUID transactionId);
}
