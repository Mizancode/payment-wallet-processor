package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.repository;

import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<Wallet,Long> {
}
