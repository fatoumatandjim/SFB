package com.backend.gesy.finance.justificatif;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JustificatifFinancierRepository extends JpaRepository<JustificatifFinancier, Long> {
    List<JustificatifFinancier> findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(String ownerType, Long ownerId);
}
