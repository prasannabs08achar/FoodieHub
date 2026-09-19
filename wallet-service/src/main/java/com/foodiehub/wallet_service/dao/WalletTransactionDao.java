package com.foodiehub.wallet_service.dao;

import com.foodiehub.wallet_service.model.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletTransactionDao extends JpaRepository<WalletTransaction, UUID> {
    Page<WalletTransaction> findByWalletId(
            UUID walletId,
            Pageable pageable
    );
}
