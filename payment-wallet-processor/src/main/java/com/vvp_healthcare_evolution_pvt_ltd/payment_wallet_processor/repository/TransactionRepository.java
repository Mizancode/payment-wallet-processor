package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.repository;

import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Long> {
}
