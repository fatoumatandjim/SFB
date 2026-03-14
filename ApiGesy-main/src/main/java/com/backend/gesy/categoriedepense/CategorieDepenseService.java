package com.backend.gesy.categoriedepense;

import com.backend.gesy.categoriedepense.dto.CategorieDepenseDTO;

import java.util.List;
import java.util.Optional;

public interface CategorieDepenseService {
    List<CategorieDepenseDTO> findAll();
    List<CategorieDepenseDTO> findAllActives();
    Optional<CategorieDepenseDTO> findById(Long id);
    CategorieDepenseDTO save(CategorieDepenseDTO dto);
    CategorieDepenseDTO update(Long id, CategorieDepenseDTO dto);
    void deleteById(Long id);
    /** Retourne la catégorie par nom, ou la crée si elle n'existe pas (ex: Coût de transport, Frais T1, Droit de douane). */
    CategorieDepenseDTO getOrCreateByName(String nom);

    /** Liste des prix unitaires transport (FCFA/litre) liés à une catégorie par son nom (ex: "Coût de transport"). */
    java.util.List<Integer> getTarifsTransportByCategorieNom(String nomCategorie);
}

