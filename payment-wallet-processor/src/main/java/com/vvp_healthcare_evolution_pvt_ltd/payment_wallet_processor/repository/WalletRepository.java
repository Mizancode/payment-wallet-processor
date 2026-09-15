package com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.repository;

import com.vvp_healthcare_evolution_pvt_ltd.payment_wallet_processor.entity.Wallet;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet,Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findByUserId(@NotNull(message = "userId is required") UUID userId);
}
