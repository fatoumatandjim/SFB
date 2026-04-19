package com.backend.gesy.comptebancaire;

import com.backend.gesy.caisse.Caisse;
import com.backend.gesy.caisse.CaisseRepository;
import com.backend.gesy.compte.Compte;
import com.backend.gesy.compte.CompteRepository;
import com.backend.gesy.comptebancaire.dto.BanqueCaisseStatsDTO;
import com.backend.gesy.comptebancaire.dto.CompteBancaireDTO;
import com.backend.gesy.comptebancaire.dto.CompteBancaireMapper;
import com.backend.gesy.finance.FinanceEntityAccessService;
import com.backend.gesy.transaction.Transaction;
import com.backend.gesy.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CompteBancaireServiceImpl implements CompteBancaireService {
    private final CompteBancaireRepository compteBancaireRepository;
    private final CompteBancaireMapper compteBancaireMapper;
    private final CaisseRepository caisseRepository;
    private final TransactionRepository transactionRepository;
    private final CompteRepository compteRepository;
    private final FinanceEntityAccessService financeEntityAccessService;

    private void applyResponsables(CompteBancaire compte, List<Long> responsableIds) {
        if (compte.getResponsables() == null) {
            compte.setResponsables(new HashSet<>());
        }
        compte.getResponsables().clear();
        if (responsableIds == null) {
            return;
        }
        for (Long cid : responsableIds) {
            if (cid == null) {
                continue;
            }
            Compte c = compteRepository.findById(cid)
                    .orElseThrow(() -> new RuntimeException("Compte responsable non trouvé avec l'id: " + cid));
            compte.getResponsables().add(c);
        }
    }

    private boolean sameResponsableIds(CompteBancaire existing, List<Long> incoming) {
        if (incoming == null) {
            return true;
        }
        Set<Long> cur = new HashSet<>();
        if (existing.getResponsables() != null) {
            for (Compte r : existing.getResponsables()) {
                if (r.getId() != null) {
                    cur.add(r.getId());
                }
            }
        }
        Set<Long> inc = incoming.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        return cur.equals(inc);
    }

    @Override
    public List<CompteBancaireDTO> findAll() {
        List<CompteBancaire> list = compteBancaireRepository.findAll();
        if (!financeEntityAccessService.isCurrentUserAdmin()) {
            Optional<Long> uid = financeEntityAccessService.getCurrentCompteId();
            if (uid.isPresent()) {
                Long u = uid.get();
                list = list.stream()
                        .filter(c -> c.getResponsables() == null || c.getResponsables().isEmpty()
                                || c.getResponsables().stream().anyMatch(r -> r.getId() != null && r.getId().equals(u)))
                        .collect(Collectors.toList());
            }
        }
        return list.stream()
            .map(compteBancaireMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<CompteBancaireDTO> findById(Long id) {
        Optional<CompteBancaire> opt = compteBancaireRepository.findById(id);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        financeEntityAccessService.assertCanManageCompteBancaire(opt.get());
        return Optional.of(compteBancaireMapper.toDTO(opt.get()));
    }

    @Override
    public Optional<CompteBancaireDTO> findByNumero(String numero) {
        return compteBancaireRepository.findByNumero(numero)
            .map(compteBancaireMapper::toDTO);
    }

    @Override
    public CompteBancaireDTO save(CompteBancaireDTO compteDTO) {
        financeEntityAccessService.validateResponsablesComptablesActifs(compteDTO.getResponsableIds());
        CompteBancaire compte = compteBancaireMapper.toEntity(compteDTO);
        applyResponsables(compte, compteDTO.getResponsableIds());
        CompteBancaire savedCompte = compteBancaireRepository.save(compte);
        return compteBancaireMapper.toDTO(savedCompte);
    }

    @Override
    public CompteBancaireDTO update(Long id, CompteBancaireDTO compteDTO) {
        CompteBancaire existingCompte = compteBancaireRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé avec l'id: " + id));
        boolean admin = financeEntityAccessService.isCurrentUserAdmin();
        if (!admin) {
            financeEntityAccessService.assertCanManageCompteBancaire(existingCompte);
            if (compteDTO.getResponsableIds() != null && !sameResponsableIds(existingCompte, compteDTO.getResponsableIds())) {
                throw new RuntimeException("Seul un administrateur peut modifier les responsables de ce compte bancaire.");
            }
        } else if (compteDTO.getResponsableIds() != null) {
            financeEntityAccessService.validateResponsablesComptablesActifs(compteDTO.getResponsableIds());
        }
        CompteBancaire compte = compteBancaireMapper.toEntity(compteDTO);
        compte.setId(existingCompte.getId());
        if (compteDTO.getResponsableIds() != null) {
            applyResponsables(compte, compteDTO.getResponsableIds());
        } else {
            compte.setResponsables(new HashSet<>(existingCompte.getResponsables()));
        }
        CompteBancaire updatedCompte = compteBancaireRepository.save(compte);
        return compteBancaireMapper.toDTO(updatedCompte);
    }

    @Override
    public void deleteById(Long id) {
        CompteBancaire existing = compteBancaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé avec l'id: " + id));
        financeEntityAccessService.assertCanManageCompteBancaire(existing);
        compteBancaireRepository.deleteById(id);
    }

    @Override
    public BanqueCaisseStatsDTO getStats() {
        List<CompteBancaire> allComptes = compteBancaireRepository.findAll();

        // Solde total = somme des soldes de tous les comptes bancaires (aligné avec la grille)
        BigDecimal soldeTotal = allComptes.stream()
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

        // Caisses : entité Caisse uniquement (même périmètre que la liste affichée côté UI)
        List<Caisse> toutesLesCaisses = caisseRepository.findAll();

        BigDecimal soldeCaisse = toutesLesCaisses.stream()
            .map(Caisse::getSolde)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal entreesCaisse = BigDecimal.ZERO;
        BigDecimal sortiesCaisse = BigDecimal.ZERO;
        for (Caisse caisse : toutesLesCaisses) {
            List<Transaction> transactionsCaisseValidees = transactionRepository.findByCaisse(caisse).stream()
                .filter(t -> t.getStatut() == Transaction.StatutTransaction.VALIDE)
                .toList();
            entreesCaisse = entreesCaisse.add(transactionsCaisseValidees.stream()
                .filter(t -> t.getType() == Transaction.TypeTransaction.DEPOT)
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
            sortiesCaisse = sortiesCaisse.add(transactionsCaisseValidees.stream()
                .filter(t -> t.getType() == Transaction.TypeTransaction.RETRAIT)
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
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

