package com.backend.gesy.facture;

import com.backend.gesy.alerte.Alerte;
import com.backend.gesy.alerte.AlerteService;
import com.backend.gesy.client.Client;
import com.backend.gesy.client.ClientRepository;
import com.backend.gesy.facture.dto.CreanceDTO;
import com.backend.gesy.facture.dto.FactureDTO;
import com.backend.gesy.facture.dto.FactureMapper;
import com.backend.gesy.facture.dto.FacturePageDto;
import com.backend.gesy.facture.dto.FactureStatsDTO;
import com.backend.gesy.facture.dto.LigneFactureDTO;
import com.backend.gesy.facture.dto.RecouvrementStatsDTO;
import com.backend.gesy.produit.Produit;
import com.backend.gesy.produit.ProduitRepository;
import com.backend.gesy.transaction.TransactionService;
import com.backend.gesy.transaction.dto.TransactionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FactureServiceImpl implements FactureService {
    private final FactureRepository factureRepository;
    private final ClientRepository clientRepository;
    private final ProduitRepository produitRepository;
    private final LigneFactureRepository ligneFactureRepository;
    private final FactureMapper factureMapper;
    private final TransactionService transactionService;
    private final PdfFactureService pdfFactureService;
    private final AlerteService alerteService;

    @Override
    public List<FactureDTO> findAll() {
        return factureRepository.findAllOrderByDateDesc().stream()
            .map(factureMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public FacturePageDto findAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Facture> facturePage = factureRepository.findAllOrderByDateDesc(pageable);
        
        List<FactureDTO> factureDTOs = facturePage.getContent().stream()
            .map(factureMapper::toDTO)
            .collect(Collectors.toList());
        
        FacturePageDto pageDto = new FacturePageDto();
        pageDto.setFactures(factureDTOs);
        pageDto.setCurrentPage(facturePage.getNumber());
        pageDto.setTotalPages(facturePage.getTotalPages());
        pageDto.setTotalElements(facturePage.getTotalElements());
        pageDto.setSize(facturePage.getSize());
        
        return pageDto;
    }

    @Override
    public Optional<FactureDTO> findById(Long id) {
        return factureRepository.findById(id)
            .map(factureMapper::toDTO);
    }

    @Override
    public Optional<FactureDTO> findByNumero(String numero) {
        return factureRepository.findByNumero(numero)
            .map(factureMapper::toDTO);
    }

    @Override
    public List<FactureDTO> findByClientId(Long clientId) {
        Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + clientId));
        return factureRepository.findByClient(client).stream()
            .map(factureMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public FactureDTO save(FactureDTO factureDTO) {
        // Générer un numéro de facture unique
        String numero = generateUniqueNumeroFacture();
        
        // Récupérer le client
        Client client = clientRepository.findById(factureDTO.getClientId())
            .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + factureDTO.getClientId()));
        
        // Calculer le montant HT à partir des lignes
        BigDecimal montantHT = BigDecimal.ZERO;
        if (factureDTO.getLignes() != null && !factureDTO.getLignes().isEmpty()) {
            for (LigneFactureDTO ligneDTO : factureDTO.getLignes()) {
                if (ligneDTO.getTotal() != null) {
                    montantHT = montantHT.add(ligneDTO.getTotal());
                }
            }
        }
        
        // Calculer les montants TTC
        BigDecimal tauxTVA = factureDTO.getTauxTVA() != null ? factureDTO.getTauxTVA() : BigDecimal.ZERO;
        BigDecimal montantTVA = montantHT.multiply(tauxTVA).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal montantTTC = montantHT.add(montantTVA);
        
        // Créer la facture
        Facture facture = factureMapper.toEntity(factureDTO);
        facture.setNumero(numero);
        facture.setClient(client);
        facture.setMontantHT(montantHT);
        facture.setMontantTTC(montantTTC);
        facture.setTauxTVA(tauxTVA);
        facture.setMontant(montantTTC); // Le montant principal est le TTC
        
        // Sauvegarder la facture
        Facture savedFacture = factureRepository.save(facture);

        alerteService.creerAlerte(Alerte.TypeAlerte.FACTURE_EMISE,
                "Facture émise : " + numero + " - " + client.getNom() + " - " + montantTTC + " FCFA",
                Alerte.PrioriteAlerte.MOYENNE, "Facture", savedFacture.getId(), "/factures/" + savedFacture.getId());

        // Créer les lignes de facture (produits)
        if (factureDTO.getLignes() != null && !factureDTO.getLignes().isEmpty()) {
            for (LigneFactureDTO ligneDTO : factureDTO.getLignes()) {
                Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + ligneDTO.getProduitId()));
                
                LigneFacture ligne = new LigneFacture();
                ligne.setFacture(savedFacture);
                ligne.setProduit(produit);
                ligne.setQuantite(ligneDTO.getQuantite());
                ligne.setPrixUnitaire(ligneDTO.getPrixUnitaire());
                ligne.setTotal(ligneDTO.getTotal());
                
                ligneFactureRepository.save(ligne);
            }
        }
        
        // Créer une transaction si un montant a été payé
        if (factureDTO.getMontantPaye() != null && factureDTO.getMontantPaye().compareTo(BigDecimal.ZERO) > 0) {
            TransactionDTO transactionDTO = new TransactionDTO();
            transactionDTO.setType("VIREMENT_ENTRANT");
            transactionDTO.setMontant(factureDTO.getMontantPaye());
            transactionDTO.setDate(LocalDateTime.now());
            transactionDTO.setStatut("VALIDE");
            transactionDTO.setDescription("Paiement facture " + numero);
            transactionDTO.setReference(numero);
            transactionDTO.setBeneficiaire(client.getNom());
            transactionDTO.setFactureId(savedFacture.getId());
            
            transactionService.save(transactionDTO);
            
            // Mettre à jour le montant payé de la facture
            savedFacture.setMontantPaye(factureDTO.getMontantPaye());
            
            // Mettre à jour le statut de la facture
            if (factureDTO.getMontantPaye().compareTo(montantTTC) >= 0) {
                savedFacture.setStatut(Facture.StatutFacture.PAYEE);
            } else {
                savedFacture.setStatut(Facture.StatutFacture.PARTIELLEMENT_PAYEE);
            }
            
            savedFacture = factureRepository.save(savedFacture);
        }
        
        return factureMapper.toDTO(savedFacture);
    }
    
    private String generateUniqueNumeroFacture() {
        int currentYear = LocalDate.now().getYear();
        String prefix = "INV-" + currentYear + "-";
        
        // Trouver le dernier numéro de facture de l'année
        List<Facture> factures = factureRepository.findAll();
        int nextNumber = 1;
        
        for (Facture facture : factures) {
            if (facture.getNumero() != null && facture.getNumero().startsWith(prefix)) {
                try {
                    String numberPart = facture.getNumero().substring(prefix.length());
                    int num = Integer.parseInt(numberPart);
                    if (num >= nextNumber) {
                        nextNumber = num + 1;
                    }
                } catch (NumberFormatException e) {
                    // Ignorer les numéros mal formatés
                }
            }
        }
        
        String numero = prefix + String.format("%04d", nextNumber);
        
        // Vérifier l'unicité
        while (factureRepository.findByNumero(numero).isPresent()) {
            nextNumber++;
            numero = prefix + String.format("%04d", nextNumber);
        }
        
        return numero;
    }

    @Override
    public FactureDTO update(Long id, FactureDTO factureDTO) {
        Facture existingFacture = factureRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Facture non trouvée avec l'id: " + id));
        Facture facture = factureMapper.toEntity(factureDTO);
        facture.setId(existingFacture.getId());
        if (existingFacture.getVoyage() != null) {
            facture.setVoyage(existingFacture.getVoyage());
        }
        if (factureDTO.getClientId() != null) {
            Client client = clientRepository.findById(factureDTO.getClientId())
                .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + factureDTO.getClientId()));
            facture.setClient(client);
        } else {
            facture.setClient(existingFacture.getClient());
        }
        Facture updatedFacture = factureRepository.save(facture);
        return factureMapper.toDTO(updatedFacture);
    }

    @Override
    public FactureDTO updateStatut(Long id, String statut) {
        Facture facture = factureRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Facture non trouvée avec l'id: " + id));
        
        try {
            Facture.StatutFacture nouveauStatut = Facture.StatutFacture.valueOf(statut);
            facture.setStatut(nouveauStatut);
            Facture updatedFacture = factureRepository.save(facture);
            return factureMapper.toDTO(updatedFacture);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide: " + statut);
        }
    }

    @Override
    public FactureStatsDTO getStats() {
        List<Facture> allFactures = factureRepository.findAll();
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate startOfLastMonth = startOfMonth.minusMonths(1);
        LocalDate endOfLastMonth = startOfMonth.minusDays(1);

        // Factures émises ce mois
        List<Facture> facturesCeMois = allFactures.stream()
            .filter(f -> f.getDate() != null && !f.getDate().isBefore(startOfMonth) && !f.getDate().isAfter(now))
            .collect(Collectors.toList());

        // Factures émises le mois dernier
        List<Facture> facturesMoisDernier = allFactures.stream()
            .filter(f -> f.getDate() != null && !f.getDate().isBefore(startOfLastMonth) && !f.getDate().isAfter(endOfLastMonth))
            .collect(Collectors.toList());

        long totalEmises = facturesCeMois.size();
        BigDecimal montantEmises = facturesCeMois.stream()
            .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalMoisDernier = facturesMoisDernier.size();
        String evolution = "0%";
        if (totalMoisDernier > 0) {
            double pourcentage = ((double)(totalEmises - totalMoisDernier) / totalMoisDernier) * 100;
            evolution = String.format("%.0f%%", pourcentage);
            if (pourcentage > 0) {
                evolution = "+" + evolution;
            }
        } else if (totalEmises > 0) {
            evolution = "+100%";
        }

        // Factures payées
        List<Facture> facturesPayees = allFactures.stream()
            .filter(f -> f.getStatut() == Facture.StatutFacture.PAYEE)
            .collect(Collectors.toList());

        long totalPayees = facturesPayees.size();
        BigDecimal montantPayees = facturesPayees.stream()
            .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Pourcentage de factures payées
        String pourcentagePayees = "0%";
        if (allFactures.size() > 0) {
            double pourcentage = ((double)totalPayees / allFactures.size()) * 100;
            pourcentagePayees = String.format("%.0f%%", pourcentage);
        }

        // Factures impayées (non payées)
        List<Facture> facturesImpayees = allFactures.stream()
            .filter(f -> f.getStatut() != Facture.StatutFacture.PAYEE && f.getStatut() != Facture.StatutFacture.ANNULEE)
            .collect(Collectors.toList());

        long totalImpayees = facturesImpayees.size();
        BigDecimal montantImpayees = facturesImpayees.stream()
            .map(f -> {
                BigDecimal montantTTC = f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO;
                BigDecimal montantPaye = f.getMontantPaye() != null ? f.getMontantPaye() : BigDecimal.ZERO;
                return montantTTC.subtract(montantPaye);
            })
            .filter(m -> m.compareTo(BigDecimal.ZERO) > 0)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Factures en retard (date d'échéance passée et non payées)
        long enRetard = facturesImpayees.stream()
            .filter(f -> f.getDateEcheance() != null && f.getDateEcheance().isBefore(now))
            .count();

        boolean urgent = enRetard > 0;

        // Créer le DTO
        FactureStatsDTO stats = new FactureStatsDTO();
        
        FactureStatsDTO.FacturesEmises facturesEmises = new FactureStatsDTO.FacturesEmises();
        facturesEmises.setTotal(totalEmises);
        facturesEmises.setMontant(montantEmises);
        facturesEmises.setPeriode("Ce mois");
        facturesEmises.setEvolution(evolution);
        stats.setFacturesEmises(facturesEmises);

        FactureStatsDTO.FacturesPayees facturesPayeesDTO = new FactureStatsDTO.FacturesPayees();
        facturesPayeesDTO.setTotal(totalPayees);
        facturesPayeesDTO.setMontant(montantPayees);
        facturesPayeesDTO.setPourcentage(pourcentagePayees);
        stats.setFacturesPayees(facturesPayeesDTO);

        FactureStatsDTO.FacturesImpayees facturesImpayeesDTO = new FactureStatsDTO.FacturesImpayees();
        facturesImpayeesDTO.setTotal(totalImpayees);
        facturesImpayeesDTO.setMontant(montantImpayees);
        facturesImpayeesDTO.setEnRetard(enRetard);
        facturesImpayeesDTO.setUrgent(urgent);
        stats.setFacturesImpayees(facturesImpayeesDTO);

        return stats;
    }

    @Override
    public void deleteById(Long id) {
        factureRepository.deleteById(id);
    }

    @Override
    public List<CreanceDTO> getUnpaidFactures() {
        List<Facture> unpaidFactures = factureRepository.findUnpaidFactures();
        LocalDate today = LocalDate.now();

        return unpaidFactures.stream().map(facture -> {
            CreanceDTO creance = new CreanceDTO();
            creance.setId(facture.getId());
            creance.setFacture(facture.getNumero());
            creance.setClientId(facture.getClient().getId());
            creance.setClientNom(facture.getClient().getNom());
            creance.setClientEmail(facture.getClient().getEmail());
            creance.setClientTelephone(facture.getClient().getTelephone());
            creance.setMontant(facture.getMontantTTC() != null ? facture.getMontantTTC() : facture.getMontant());
            creance.setMontantPaye(facture.getMontantPaye() != null ? facture.getMontantPaye() : BigDecimal.ZERO);
            creance.setResteAPayer(creance.getMontant().subtract(creance.getMontantPaye()));
            creance.setDateEmission(facture.getDate());
            creance.setDateEcheance(facture.getDateEcheance());
            
            // Calculer les jours de retard
            if (facture.getDateEcheance() != null && facture.getDateEcheance().isBefore(today)) {
                creance.setJoursRetard(ChronoUnit.DAYS.between(facture.getDateEcheance(), today));
            } else {
                creance.setJoursRetard(0L);
            }
            
            // Déterminer le statut
            if (facture.getStatut() == Facture.StatutFacture.PAYEE) {
                creance.setStatut("recouvre");
            } else if (facture.getDateEcheance() != null && facture.getDateEcheance().isBefore(today)) {
                creance.setStatut("en-retard");
            } else if (creance.getMontantPaye().compareTo(BigDecimal.ZERO) == 0) {
                creance.setStatut("impaye");
            } else {
                creance.setStatut("en-cours");
            }
            
            // Déterminer la priorité
            if (creance.getJoursRetard() > 20) {
                creance.setPriorite("haute");
            } else if (creance.getJoursRetard() > 10) {
                creance.setPriorite("moyenne");
            } else {
                creance.setPriorite("basse");
            }
            
            // Pour l'instant, on met des valeurs par défaut pour relances et dernierContact
            // Ces valeurs pourraient être stockées dans une table séparée plus tard
            creance.setRelances(0);
            creance.setDernierContact(facture.getDate());
            
            return creance;
        }).collect(Collectors.toList());
    }

    @Override
    public RecouvrementStatsDTO getRecouvrementStats() {
        List<Facture> allFactures = factureRepository.findAll();
        List<Facture> unpaidFactures = factureRepository.findUnpaidFactures();
        List<Facture> overdueFactures = factureRepository.findOverdueFactures();
        LocalDate today = LocalDate.now();

        // Total créances (toutes les factures non payées)
        BigDecimal totalCreancesMontant = unpaidFactures.stream()
            .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : f.getMontant())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Long totalCreancesNombre = (long) unpaidFactures.size();

        // En retard (factures avec dateEcheance < aujourd'hui et non payées)
        BigDecimal enRetardMontant = overdueFactures.stream()
            .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : f.getMontant())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Long enRetardNombre = (long) overdueFactures.size();
        
        // Calculer les jours moyens de retard
        Long joursMoyen = 0L;
        if (!overdueFactures.isEmpty()) {
            Long totalJours = overdueFactures.stream()
                .filter(f -> f.getDateEcheance() != null)
                .mapToLong(f -> ChronoUnit.DAYS.between(f.getDateEcheance(), today))
                .sum();
            joursMoyen = totalJours / overdueFactures.size();
        }

        // Recouvré (factures payées)
        List<Facture> paidFactures = allFactures.stream()
            .filter(f -> f.getMontantPaye() != null && 
                        f.getMontantTTC() != null &&
                        f.getMontantPaye().compareTo(f.getMontantTTC()) >= 0)
            .collect(Collectors.toList());
        BigDecimal recouvreMontant = paidFactures.stream()
            .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : f.getMontant())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Long recouvreNombre = (long) paidFactures.size();
        
        // Pourcentage recouvré
        String pourcentageRecouvre = "0%";
        if (allFactures.size() > 0) {
            double pourcentage = ((double) recouvreNombre / allFactures.size()) * 100;
            pourcentageRecouvre = String.format("%.0f%%", pourcentage);
        }

        // Impayé (factures avec montantPaye = 0)
        List<Facture> impayeFactures = unpaidFactures.stream()
            .filter(f -> f.getMontantPaye() == null || f.getMontantPaye().compareTo(BigDecimal.ZERO) == 0)
            .collect(Collectors.toList());
        BigDecimal impayeMontant = impayeFactures.stream()
            .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : f.getMontant())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Long impayeNombre = (long) impayeFactures.size();

        // Créer le DTO
        RecouvrementStatsDTO stats = new RecouvrementStatsDTO();
        
        RecouvrementStatsDTO.TotalCreances totalCreances = new RecouvrementStatsDTO.TotalCreances();
        totalCreances.setMontant(totalCreancesMontant);
        totalCreances.setNombre(totalCreancesNombre);
        stats.setTotalCreances(totalCreances);

        RecouvrementStatsDTO.EnRetard enRetard = new RecouvrementStatsDTO.EnRetard();
        enRetard.setMontant(enRetardMontant);
        enRetard.setNombre(enRetardNombre);
        enRetard.setJoursMoyen(joursMoyen);
        stats.setEnRetard(enRetard);

        RecouvrementStatsDTO.Recouvre recouvre = new RecouvrementStatsDTO.Recouvre();
        recouvre.setMontant(recouvreMontant);
        recouvre.setNombre(recouvreNombre);
        recouvre.setPourcentage(pourcentageRecouvre);
        stats.setRecouvre(recouvre);

        RecouvrementStatsDTO.Impaye impaye = new RecouvrementStatsDTO.Impaye();
        impaye.setMontant(impayeMontant);
        impaye.setNombre(impayeNombre);
        stats.setImpaye(impaye);

        return stats;
    }

    @Override
    public byte[] generateFacturesPdf(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + clientId));
        
        // Récupérer toutes les factures du client (sans pagination)
        List<Facture> factures = factureRepository.findByClient(client);
        
        // Générer le PDF
        return pdfFactureService.generateFacturesPdf(client, factures);
    }
}

