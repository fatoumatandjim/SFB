package com.backend.gesy.douane;

import com.backend.gesy.axe.Axe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FraisDouaneAxeRepository extends JpaRepository<FraisDouaneAxe, Long> {
    Optional<FraisDouaneAxe> findByAxe(Axe axe);
    Optional<FraisDouaneAxe> findByAxeId(Long axeId);
    boolean existsByAxeId(Long axeId);
}
