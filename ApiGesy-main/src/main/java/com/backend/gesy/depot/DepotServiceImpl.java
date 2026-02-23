package com.backend.gesy.depot;

import com.backend.gesy.depot.dto.DepotDTO;
import com.backend.gesy.depot.dto.DepotMapper;
import com.backend.gesy.stock.Stock;
import com.backend.gesy.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DepotServiceImpl implements DepotService {
    private final DepotRepository depotRepository;
    private final DepotMapper depotMapper;
    private final StockRepository stockRepository;

    /** Calcule la capacité utilisée à partir des stocks (quantite + quantityCession). */
    private double computeCapaciteUtiliseeFromStocks(Long depotId) {
        List<Stock> stocks = stockRepository.findByDepotId(depotId);
        return stocks.stream()
                .mapToDouble(s -> (s.getQuantite() != null ? s.getQuantite() : 0.0)
                        + (s.getQuantityCession() != null ? s.getQuantityCession() : 0.0))
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepotDTO> findAll() {
        List<DepotDTO> list = depotRepository.findByStatut(Depot.StatutDepot.ACTIF).stream()
                .map(depotMapper::toDTO)
                .collect(Collectors.toList());
        for (DepotDTO dto : list) {
            if (dto.getId() != null) {
                dto.setCapaciteUtilisee(computeCapaciteUtiliseeFromStocks(dto.getId()));
            }
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DepotDTO> findById(Long id) {
        return depotRepository.findById(id)
                .map(depotMapper::toDTO)
                .map(dto -> {
                    dto.setCapaciteUtilisee(computeCapaciteUtiliseeFromStocks(id));
                    return dto;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DepotDTO> findByNom(String nom) {
        return depotRepository.findByNom(nom)
                .map(depotMapper::toDTO)
                .map(dto -> {
                    if (dto.getId() != null) {
                        dto.setCapaciteUtilisee(computeCapaciteUtiliseeFromStocks(dto.getId()));
                    }
                    return dto;
                });
    }

    @Override
    public DepotDTO save(DepotDTO depotDTO) {
        Depot depot = depotMapper.toEntity(depotDTO);
        // Initialiser la capacité utilisée à 0 si elle est null
        if (depot.getCapaciteUtilisee() == null) {
            depot.setCapaciteUtilisee(0.0);
        }
        Depot savedDepot = depotRepository.save(depot);
        return depotMapper.toDTO(savedDepot);
    }

    @Override
    public DepotDTO update(Long id, DepotDTO depotDTO) {
        Depot existingDepot = depotRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Dépôt non trouvé avec l'id: " + id));
        
        // Mettre à jour les champs de l'objet existant
        if (depotDTO.getNom() != null) {
            existingDepot.setNom(depotDTO.getNom());
        }
        if (depotDTO.getVille() != null) {
            existingDepot.setVille(depotDTO.getVille());
        }
        if (depotDTO.getAdresse() != null) {
            existingDepot.setAdresse(depotDTO.getAdresse());
        }
        if (depotDTO.getTelephone() != null) {
            existingDepot.setTelephone(depotDTO.getTelephone());
        }
        if (depotDTO.getResponsable() != null) {
            existingDepot.setResponsable(depotDTO.getResponsable());
        }
        if (depotDTO.getCapacite() != null) {
            existingDepot.setCapacite(depotDTO.getCapacite());
        }
        if (depotDTO.getStatut() != null) {
            existingDepot.setStatut(Depot.StatutDepot.valueOf(depotDTO.getStatut()));
        }
        // Ne pas modifier la capacité utilisée lors de la mise à jour (elle est gérée automatiquement)
        // existingDepot.setCapaciteUtilisee reste inchangé
        
        Depot updatedDepot = depotRepository.save(existingDepot);
        return depotMapper.toDTO(updatedDepot);
    }

    @Override
    public void deleteById(Long id) {
        Depot depot = depotRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Dépôt non trouvé avec l'id: " + id));
        depot.setStatut(Depot.StatutDepot.INACTIF);
        depotRepository.save(depot);
    }
}

