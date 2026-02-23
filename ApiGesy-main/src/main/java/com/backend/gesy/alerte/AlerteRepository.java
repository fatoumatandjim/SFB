package com.backend.gesy.alerte;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlerteRepository extends JpaRepository<Alerte, Long> {
    List<Alerte> findByLu(Boolean lu);
    List<Alerte> findByType(Alerte.TypeAlerte type);

    /** Pagination : alertes triées par date décroissante. */
    Page<Alerte> findAllByOrderByDateDesc(Pageable pageable);
}

