package com.backend.gesy.comptebancaire;

import com.backend.gesy.comptebancaire.dto.BanqueCaisseStatsDTO;
import com.backend.gesy.comptebancaire.dto.CompteBancaireDTO;

import java.util.List;
import java.util.Optional;

public interface CompteBancaireService {
    List<CompteBancaireDTO> findAll();
    Optional<CompteBancaireDTO> findById(Long id);
    Optional<CompteBancaireDTO> findByNumero(String numero);
    CompteBancaireDTO save(CompteBancaireDTO compteDTO);
    CompteBancaireDTO update(Long id, CompteBancaireDTO compteDTO);
    void deleteById(Long id);
    BanqueCaisseStatsDTO getStats();
}

