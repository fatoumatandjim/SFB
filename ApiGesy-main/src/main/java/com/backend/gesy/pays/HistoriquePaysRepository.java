package com.backend.gesy.pays;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriquePaysRepository extends JpaRepository<HistoriquePays, Long> {
    List<HistoriquePays> findByPaysIdOrderByDateModificationDesc(Long paysId);
}
