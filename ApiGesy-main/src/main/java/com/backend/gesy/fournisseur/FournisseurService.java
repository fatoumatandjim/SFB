package com.backend.gesy.fournisseur;

import com.backend.gesy.fournisseur.dto.FournisseurDTO;

import java.util.List;
import java.util.Optional;

public interface FournisseurService {
    List<FournisseurDTO> findAll();
    Optional<FournisseurDTO> findById(Long id);
    Optional<FournisseurDTO> findByEmail(String email);
    Optional<FournisseurDTO> findByCodeFournisseur(String codeFournisseur);
    List<FournisseurDTO> findByType(String type);
    FournisseurDTO save(FournisseurDTO fournisseurDTO);
    FournisseurDTO update(Long id, FournisseurDTO fournisseurDTO);
    void deleteById(Long id);
}

