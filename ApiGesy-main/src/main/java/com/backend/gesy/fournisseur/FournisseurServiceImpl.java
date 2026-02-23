package com.backend.gesy.fournisseur;

import com.backend.gesy.fournisseur.dto.FournisseurDTO;
import com.backend.gesy.fournisseur.dto.FournisseurMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FournisseurServiceImpl implements FournisseurService {
    private final FournisseurRepository fournisseurRepository;
    private final FournisseurMapper fournisseurMapper;

    @Override
    public List<FournisseurDTO> findAll() {
        return fournisseurRepository.findAll().stream()
            .map(fournisseurMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<FournisseurDTO> findById(Long id) {
        return fournisseurRepository.findById(id)
            .map(fournisseurMapper::toDTO);
    }

    @Override
    public Optional<FournisseurDTO> findByEmail(String email) {
        return fournisseurRepository.findByEmail(email)
            .map(fournisseurMapper::toDTO);
    }

    @Override
    public Optional<FournisseurDTO> findByCodeFournisseur(String codeFournisseur) {
        return fournisseurRepository.findByCodeFournisseur(codeFournisseur)
            .map(fournisseurMapper::toDTO);
    }

    @Override
    public List<FournisseurDTO> findByType(String type) {
        Fournisseur.TypeFournisseur typeFournisseur = Fournisseur.TypeFournisseur.valueOf(type.toUpperCase());
        return fournisseurRepository.findByTypeFournisseur(typeFournisseur).stream()
            .map(fournisseurMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public FournisseurDTO save(FournisseurDTO fournisseurDTO) {
        Fournisseur fournisseur = fournisseurMapper.toEntity(fournisseurDTO);
        
        // Générer un code fournisseur unique si non fourni
        if (fournisseur.getCodeFournisseur() == null || fournisseur.getCodeFournisseur().trim().isEmpty()) {
            String codeFournisseur = generateUniqueCodeFournisseur();
            fournisseur.setCodeFournisseur(codeFournisseur);
        } else {
            // Vérifier l'unicité du code fourni
            if (fournisseurRepository.findByCodeFournisseur(fournisseur.getCodeFournisseur()).isPresent()) {
                throw new RuntimeException("Le code fournisseur " + fournisseur.getCodeFournisseur() + " existe déjà");
            }
        }
        
        Fournisseur savedFournisseur = fournisseurRepository.save(fournisseur);
        return fournisseurMapper.toDTO(savedFournisseur);
    }
    
    /**
     * Génère un code fournisseur unique au format FRS-XXX
     */
    private String generateUniqueCodeFournisseur() {
        String prefix = "FRS-";
        int nextNumber = 1;
        
        // Trouver le dernier numéro
        List<Fournisseur> fournisseurs = fournisseurRepository.findAll();
        for (Fournisseur fournisseur : fournisseurs) {
            if (fournisseur.getCodeFournisseur() != null && fournisseur.getCodeFournisseur().startsWith(prefix)) {
                try {
                    String numberPart = fournisseur.getCodeFournisseur().substring(prefix.length());
                    int num = Integer.parseInt(numberPart);
                    if (num >= nextNumber) {
                        nextNumber = num + 1;
                    }
                } catch (NumberFormatException e) {
                    // Ignorer les codes mal formatés
                }
            }
        }
        
        String codeFournisseur = prefix + String.format("%03d", nextNumber);
        
        // Vérifier l'unicité
        while (fournisseurRepository.findByCodeFournisseur(codeFournisseur).isPresent()) {
            nextNumber++;
            codeFournisseur = prefix + String.format("%03d", nextNumber);
        }
        
        return codeFournisseur;
    }

    @Override
    public FournisseurDTO update(Long id, FournisseurDTO fournisseurDTO) {
        Fournisseur existingFournisseur = fournisseurRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé avec l'id: " + id));
        Fournisseur fournisseur = fournisseurMapper.toEntity(fournisseurDTO);
        fournisseur.setId(existingFournisseur.getId());
        
        // Si le code fournisseur change, vérifier l'unicité
        if (fournisseur.getCodeFournisseur() != null && !fournisseur.getCodeFournisseur().equals(existingFournisseur.getCodeFournisseur())) {
            if (fournisseurRepository.findByCodeFournisseur(fournisseur.getCodeFournisseur()).isPresent()) {
                throw new RuntimeException("Le code fournisseur " + fournisseur.getCodeFournisseur() + " existe déjà");
            }
        } else {
            // Conserver le code existant si non modifié
            fournisseur.setCodeFournisseur(existingFournisseur.getCodeFournisseur());
        }
        
        Fournisseur updatedFournisseur = fournisseurRepository.save(fournisseur);
        return fournisseurMapper.toDTO(updatedFournisseur);
    }

    @Override
    public void deleteById(Long id) {
        fournisseurRepository.deleteById(id);
    }
}

