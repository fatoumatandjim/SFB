package com.backend.gesy.transitaire;

import com.backend.gesy.roles.Roles;
import com.backend.gesy.roles.RolesRepository;
import com.backend.gesy.transitaire.dto.TransitaireDTO;
import com.backend.gesy.transitaire.dto.TransitaireMapper;
import com.backend.gesy.transitaire.dto.TransitairePageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransitaireServiceImpl implements TransitaireService {
    private final TransitaireRepository transitaireRepository;
    private final TransitaireMapper transitaireMapper;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();
    private final RolesRepository rolesRepository;

    @Override
    public List<TransitaireDTO> findAll() {
        return transitaireRepository.findAll().stream()
            .map(transitaireMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public TransitairePageDto findAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transitaire> transitairePage = transitaireRepository.findAllOrderByCreatedAtDesc(pageable);
        
        List<TransitaireDTO> transitaires = transitairePage.getContent().stream()
            .map(transitaireMapper::toDTO)
            .collect(Collectors.toList());
        
        return new TransitairePageDto(
            transitaires,
            transitairePage.getNumber(),
            transitairePage.getTotalPages(),
            transitairePage.getTotalElements(),
            transitairePage.getSize()
        );
    }

    @Override
    public Optional<TransitaireDTO> findById(Long id) {
        return transitaireRepository.findById(id)
            .map(transitaireMapper::toDTO);
    }

    @Override
    public Optional<TransitaireDTO> findByIdentifiant(String identifiant) {
        return transitaireRepository.findByIdentifiant(identifiant)
            .map(transitaireMapper::toDTO);
    }

    @Override
    public Optional<TransitaireDTO> findByEmail(String email) {
        return transitaireRepository.findByEmail(email)
            .map(transitaireMapper::toDTO);
    }

    @Override
    public TransitaireDTO save(TransitaireDTO transitaireDTO) {
        Transitaire transitaire = transitaireMapper.toEntity(transitaireDTO);
        
        // Générer un identifiant unique au format TRS-XXX
        if (transitaire.getIdentifiant() == null || transitaire.getIdentifiant().isEmpty()) {
            String identifiant = generateUniqueIdentifiant();
            transitaire.setIdentifiant(identifiant);
        }

        // Générer un mot de passe au format mdp@XXX
        String motDePasse = generatePassword();
        transitaire.setDefaultPass(motDePasse); // Stocker le mot de passe en clair dans defaultPass
        // Encoder le mot de passe avec BCrypt
        transitaire.setMotDePasse(passwordEncoder.encode(motDePasse));

        Roles roles = rolesRepository.findByNom("Transitaire")
            .orElseThrow(() -> new RuntimeException("Rôle TRANSITAIRE non trouvé"));
        transitaire.getRoles().add(roles);
        Transitaire savedTransitaire = transitaireRepository.save(transitaire);


        return transitaireMapper.toDTO(savedTransitaire);
    }

    @Override
    public TransitaireDTO update(Long id, TransitaireDTO transitaireDTO) {
        Transitaire existingTransitaire = transitaireRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Transitaire non trouvé avec l'id: " + id));
        
        Transitaire transitaire = transitaireMapper.toEntity(transitaireDTO);
        transitaire.setId(existingTransitaire.getId());
        transitaire.setIdentifiant(existingTransitaire.getIdentifiant()); // Conserver l'identifiant existant
        transitaire.setVoyages(existingTransitaire.getVoyages()); // Conserver les voyages existants
        transitaire.setRoles(existingTransitaire.getRoles()); // Conserver les rôles existants
        
        // Ne pas modifier le mot de passe lors de la mise à jour sauf si explicitement fourni
        if (transitaireDTO.getMotDePasse() == null || transitaireDTO.getMotDePasse().isEmpty()) {
            transitaire.setMotDePasse(existingTransitaire.getMotDePasse());
            transitaire.setDefaultPass(existingTransitaire.getDefaultPass());
        }
        
        Transitaire updatedTransitaire = transitaireRepository.save(transitaire);
        return transitaireMapper.toDTO(updatedTransitaire);
    }

    @Override
    public void deleteById(Long id) {
        transitaireRepository.deleteById(id);
    }

    /**
     * Génère un identifiant unique au format TRS-XXX (3 chiffres)
     */
    private String generateUniqueIdentifiant() {
        String prefix = "TRS-";
        int maxAttempts = 1000;
        int attempts = 0;

        while (attempts < maxAttempts) {
            int number = random.nextInt(900) + 100; // Génère un nombre entre 100 et 999
            String identifiant = prefix + String.format("%03d", number);

            // Vérifier l'unicité
            if (transitaireRepository.findByIdentifiant(identifiant).isEmpty()) {
                return identifiant;
            }
            attempts++;
        }

        // Si on n'a pas trouvé d'identifiant unique après 1000 tentatives, utiliser un timestamp
        return prefix + String.format("%03d", (int) (System.currentTimeMillis() % 1000));
    }

    /**
     * Génère un mot de passe au format mdp@XXX (3 chiffres)
     */
    private String generatePassword() {
        int number = random.nextInt(900) + 100; // Génère un nombre entre 100 et 999
        return "mdp@" + String.format("%03d", number);
    }
}
