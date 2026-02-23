package com.backend.gesy.comptebancaire;

import com.backend.gesy.caisse.Caisse;
import com.backend.gesy.caisse.CaisseRepository;
import com.backend.gesy.comptebancaire.dto.BanqueCaisseStatsDTO;
import com.backend.gesy.comptebancaire.dto.CompteBancaireDTO;
import com.backend.gesy.comptebancaire.dto.CompteBancaireMapper;
import com.backend.gesy.transaction.Transaction;
import com.backend.gesy.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CompteBancaireServiceImpl implements CompteBancaireService {
    private final CompteBancaireRepository compteBancaireRepository;
    private final CompteBancaireMapper compteBancaireMapper;
    private final CaisseRepository caisseRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public List<CompteBancaireDTO> findAll() {
        return compteBancaireRepository.findAll().stream()
            .map(compteBancaireMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<CompteBancaireDTO> findById(Long id) {
        return compteBancaireRepository.findById(id)
            .map(compteBancaireMapper::toDTO);
    }

    @Override
    public Optional<CompteBancaireDTO> findByNumero(String numero) {
        return compteBancaireRepository.findByNumero(numero)
            .map(compteBancaireMapper::toDTO);
    }

    @Override
    public CompteBancaireDTO save(CompteBancaireDTO compteDTO) {
        CompteBancaire compte = compteBancaireMapper.toEntity(compteDTO);
        CompteBancaire savedCompte = compteBancaireRepository.save(compte);
        return compteBancaireMapper.toDTO(savedCompte);
    }

    @Override
    public CompteBancaireDTO update(Long id, CompteBancaireDTO compteDTO) {
        CompteBancaire existingCompte = compteBancaireRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé avec l'id: " + id));
        CompteBancaire compte = compteBancaireMapper.toEntity(compteDTO);
        compte.setId(existingCompte.getId());
        CompteBancaire updatedCompte = compteBancaireRepository.save(compte);
        return compteBancaireMapper.toDTO(updatedCompte);
    }

    @Override
    public void deleteById(Long id) {
        compteBancaireRepository.deleteById(id);
    }

    @Override
    public BanqueCaisseStatsDTO getStats() {
        // Récupérer tous les comptes bancaires
        List<CompteBancaire> allComptes = compteBancaireRepository.findAll();
        
        // Calculer le solde total de tous les comptes bancaires
        BigDecimal soldeTotal = allComptes.stream()
            .filter(c -> c.getStatut() == CompteBancaire.StatutCompte.ACTIF)
            .map(CompteBancaire::getSolde)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculer le solde total du mois dernier pour l'évolution
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate startOfLastMonth = startOfMonth.minusMonths(1);

        // Pour l'évolution, on compare avec le solde total du mois dernier
        // On peut utiliser les transactions pour calculer l'évolution
        List<Transaction> transactionsCeMois = transactionRepository.findAll().stream()
            .filter(t -> t.getDate() != null && 
                t.getDate().toLocalDate().isAfter(startOfMonth.minusDays(1)) &&
                t.getDate().toLocalDate().isBefore(now.plusDays(1)) &&
                t.getStatut() == Transaction.StatutTransaction.VALIDE)
            .toList();

        List<Transaction> transactionsMoisDernier = transactionRepository.findAll().stream()
            .filter(t -> t.getDate() != null &&
                t.getDate().toLocalDate().isAfter(startOfLastMonth.minusDays(1)) &&
                t.getDate().toLocalDate().isBefore(startOfMonth) &&
                t.getStatut() == Transaction.StatutTransaction.VALIDE)
            .toList();

        BigDecimal entreesCeMois = transactionsCeMois.stream()
            .filter(t -> t.getType() == Transaction.TypeTransaction.VIREMENT_ENTRANT ||
                        t.getType() == Transaction.TypeTransaction.DEPOT)
            .map(Transaction::getMontant)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal sortiesCeMois = transactionsCeMois.stream()
            .filter(t -> t.getType() == Transaction.TypeTransaction.VIREMENT_SORTANT ||
                        t.getType() == Transaction.TypeTransaction.RETRAIT)
            .map(Transaction::getMontant)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal entreesMoisDernier = transactionsMoisDernier.stream()
            .filter(t -> t.getType() == Transaction.TypeTransaction.VIREMENT_ENTRANT ||
                        t.getType() == Transaction.TypeTransaction.DEPOT)
            .map(Transaction::getMontant)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal sortiesMoisDernier = transactionsMoisDernier.stream()
            .filter(t -> t.getType() == Transaction.TypeTransaction.VIREMENT_SORTANT ||
                        t.getType() == Transaction.TypeTransaction.RETRAIT)
            .map(Transaction::getMontant)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal soldeNetCeMois = entreesCeMois.subtract(sortiesCeMois);
        BigDecimal soldeNetMoisDernier = entreesMoisDernier.subtract(sortiesMoisDernier);

        String evolution = "0%";
        if (soldeNetMoisDernier.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal difference = soldeNetCeMois.subtract(soldeNetMoisDernier);
            BigDecimal pourcentage = difference.divide(soldeNetMoisDernier.abs(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            evolution = String.format("%.1f%%", pourcentage);
            if (pourcentage.compareTo(BigDecimal.ZERO) > 0) {
                evolution = "+" + evolution;
            }
        } else if (soldeNetCeMois.compareTo(BigDecimal.ZERO) > 0) {
            evolution = "+100%";
        }

        // Récupérer la caisse principale
        Optional<Caisse> caissePrincipaleOpt = caisseRepository.findByNom("Caisse Principale");
        BigDecimal soldeCaisse = BigDecimal.ZERO;
        BigDecimal entreesCaisse = BigDecimal.ZERO;
        BigDecimal sortiesCaisse = BigDecimal.ZERO;

        if (caissePrincipaleOpt.isPresent()) {
            Caisse caissePrincipale = caissePrincipaleOpt.get();
            soldeCaisse = caissePrincipale.getSolde();

            // Calculer les entrées et sorties de la caisse (transactions validées)
            List<Transaction> transactionsCaisse = transactionRepository.findByCaisse(caissePrincipale);
            List<Transaction> transactionsCaisseValidees = transactionsCaisse.stream()
                .filter(t -> t.getStatut() == Transaction.StatutTransaction.VALIDE)
                .toList();

            entreesCaisse = transactionsCaisseValidees.stream()
                .filter(t -> t.getType() == Transaction.TypeTransaction.DEPOT)
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            sortiesCaisse = transactionsCaisseValidees.stream()
                .filter(t -> t.getType() == Transaction.TypeTransaction.RETRAIT)
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Calculer les totaux d'entrées et sorties (toutes les transactions validées)
        List<Transaction> allTransactionsValidees = transactionRepository.findAll().stream()
            .filter(t -> t.getStatut() == Transaction.StatutTransaction.VALIDE)
            .toList();

        BigDecimal totalEntrees = allTransactionsValidees.stream()
            .filter(t -> t.getType() == Transaction.TypeTransaction.VIREMENT_ENTRANT ||
                        t.getType() == Transaction.TypeTransaction.DEPOT)
            .map(Transaction::getMontant)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSorties = allTransactionsValidees.stream()
            .filter(t -> t.getType() == Transaction.TypeTransaction.VIREMENT_SORTANT ||
                        t.getType() == Transaction.TypeTransaction.RETRAIT ||
                        t.getType() == Transaction.TypeTransaction.FRAIS ||
                        t.getType() == Transaction.TypeTransaction.FRAIS_LOCATION ||
                        t.getType() == Transaction.TypeTransaction.FRAIS_FRONTIERE ||
                        t.getType() == Transaction.TypeTransaction.TS_FRAIS_PRESTATIONS ||
                        t.getType() == Transaction.TypeTransaction.FRAIS_REPERTOIRE ||
                        t.getType() == Transaction.TypeTransaction.FRAIS_CHAMBRE_COMMERCE ||
                        t.getType() == Transaction.TypeTransaction.SALAIRE)
            .map(Transaction::getMontant)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Compter les comptes bancaires
        long totalComptes = allComptes.size();
        long comptesActifs = allComptes.stream()
            .filter(c -> c.getStatut() == CompteBancaire.StatutCompte.ACTIF)
            .count();

        // Construire le DTO
        BanqueCaisseStatsDTO stats = new BanqueCaisseStatsDTO();

        // Solde Total
        BanqueCaisseStatsDTO.SoldeTotal soldeTotalDTO = new BanqueCaisseStatsDTO.SoldeTotal();
        soldeTotalDTO.setMontant(soldeTotal);
        soldeTotalDTO.setEvolution(evolution);
        soldeTotalDTO.setPeriode("vs mois dernier");
        stats.setSoldeTotal(soldeTotalDTO);

        // Solde Caisse
        BanqueCaisseStatsDTO.SoldeCaisse soldeCaisseDTO = new BanqueCaisseStatsDTO.SoldeCaisse();
        soldeCaisseDTO.setMontant(soldeCaisse);
        soldeCaisseDTO.setEntrees(entreesCaisse);
        soldeCaisseDTO.setSorties(sortiesCaisse);
        soldeCaisseDTO.setDate("Arrêté aujourd'hui");
        stats.setSoldeCaisse(soldeCaisseDTO);

        // Comptes Bancaires
        BanqueCaisseStatsDTO.ComptesBancaires comptesBancairesDTO = new BanqueCaisseStatsDTO.ComptesBancaires();
        comptesBancairesDTO.setTotal(totalComptes);
        comptesBancairesDTO.setActifs(comptesActifs);
        stats.setComptesBancaires(comptesBancairesDTO);

        // Total Entrées
        BanqueCaisseStatsDTO.TotalEntrees totalEntreesDTO = new BanqueCaisseStatsDTO.TotalEntrees();
        totalEntreesDTO.setMontant(totalEntrees);
        stats.setTotalEntrees(totalEntreesDTO);

        // Total Sorties
        BanqueCaisseStatsDTO.TotalSorties totalSortiesDTO = new BanqueCaisseStatsDTO.TotalSorties();
        totalSortiesDTO.setMontant(totalSorties);
        stats.setTotalSorties(totalSortiesDTO);

        return stats;
    }
}

