package com.backend.gesy.douane;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DouaneRepository extends JpaRepository<Douane, Long> {
    // Il n'y aura qu'une seule instance de Douane
    Douane findFirstByOrderByIdAsc();
}

