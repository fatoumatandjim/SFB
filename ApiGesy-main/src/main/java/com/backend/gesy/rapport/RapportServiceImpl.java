package com.backend.gesy.rapport;

import com.backend.gesy.depense.Depense;
import com.backend.gesy.depense.DepenseRepository;
import com.backend.gesy.facture.Facture;
import com.backend.gesy.facture.FactureRepository;
import com.backend.gesy.manquant.Manquant;
import com.backend.gesy.manquant.ManquantRepository;
import com.backend.gesy.rapport.dto.RapportFinancierDTO;
import com.backend.gesy.transaction.Transaction;
import com.backend.gesy.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RapportServiceImpl implements RapportService {
    private final FactureRepository factureRepository;
    private final TransactionRepository transactionRepository;
    private final DepenseRepository depenseRepository;
    private final ManquantRepository manquantRepository;

    @Override
    public RapportFinancierDTO getRapportFinancier(String periode, Integer annee, LocalDate dateDebut, LocalDate dateFin) {
        // Déterminer les dates de début et fin selon la période
        LocalDate startDate;
        LocalDate endDate;
        LocalDate today = LocalDate.now();
        
        if (periode != null && periode.equals("personnalise") && dateDebut != null && dateFin != null) {
            startDate = dateDebut;
            endDate = dateFin;
        } else if (periode != null && periode.equals("mois")) {
            startDate = today.withDayOfMonth(1);
            endDate = today;
        } else if (periode != null && periode.equals("trimestre")) {
            int quarter = (today.getMonthValue() - 1) / 3;
            startDate = today.withMonth(quarter * 3 + 1).withDayOfMonth(1);
            endDate = today;
        } else if (periode != null && periode.equals("annee")) {
            if (annee != null) {
                startDate = LocalDate.of(annee, 1, 1);
                endDate = LocalDate.of(annee, 12, 31);
            } else {
                startDate = today.withDayOfYear(1);
                endDate = today;
            }
        } else {
            // Par défaut, ce mois
            startDate = today.withDayOfMonth(1);
            endDate = today;
        }

        // Calculer les stats pour la période actuelle
        RapportFinancierDTO.StatsFinancieres stats = calculerStatsFinancieres(startDate, endDate);
        
        // Calculer les stats pour la période précédente (pour l'évolution)
        LocalDate previousStartDate;
        LocalDate previousEndDate;
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        
        if (periode != null && periode.equals("mois")) {
            previousStartDate = startDate.minusMonths(1);
            previousEndDate = startDate.minusDays(1);
        } else if (periode != null && periode.equals("trimestre")) {
            previousStartDate = startDate.minusMonths(3);
            previousEndDate = startDate.minusDays(1);
        } else if (periode != null && periode.equals("annee")) {
            previousStartDate = startDate.minusYears(1);
            previousEndDate = startDate.minusDays(1);
        } else {
            previousStartDate = startDate.minusDays(daysBetween + 1);
            previousEndDate = startDate.minusDays(1);
        }
        
        RapportFinancierDTO.StatsFinancieres previousStats = calculerStatsFinancieres(previousStartDate, previousEndDate);
        
        // Calculer les évolutions
        calculerEvolutions(stats, previousStats);
        
        // Déterminer la période pour l'affichage
        String periodeLabel = getPeriodeLabel(periode, startDate, endDate);
        stats.getChiffreAffaires().setPeriode(periodeLabel);
        stats.getDepenses().setPeriode(periodeLabel);
        stats.getBenefice().setPeriode(periodeLabel);

        // Calculer les données mensuelles (6 derniers mois)
        List<RapportFinancierDTO.DonneeMensuelleDTO> donneesMensuelles = calculerDonneesMensuelles(6);

        // Calculer les catégories de dépenses
        List<RapportFinancierDTO.CategorieDepenseDTO> categoriesDepenses = calculerCategoriesDepenses(startDate, endDate);

        // Calculer les frais douaniers
        RapportFinancierDTO.FraisDouaniers fraisDouaniers = calculerFraisDouaniers(startDate, endDate, periode, previousStartDate, previousEndDate);

        // Calculer les pertes (manquants)
        RapportFinancierDTO.Pertes pertes = calculerPertes(startDate, endDate, periode, previousStartDate, previousEndDate);

        RapportFinancierDTO rapport = new RapportFinancierDTO();
        rapport.setStats(stats);
        rapport.setDonneesMensuelles(donneesMensuelles);
        rapport.setCategoriesDepenses(categoriesDepenses);
        rapport.setFraisDouaniers(fraisDouaniers);
        rapport.setPertes(pertes);

        return rapport;
    }

    private RapportFinancierDTO.StatsFinancieres calculerStatsFinancieres(LocalDate startDate, LocalDate endDate) {
        // Chiffre d'affaires : somme des montants TTC des factures payées ou partiellement payées
        List<Facture> factures = factureRepository.findAll().stream()
            .filter(f -> f.getDate() != null && 
                        !f.getDate().isBefore(startDate) && 
                        !f.getDate().isAfter(endDate) &&
                        (f.getStatut() == Facture.StatutFacture.PAYEE || 
                         f.getStatut() == Facture.StatutFacture.PARTIELLEMENT_PAYEE))
            .toList();

        BigDecimal chiffreAffaires = factures.stream()
            .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : f.getMontant())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Dépenses : somme des transactions de type dépense avec statut VALIDE + dépenses (Depense)
        List<Transaction> transactions = transactionRepository.findAll().stream()
            .filter(t -> t.getDate() != null &&
                        !t.getDate().toLocalDate().isBefore(startDate) &&
                        !t.getDate().toLocalDate().isAfter(endDate) &&
                        t.getStatut() == Transaction.StatutTransaction.VALIDE &&
                        isDepense(t.getType()))
            .toList();

        BigDecimal depensesTransactions = transactions.stream()
            .map(Transaction::getMontant)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ajouter les dépenses (Depense)
        List<Depense> depensesList = depenseRepository.findAll().stream()
            .filter(d -> d.getDateDepense() != null &&
                        !d.getDateDepense().toLocalDate().isBefore(startDate) &&
                        !d.getDateDepense().toLocalDate().isAfter(endDate))
            .toList();

        BigDecimal depensesMontant = depensesList.stream()
            .map(Depense::getMontant)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal depenses = depensesTransactions.add(depensesMontant);

        // Bénéfice
        BigDecimal benefice = chiffreAffaires.subtract(depenses);

        // Marge bénéficiaire
        BigDecimal marge = BigDecimal.ZERO;
        if (chiffreAffaires.compareTo(BigDecimal.ZERO) > 0) {
            marge = benefice.divide(chiffreAffaires, 4, RoundingMode.HALF_UP)
                           .multiply(BigDecimal.valueOf(100));
        }

        RapportFinancierDTO.StatsFinancieres stats = new RapportFinancierDTO.StatsFinancieres();
        
        RapportFinancierDTO.ChiffreAffaires ca = new RapportFinancierDTO.ChiffreAffaires();
        ca.setTotal(chiffreAffaires);
        stats.setChiffreAffaires(ca);

        RapportFinancierDTO.Depenses dep = new RapportFinancierDTO.Depenses();
        dep.setTotal(depenses);
        stats.setDepenses(dep);

        RapportFinancierDTO.Benefice ben = new RapportFinancierDTO.Benefice();
        ben.setTotal(benefice);
        stats.setBenefice(ben);

        RapportFinancierDTO.Marge marg = new RapportFinancierDTO.Marge();
        marg.setPourcentage(marge);
        stats.setMarge(marg);

        return stats;
    }

    private void calculerEvolutions(RapportFinancierDTO.StatsFinancieres current, RapportFinancierDTO.StatsFinancieres previous) {
        // Évolution du chiffre d'affaires
        String evolutionCA = calculerEvolution(current.getChiffreAffaires().getTotal(), previous.getChiffreAffaires().getTotal());
        current.getChiffreAffaires().setEvolution(evolutionCA);

        // Évolution des dépenses
        String evolutionDep = calculerEvolution(current.getDepenses().getTotal(), previous.getDepenses().getTotal());
        current.getDepenses().setEvolution(evolutionDep);

        // Évolution du bénéfice
        String evolutionBen = calculerEvolution(current.getBenefice().getTotal(), previous.getBenefice().getTotal());
        current.getBenefice().setEvolution(evolutionBen);

        // Évolution de la marge
        String evolutionMarge = calculerEvolution(current.getMarge().getPourcentage(), previous.getMarge().getPourcentage());
        current.getMarge().setEvolution(evolutionMarge);
    }

    private String calculerEvolution(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current.compareTo(BigDecimal.ZERO) > 0) {
                return "+100%";
            }
            return "0%";
        }
        
        BigDecimal difference = current.subtract(previous);
        BigDecimal pourcentage = difference.divide(previous, 4, RoundingMode.HALF_UP)
                                           .multiply(BigDecimal.valueOf(100));
        
        String sign = pourcentage.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + pourcentage.setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private List<RapportFinancierDTO.DonneeMensuelleDTO> calculerDonneesMensuelles(int nombreMois) {
        List<RapportFinancierDTO.DonneeMensuelleDTO> donnees = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = nombreMois - 1; i >= 0; i--) {
            LocalDate moisDate = today.minusMonths(i);
            LocalDate startOfMonth = moisDate.withDayOfMonth(1);
            final LocalDate endOfMonth;
            
            // Si c'est le mois actuel, limiter à aujourd'hui
            if (i == 0) {
                endOfMonth = today;
            } else {
                endOfMonth = moisDate.withDayOfMonth(moisDate.lengthOfMonth());
            }

            // Chiffre d'affaires du mois
            List<Facture> facturesMois = factureRepository.findAll().stream()
                .filter(f -> f.getDate() != null &&
                            !f.getDate().isBefore(startOfMonth) &&
                            !f.getDate().isAfter(endOfMonth) &&
                            (f.getStatut() == Facture.StatutFacture.PAYEE ||
                             f.getStatut() == Facture.StatutFacture.PARTIELLEMENT_PAYEE))
                .toList();

            BigDecimal caMois = facturesMois.stream()
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : f.getMontant())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Dépenses du mois : transactions + dépenses (Depense)
            List<Transaction> transactionsMois = transactionRepository.findAll().stream()
                .filter(t -> t.getDate() != null &&
                            !t.getDate().toLocalDate().isBefore(startOfMonth) &&
                            !t.getDate().toLocalDate().isAfter(endOfMonth) &&
                            t.getStatut() == Transaction.StatutTransaction.VALIDE &&
                            isDepense(t.getType()))
                .toList();

            BigDecimal depensesTransactionsMois = transactionsMois.stream()
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Ajouter les dépenses (Depense) du mois
            List<Depense> depensesListMois = depenseRepository.findAll().stream()
                .filter(d -> d.getDateDepense() != null &&
                            !d.getDateDepense().toLocalDate().isBefore(startOfMonth) &&
                            !d.getDateDepense().toLocalDate().isAfter(endOfMonth))
                .toList();

            BigDecimal depensesMontantMois = depensesListMois.stream()
                .map(Depense::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal depensesMois = depensesTransactionsMois.add(depensesMontantMois);

            BigDecimal beneficeMois = caMois.subtract(depensesMois);

            RapportFinancierDTO.DonneeMensuelleDTO donnee = new RapportFinancierDTO.DonneeMensuelleDTO();
            donnee.setMois(moisDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH));
            donnee.setChiffreAffaires(caMois);
            donnee.setDepenses(depensesMois);
            donnee.setBenefice(beneficeMois);
            
            donnees.add(donnee);
        }

        return donnees;
    }

    private List<RapportFinancierDTO.CategorieDepenseDTO> calculerCategoriesDepenses(LocalDate startDate, LocalDate endDate) {
        // Récupérer toutes les transactions de dépense
        List<Transaction> transactions = transactionRepository.findAll().stream()
            .filter(t -> t.getDate() != null &&
                        !t.getDate().toLocalDate().isBefore(startDate) &&
                        !t.getDate().toLocalDate().isAfter(endDate) &&
                        t.getStatut() == Transaction.StatutTransaction.VALIDE &&
                        isDepense(t.getType()))
            .collect(Collectors.toList());

        // Grouper par type de transaction
        Map<Transaction.TypeTransaction, BigDecimal> depensesParType = transactions.stream()
            .collect(Collectors.groupingBy(
                Transaction::getType,
                Collectors.reducing(BigDecimal.ZERO, Transaction::getMontant, BigDecimal::add)
            ));

        // Calculer le total des dépenses
        BigDecimal totalDepenses = depensesParType.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Créer les DTOs de catégories
        List<RapportFinancierDTO.CategorieDepenseDTO> categories = new ArrayList<>();
        
        Map<Transaction.TypeTransaction, String> nomsCategories = new HashMap<>();
        nomsCategories.put(Transaction.TypeTransaction.FRAIS_LOCATION, "Location");
        nomsCategories.put(Transaction.TypeTransaction.FRAIS_FRONTIERE, "Frontière");
        nomsCategories.put(Transaction.TypeTransaction.SALAIRE, "Salaires");
        nomsCategories.put(Transaction.TypeTransaction.FRAIS, "Frais");
        nomsCategories.put(Transaction.TypeTransaction.TS_FRAIS_PRESTATIONS, "Prestations");
        nomsCategories.put(Transaction.TypeTransaction.FRAIS_REPERTOIRE, "Répertoire");
        nomsCategories.put(Transaction.TypeTransaction.FRAIS_CHAMBRE_COMMERCE, "Chambre Commerce");
        nomsCategories.put(Transaction.TypeTransaction.INTERET, "Intérêts");

        Map<Transaction.TypeTransaction, String> couleurs = new HashMap<>();
        couleurs.put(Transaction.TypeTransaction.FRAIS_LOCATION, "blue");
        couleurs.put(Transaction.TypeTransaction.FRAIS_FRONTIERE, "orange");
        couleurs.put(Transaction.TypeTransaction.SALAIRE, "green");
        couleurs.put(Transaction.TypeTransaction.FRAIS, "purple");
        couleurs.put(Transaction.TypeTransaction.TS_FRAIS_PRESTATIONS, "red");
        couleurs.put(Transaction.TypeTransaction.FRAIS_REPERTOIRE, "blue");
        couleurs.put(Transaction.TypeTransaction.FRAIS_CHAMBRE_COMMERCE, "purple");
        couleurs.put(Transaction.TypeTransaction.INTERET, "red");

        for (Map.Entry<Transaction.TypeTransaction, BigDecimal> entry : depensesParType.entrySet()) {
            RapportFinancierDTO.CategorieDepenseDTO categorie = new RapportFinancierDTO.CategorieDepenseDTO();
            categorie.setNom(nomsCategories.getOrDefault(entry.getKey(), entry.getKey().name()));
            categorie.setMontant(entry.getValue());
            
            BigDecimal pourcentage = BigDecimal.ZERO;
            if (totalDepenses.compareTo(BigDecimal.ZERO) > 0) {
                pourcentage = entry.getValue().divide(totalDepenses, 4, RoundingMode.HALF_UP)
                                   .multiply(BigDecimal.valueOf(100));
            }
            categorie.setPourcentage(pourcentage.setScale(1, RoundingMode.HALF_UP));
            categorie.setCouleur(couleurs.getOrDefault(entry.getKey(), "blue"));
            
            categories.add(categorie);
        }

        // Trier par montant décroissant
        categories.sort((a, b) -> b.getMontant().compareTo(a.getMontant()));

        return categories;
    }

    private boolean isDepense(Transaction.TypeTransaction type) {
        return type == Transaction.TypeTransaction.FRAIS ||
               type == Transaction.TypeTransaction.FRAIS_LOCATION ||
               type == Transaction.TypeTransaction.FRAIS_FRONTIERE ||
               type == Transaction.TypeTransaction.SALAIRE ||
               type == Transaction.TypeTransaction.TS_FRAIS_PRESTATIONS ||
               type == Transaction.TypeTransaction.FRAIS_REPERTOIRE ||
               type == Transaction.TypeTransaction.FRAIS_CHAMBRE_COMMERCE ||
               type == Transaction.TypeTransaction.INTERET;
               // Note: FRAIS_DOUANE et FRAIS_T1 sont traités séparément dans fraisDouaniers
    }

    private RapportFinancierDTO.FraisDouaniers calculerFraisDouaniers(LocalDate startDate, LocalDate endDate, String periode, LocalDate previousStartDate, LocalDate previousEndDate) {
        // Récupérer les transactions de type FRAIS_DOUANE et FRAIS_T1
        List<Transaction> fraisDouane = transactionRepository.findAll().stream()
            .filter(t -> t.getDate() != null &&
                        !t.getDate().toLocalDate().isBefore(startDate) &&
                        !t.getDate().toLocalDate().isAfter(endDate) &&
                        t.getStatut() == Transaction.StatutTransaction.VALIDE &&
                        t.getType() == Transaction.TypeTransaction.FRAIS_DOUANE)
            .toList();

        List<Transaction> fraisT1 = transactionRepository.findAll().stream()
            .filter(t -> t.getDate() != null &&
                        !t.getDate().toLocalDate().isBefore(startDate) &&
                        !t.getDate().toLocalDate().isAfter(endDate) &&
                        t.getStatut() == Transaction.StatutTransaction.VALIDE &&
                        t.getType() == Transaction.TypeTransaction.FRAIS_T1)
            .toList();

        BigDecimal totalFraisDouane = fraisDouane.stream()
            .map(Transaction::getMontant)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFraisT1 = fraisT1.stream()
            .map(Transaction::getMontant)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = totalFraisDouane.add(totalFraisT1);

        // Calculer l'évolution
        List<Transaction> previousFraisDouane = transactionRepository.findAll().stream()
            .filter(t -> t.getDate() != null &&
                        !t.getDate().toLocalDate().isBefore(previousStartDate) &&
                        !t.getDate().toLocalDate().isAfter(previousEndDate) &&
                        t.getStatut() == Transaction.StatutTransaction.VALIDE &&
                        (t.getType() == Transaction.TypeTransaction.FRAIS_DOUANE || t.getType() == Transaction.TypeTransaction.FRAIS_T1))
            .toList();

        BigDecimal previousTotal = previousFraisDouane.stream()
            .map(Transaction::getMontant)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        String evolution = calculerEvolution(total, previousTotal);

        RapportFinancierDTO.FraisDouaniers fraisDouaniers = new RapportFinancierDTO.FraisDouaniers();
        fraisDouaniers.setTotal(total);
        fraisDouaniers.setFraisDouane(totalFraisDouane);
        fraisDouaniers.setFraisT1(totalFraisT1);
        fraisDouaniers.setEvolution(evolution);

        return fraisDouaniers;
    }

    private RapportFinancierDTO.Pertes calculerPertes(LocalDate startDate, LocalDate endDate, String periode, LocalDate previousStartDate, LocalDate previousEndDate) {
        // Récupérer les manquants dans la période
        List<Manquant> manquants = manquantRepository.findAll().stream()
            .filter(m -> m.getDateCreation() != null &&
                        !m.getDateCreation().toLocalDate().isBefore(startDate) &&
                        !m.getDateCreation().toLocalDate().isAfter(endDate))
            .toList();

        // Calculer la quantité totale
        BigDecimal quantiteTotale = manquants.stream()
            .map(m -> m.getQuantite() != null ? m.getQuantite() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Pour le total, on utilise la quantité totale (en litres)

        // Calculer l'évolution
        List<Manquant> previousManquants = manquantRepository.findAll().stream()
            .filter(m -> m.getDateCreation() != null &&
                        !m.getDateCreation().toLocalDate().isBefore(previousStartDate) &&
                        !m.getDateCreation().toLocalDate().isAfter(previousEndDate))
            .toList();

        BigDecimal previousQuantite = previousManquants.stream()
            .map(m -> m.getQuantite() != null ? m.getQuantite() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        String evolution = calculerEvolution(quantiteTotale, previousQuantite);

        RapportFinancierDTO.Pertes pertes = new RapportFinancierDTO.Pertes();
        pertes.setTotal(quantiteTotale);
        pertes.setQuantiteTotale(quantiteTotale);
        pertes.setEvolution(evolution);

        return pertes;
    }

    private String getPeriodeLabel(String periode, LocalDate startDate, LocalDate endDate) {
        if (periode == null || periode.equals("mois")) {
            return "Ce mois";
        } else if (periode.equals("trimestre")) {
            return "Ce trimestre";
        } else if (periode.equals("annee")) {
            return "Cette année";
        } else if (periode.equals("personnalise")) {
            return startDate.toString() + " - " + endDate.toString();
        }
        return "Période sélectionnée";
    }
}

