package com.backend.gesy.douane;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoriqueDouaneRepository extends JpaRepository<HistoriqueDouane, Long> {
    List<HistoriqueDouane> findAllByOrderByDateModificationDesc();
    List<HistoriqueDouane> findByModifieParOrderByDateModificationDesc(String modifiePar);
    List<HistoriqueDouane> findByDateModificationBetweenOrderByDateModificationDesc(LocalDateTime debut, LocalDateTime fin);
}
