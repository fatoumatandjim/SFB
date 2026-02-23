package com.backend.gesy.depense;

import com.backend.gesy.caisse.CaisseRepository;
import com.backend.gesy.categoriedepense.CategorieDepense;
import com.backend.gesy.categoriedepense.CategorieDepenseRepository;
import com.backend.gesy.comptebancaire.CompteBancaireRepository;
import com.backend.gesy.depense.dto.DepenseDTO;
import com.backend.gesy.depense.dto.DepenseMapper;
import com.backend.gesy.depense.dto.DepensePageDTO;
import com.backend.gesy.transaction.TransactionService;
import com.backend.gesy.transaction.dto.TransactionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DepenseServiceImpl implements DepenseService {
    
    private final DepenseRepository depenseRepository;
    private final CategorieDepenseRepository categorieDepenseRepository;
    private final CompteBancaireRepository compteBancaireRepository;
    private final CaisseRepository caisseRepository;
    private final DepenseMapper depenseMapper;
    private final TransactionService transactionService;

    @Override
    public DepenseDTO save(DepenseDTO dto) {
        if (dto.getMontant() == null || dto.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le montant de la dépense doit être supérieur à zéro");
        }
        boolean fromCompte = dto.getCompteId() != null;
        boolean fromCaisse = dto.getCaisseId() != null;
        if (fromCompte == fromCaisse) {
            throw new RuntimeException("Veuillez sélectionner soit un compte bancaire, soit une caisse pour déduire le montant (pas les deux, pas aucun)");
        }

        Depense entity = depenseMapper.toEntity(dto);
        entity.setDateCreation(LocalDateTime.now());
        entity.setCreePar(getCurrentUsername());
        if (dto.getDateDepense() == null) {
            entity.setDateDepense(LocalDateTime.now());
        }

        Depense saved = depenseRepository.save(entity);

        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setMontant(saved.getMontant());
        transactionDTO.setDate(saved.getDateDepense());
        transactionDTO.setType(fromCompte ? "VIREMENT_SORTANT" : "RETRAIT");
        transactionDTO.setStatut("VALIDE");
        transactionDTO.setDescription("Dépense: " + (saved.getLibelle() != null ? saved.getLibelle() : ""));
        transactionDTO.setReference(saved.getReference());
        if (fromCompte) {
            transactionDTO.setCompteId(dto.getCompteId());
        } else {
            transactionDTO.setCaisseId(dto.getCaisseId());
        }
        transactionService.createPaiement(transactionDTO);

        return depenseMapper.toDTO(saved);
    }

    @Override
    public DepenseDTO update(Long id, DepenseDTO dto) {
        Depense existing = depenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dépense non trouvée avec l'id: " + id));
        
        existing.setLibelle(dto.getLibelle());
        existing.setMontant(dto.getMontant());
        existing.setDateDepense(dto.getDateDepense());
        existing.setDescription(dto.getDescription());
        existing.setReference(dto.getReference());

        if (dto.getCategorieId() != null) {
            CategorieDepense categorie = categorieDepenseRepository.findById(dto.getCategorieId())
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id: " + dto.getCategorieId()));
            existing.setCategorie(categorie);
        }

        if (dto.getCompteId() != null) {
            existing.setCompteBancaire(compteBancaireRepository.findById(dto.getCompteId())
                    .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé avec l'id: " + dto.getCompteId())));
            existing.setCaisse(null);
        } else if (dto.getCaisseId() != null) {
            existing.setCaisse(caisseRepository.findById(dto.getCaisseId())
                    .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id: " + dto.getCaisseId())));
            existing.setCompteBancaire(null);
        } else {
            existing.setCompteBancaire(null);
            existing.setCaisse(null);
        }

        return depenseMapper.toDTO(depenseRepository.save(existing));
    }

    @Override
    public Optional<DepenseDTO> findById(Long id) {
        return depenseRepository.findById(id).map(depenseMapper::toDTO);
    }

    @Override
    public void deleteById(Long id) {
        if (!depenseRepository.existsById(id)) {
            throw new RuntimeException("Dépense non trouvée avec l'id: " + id);
        }
        depenseRepository.deleteById(id);
    }

    @Override
    public List<DepenseDTO> findAll() {
        return depenseRepository.findByOrderByDateDepenseDesc().stream()
                .map(depenseMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DepenseDTO> findByCategorie(Long categorieId) {
        CategorieDepense categorie = getCategorieById(categorieId);
        return depenseRepository.findByCategorieOrderByDateDepenseDesc(categorie).stream()
                .map(depenseMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DepensePageDTO findAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Depense> depensePage = depenseRepository.findAllByOrderByDateDepenseDesc(pageable);
        return toPageDTO(depensePage);
    }

    @Override
    public DepensePageDTO findByCategoriePaginated(Long categorieId, int page, int size) {
        CategorieDepense categorie = getCategorieById(categorieId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Depense> depensePage = depenseRepository.findByCategorieOrderByDateDepenseDesc(categorie, pageable);
        return toPageDTO(depensePage);
    }

    @Override
    public DepensePageDTO findByDate(LocalDate date, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime dateTime = date.atStartOfDay();
        Page<Depense> depensePage = depenseRepository.findByDate(dateTime, pageable);
        return toPageDTO(depensePage);
    }

    @Override
    public DepensePageDTO findByDateRange(LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        Page<Depense> depensePage = depenseRepository.findByDateRange(start, end, pageable);
        return toPageDTO(depensePage);
    }

    @Override
    public DepensePageDTO findByCategorieAndDate(Long categorieId, LocalDate date, int page, int size) {
        CategorieDepense categorie = getCategorieById(categorieId);
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime dateTime = date.atStartOfDay();
        Page<Depense> depensePage = depenseRepository.findByCategorieAndDate(categorie, dateTime, pageable);
        return toPageDTO(depensePage);
    }

    @Override
    public DepensePageDTO findByCategorieAndDateRange(Long categorieId, LocalDate startDate, LocalDate endDate, int page, int size) {
        CategorieDepense categorie = getCategorieById(categorieId);
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        Page<Depense> depensePage = depenseRepository.findByCategorieAndDateRange(categorie, start, end, pageable);
        return toPageDTO(depensePage);
    }

    @Override
    public BigDecimal sumByCategorie(Long categorieId) {
        CategorieDepense categorie = getCategorieById(categorieId);
        BigDecimal sum = depenseRepository.sumByCategorie(categorie);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal sumByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        BigDecimal sum = depenseRepository.sumByDateRange(start, end);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private CategorieDepense getCategorieById(Long categorieId) {
        return categorieDepenseRepository.findById(categorieId)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id: " + categorieId));
    }

    private DepensePageDTO toPageDTO(Page<Depense> depensePage) {
        List<DepenseDTO> depenses = depensePage.getContent().stream()
                .map(depenseMapper::toDTO)
                .collect(Collectors.toList());
        
        return new DepensePageDTO(
                depenses,
                depensePage.getNumber(),
                depensePage.getSize(),
                depensePage.getTotalElements(),
                depensePage.getTotalPages()
        );
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "Système";
    }
}

