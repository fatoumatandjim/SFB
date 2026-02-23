package com.backend.gesy.transitaire;

import com.backend.gesy.transitaire.dto.TransitaireDTO;
import com.backend.gesy.transitaire.dto.TransitairePageDto;
import java.util.List;
import java.util.Optional;

public interface TransitaireService {
    List<TransitaireDTO> findAll();
    TransitairePageDto findAllPaginated(int page, int size);
    Optional<TransitaireDTO> findById(Long id);
    Optional<TransitaireDTO> findByIdentifiant(String identifiant);
    Optional<TransitaireDTO> findByEmail(String email);
    TransitaireDTO save(TransitaireDTO transitaireDTO);
    TransitaireDTO update(Long id, TransitaireDTO transitaireDTO);
    void deleteById(Long id);
}

