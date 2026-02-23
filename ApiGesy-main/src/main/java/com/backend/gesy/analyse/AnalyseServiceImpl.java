package com.backend.gesy.analyse;

import com.backend.gesy.analyse.dto.AnalyseDTO;
import com.backend.gesy.camion.Camion;
import com.backend.gesy.camion.CamionRepository;
import com.backend.gesy.client.Client;
import com.backend.gesy.depense.Depense;
import com.backend.gesy.depense.DepenseRepository;
import com.backend.gesy.facture.Facture;
import com.backend.gesy.facture.FactureRepository;
import com.backend.gesy.manquant.Manquant;
import com.backend.gesy.manquant.ManquantRepository;
import com.backend.gesy.transaction.Transaction;
import com.backend.gesy.transaction.TransactionRepository;
import com.backend.gesy.voyage.Voyage;
import com.backend.gesy.voyage.VoyageRepository;
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
public class AnalyseServiceImpl implements AnalyseService {
    private final FactureRepository factureRepository;
    private final VoyageRepository voyageRepository;
    private final CamionRepository camionRepository;
    private final DepenseRepository depenseRepository;
    private final ManquantRepository manquantRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public AnalyseDTO getAnalyse(String periode, Integer annee, LocalDate dateDebut, LocalDate dateFin) {
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

        // Calculer les KPIs
        AnalyseDTO.KPIs kpis = calculerKPIs(startDate, endDate);
        
        // Calculer les données hebdomadaires (4 dernières semaines)
        List<AnalyseDTO.DonneeHebdomadaireDTO> donneesHebdomadaires = calculerDonneesHebdomadaires(4);
        
        // Calculer les tendances
        List<AnalyseDTO.TendanceDTO> tendances = calculerTendances(startDate, endDate);
        
        // Calculer les performances
        List<AnalyseDTO.PerformanceDTO> performances = calculerPerformances();

        AnalyseDTO analyse = new AnalyseDTO();
        analyse.setKpis(kpis);
        analyse.setDonneesHebdomadaires(donneesHebdomadaires);
        analyse.setTendances(tendances);
        analyse.setPerformances(performances);

        return analyse;
    }

    private AnalyseDTO.KPIs calculerKPIs(LocalDate startDate, LocalDate endDate) {
        // Période précédente pour les évolutions
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        LocalDate previousStartDate = startDate.minusDays(daysBetween + 1);
        LocalDate previousEndDate = startDate.minusDays(1);

        // Croissance : taux de croissance des ventes (factures)
        List<Facture> facturesActuelles = factureRepository.findAll().stream()
            .filter(f -> f.getDate() != null &&
                        !f.getDate().isBefore(startDate) &&
                        !f.getDate().isAfter(endDate) &&
                        (f.getStatut() == Facture.StatutFacture.PAYEE ||
                         f.getStatut() == Facture.StatutFacture.PARTIELLEMENT_PAYEE))
            .toList();

        BigDecimal ventesActuelles = facturesActuelles.stream()
            .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : f.getMontant())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Facture> facturesPrecedentes = factureRepository.findAll().stream()
            .filter(f -> f.getDate() != null &&
                        !f.getDate().isBefore(previousStartDate) &&
                        !f.getDate().isAfter(previousEndDate) &&
                        (f.getStatut() == Facture.StatutFacture.PAYEE ||
                         f.getStatut() == Facture.StatutFacture.PARTIELLEMENT_PAYEE))
            .toList();

        BigDecimal ventesPrecedentes = facturesPrecedentes.stream()
            .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : f.getMontant())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal croissance = BigDecimal.ZERO;
        if (ventesPrecedentes.compareTo(BigDecimal.ZERO) > 0) {
            croissance = ventesActuelles.subtract(ventesPrecedentes)
                .divide(ventesPrecedentes, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        } else if (ventesActuelles.compareTo(BigDecimal.ZERO) > 0) {
            croissance = BigDecimal.valueOf(100);
        }

        String evolutionCroissance = calculerEvolution(ventesActuelles, ventesPrecedentes);

        // Efficacité : taux de voyages livrés vs total (hors cession)
        List<Voyage> voyagesActuels = voyageRepository.findAll().stream()
            .filter(v -> !v.isCession())
            .filter(v -> v.getDateDepart() != null &&
                        !v.getDateDepart().toLocalDate().isBefore(startDate) &&
                        !v.getDateDepart().toLocalDate().isAfter(endDate))
            .toList();

        long voyagesTotal = voyagesActuels.size();
        long voyagesLivre = voyagesActuels.stream()
            .filter(v -> v.getStatut() == Voyage.StatutVoyage.DECHARGER)
            .count();

        BigDecimal efficacite = BigDecimal.ZERO;
        if (voyagesTotal > 0) {
            efficacite = BigDecimal.valueOf(voyagesLivre)
                .divide(BigDecimal.valueOf(voyagesTotal), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        List<Voyage> voyagesPrecedents = voyageRepository.findAll().stream()
            .filter(v -> !v.isCession())
            .filter(v -> v.getDateDepart() != null &&
                        !v.getDateDepart().toLocalDate().isBefore(previousStartDate) &&
                        !v.getDateDepart().toLocalDate().isAfter(previousEndDate))
            .toList();

        long voyagesTotalPrecedent = voyagesPrecedents.size();
        long voyagesLivrePrecedent = voyagesPrecedents.stream()
            .filter(v -> v.getStatut() == Voyage.StatutVoyage.DECHARGER)
            .count();

        BigDecimal efficacitePrecedente = BigDecimal.ZERO;
        if (voyagesTotalPrecedent > 0) {
            efficacitePrecedente = BigDecimal.valueOf(voyagesLivrePrecedent)
                .divide(BigDecimal.valueOf(voyagesTotalPrecedent), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        String evolutionEfficacite = calculerEvolution(efficacite, efficacitePrecedente);

        // Satisfaction : taux de factures payées à temps (dans les délais)
        long facturesPayeesATemps = facturesActuelles.stream()
            .filter(f -> f.getDateEcheance() != null &&
                        f.getStatut() == Facture.StatutFacture.PAYEE &&
                        (f.getDateEcheance().isAfter(f.getDate()) || 
                         f.getDateEcheance().equals(f.getDate())))
            .count();

        BigDecimal satisfaction = BigDecimal.ZERO;
        if (facturesActuelles.size() > 0) {
            satisfaction = BigDecimal.valueOf(facturesPayeesATemps)
                .divide(BigDecimal.valueOf(facturesActuelles.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        long facturesPayeesATempsPrecedent = facturesPrecedentes.stream()
            .filter(f -> f.getDateEcheance() != null &&
                        f.getStatut() == Facture.StatutFacture.PAYEE &&
                        (f.getDateEcheance().isAfter(f.getDate()) || 
                         f.getDateEcheance().equals(f.getDate())))
            .count();

        BigDecimal satisfactionPrecedente = BigDecimal.ZERO;
        if (facturesPrecedentes.size() > 0) {
            satisfactionPrecedente = BigDecimal.valueOf(facturesPayeesATempsPrecedent)
                .divide(BigDecimal.valueOf(facturesPrecedentes.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        String evolutionSatisfaction = calculerEvolution(satisfaction, satisfactionPrecedente);

        // Rentabilité : marge bénéficiaire (déjà calculée dans rapport, on réutilise la logique)
        // Calculer les dépenses (transactions de type dépense)
        BigDecimal depenses = BigDecimal.ZERO; // Simplifié, on pourrait utiliser TransactionRepository
        BigDecimal rentabilite = BigDecimal.ZERO;
        if (ventesActuelles.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal benefice = ventesActuelles.subtract(depenses);
            rentabilite = benefice.divide(ventesActuelles, 4, RoundingMode.HALF_UP)
                                 .multiply(BigDecimal.valueOf(100));
        }

        BigDecimal depensesPrecedentes = BigDecimal.ZERO;
        BigDecimal rentabilitePrecedente = BigDecimal.ZERO;
        if (ventesPrecedentes.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal beneficePrecedent = ventesPrecedentes.subtract(depensesPrecedentes);
            rentabilitePrecedente = beneficePrecedent.divide(ventesPrecedentes, 4, RoundingMode.HALF_UP)
                                                    .multiply(BigDecimal.valueOf(100));
        }

        String evolutionRentabilite = calculerEvolution(rentabilite, rentabilitePrecedente);

        AnalyseDTO.KPIs kpis = new AnalyseDTO.KPIs();
        
        AnalyseDTO.Croissance croiss = new AnalyseDTO.Croissance();
        croiss.setValeur(croissance.setScale(1, RoundingMode.HALF_UP));
        croiss.setEvolution(evolutionCroissance);
        kpis.setCroissance(croiss);

        AnalyseDTO.Efficacite eff = new AnalyseDTO.Efficacite();
        eff.setValeur(efficacite.setScale(1, RoundingMode.HALF_UP));
        eff.setEvolution(evolutionEfficacite);
        kpis.setEfficacite(eff);

        AnalyseDTO.Satisfaction sat = new AnalyseDTO.Satisfaction();
        sat.setValeur(satisfaction.setScale(1, RoundingMode.HALF_UP));
        sat.setEvolution(evolutionSatisfaction);
        kpis.setSatisfaction(sat);

        AnalyseDTO.Rentabilite rent = new AnalyseDTO.Rentabilite();
        rent.setValeur(rentabilite.setScale(1, RoundingMode.HALF_UP));
        rent.setEvolution(evolutionRentabilite);
        kpis.setRentabilite(rent);

        return kpis;
    }

    private List<AnalyseDTO.DonneeHebdomadaireDTO> calculerDonneesHebdomadaires(int nombreSemaines) {
        List<AnalyseDTO.DonneeHebdomadaireDTO> donnees = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = nombreSemaines - 1; i >= 0; i--) {
            LocalDate semaineDate = today.minusWeeks(i);
            LocalDate startOfWeek = semaineDate.minusDays(semaineDate.getDayOfWeek().getValue() - 1);
            final LocalDate endOfWeek;
            
            if (i == 0) {
                endOfWeek = today;
            } else {
                endOfWeek = startOfWeek.plusDays(6);
            }

            // Ventes de la semaine (factures)
            List<Facture> facturesSemaine = factureRepository.findAll().stream()
                .filter(f -> f.getDate() != null &&
                            !f.getDate().isBefore(startOfWeek) &&
                            !f.getDate().isAfter(endOfWeek) &&
                            (f.getStatut() == Facture.StatutFacture.PAYEE ||
                             f.getStatut() == Facture.StatutFacture.PARTIELLEMENT_PAYEE))
                .toList();

            BigDecimal ventesSemaine = facturesSemaine.stream()
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : f.getMontant())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Clients actifs (clients uniques avec factures)
            Set<Long> clientsUniques = facturesSemaine.stream()
                .map(f -> f.getClient() != null ? f.getClient().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            // Camions actifs (camions en route ou assignés, hors cession)
            List<Voyage> voyagesSemaine = voyageRepository.findAll().stream()
                .filter(v -> !v.isCession())
                .filter(v -> v.getDateDepart() != null &&
                            !v.getDateDepart().toLocalDate().isBefore(startOfWeek) &&
                            !v.getDateDepart().toLocalDate().isAfter(endOfWeek))
                .toList();

            Set<Long> camionsActifs = voyagesSemaine.stream()
                .map(v -> v.getCamion() != null ? v.getCamion().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            AnalyseDTO.DonneeHebdomadaireDTO donnee = new AnalyseDTO.DonneeHebdomadaireDTO();
            donnee.setSemaine("Sem " + (nombreSemaines - i));
            donnee.setVentes(ventesSemaine);
            donnee.setClients(clientsUniques.size());
            donnee.setCamions(camionsActifs.size());
            
            donnees.add(donnee);
        }

        return donnees;
    }

    private List<AnalyseDTO.TendanceDTO> calculerTendances(LocalDate startDate, LocalDate endDate) {
        List<AnalyseDTO.TendanceDTO> tendances = new ArrayList<>();
        
        // Période précédente
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        LocalDate previousStartDate = startDate.minusDays(daysBetween + 1);
        LocalDate previousEndDate = startDate.minusDays(1);

        // Tendance Ventes
        List<Facture> facturesActuelles = factureRepository.findAll().stream()
            .filter(f -> f.getDate() != null &&
                        !f.getDate().isBefore(startDate) &&
                        !f.getDate().isAfter(endDate) &&
                        (f.getStatut() == Facture.StatutFacture.PAYEE ||
                         f.getStatut() == Facture.StatutFacture.PARTIELLEMENT_PAYEE))
            .toList();

        BigDecimal ventesActuelles = facturesActuelles.stream()
            .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : f.getMontant())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Facture> facturesPrecedentes = factureRepository.findAll().stream()
            .filter(f -> f.getDate() != null &&
                        !f.getDate().isBefore(previousStartDate) &&
                        !f.getDate().isAfter(previousEndDate) &&
                        (f.getStatut() == Facture.StatutFacture.PAYEE ||
                         f.getStatut() == Facture.StatutFacture.PARTIELLEMENT_PAYEE))
            .toList();

        BigDecimal ventesPrecedentes = facturesPrecedentes.stream()
            .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : f.getMontant())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal evolutionVentes = calculerEvolutionPourcentage(ventesActuelles, ventesPrecedentes);
        String tendanceVentes = determinerTendance(evolutionVentes);
        
        AnalyseDTO.TendanceDTO tendanceV = new AnalyseDTO.TendanceDTO();
        tendanceV.setCategorie("Ventes");
        tendanceV.setEvolution(evolutionVentes);
        tendanceV.setTendance(tendanceVentes);
        tendanceV.setCouleur(tendanceVentes.equals("hausse") ? "green" : tendanceVentes.equals("baisse") ? "red" : "gray");
        tendances.add(tendanceV);

        // Tendance Clients actifs
        Set<Long> clientsActuels = facturesActuelles.stream()
            .map(f -> f.getClient() != null ? f.getClient().getId() : null)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Set<Long> clientsPrecedents = facturesPrecedentes.stream()
            .map(f -> f.getClient() != null ? f.getClient().getId() : null)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        BigDecimal evolutionClients = calculerEvolutionPourcentage(
            BigDecimal.valueOf(clientsActuels.size()),
            BigDecimal.valueOf(clientsPrecedents.size())
        );
        String tendanceClients = determinerTendance(evolutionClients);
        
        AnalyseDTO.TendanceDTO tendanceC = new AnalyseDTO.TendanceDTO();
        tendanceC.setCategorie("Clients actifs");
        tendanceC.setEvolution(evolutionClients);
        tendanceC.setTendance(tendanceClients);
        tendanceC.setCouleur(tendanceClients.equals("hausse") ? "green" : tendanceClients.equals("baisse") ? "red" : "gray");
        tendances.add(tendanceC);

        // Tendance Coûts opérationnels (simplifié - on pourrait utiliser TransactionRepository)
        BigDecimal evolutionCouts = BigDecimal.ZERO;
        String tendanceCouts = "stable";
        
        AnalyseDTO.TendanceDTO tendanceCo = new AnalyseDTO.TendanceDTO();
        tendanceCo.setCategorie("Coûts opérationnels");
        tendanceCo.setEvolution(evolutionCouts);
        tendanceCo.setTendance(tendanceCouts);
        tendanceCo.setCouleur("green");
        tendances.add(tendanceCo);

        // Tendance Temps de livraison (basé sur la différence moyenne entre dateDepart et dateArrivee, hors cession)
        List<Voyage> voyagesActuels = voyageRepository.findAll().stream()
            .filter(v -> !v.isCession())
            .filter(v -> v.getDateDepart() != null &&
                        v.getDateArrivee() != null &&
                        !v.getDateDepart().toLocalDate().isBefore(startDate) &&
                        !v.getDateDepart().toLocalDate().isAfter(endDate) &&
                        (v.getStatut() == Voyage.StatutVoyage.DECHARGER))
            .toList();

        long tempsMoyenActuel = 0;
        if (!voyagesActuels.isEmpty()) {
            tempsMoyenActuel = voyagesActuels.stream()
                .mapToLong(v -> java.time.temporal.ChronoUnit.DAYS.between(
                    v.getDateDepart().toLocalDate(),
                    v.getDateArrivee().toLocalDate()))
                .sum() / voyagesActuels.size();
        }

        List<Voyage> voyagesPrecedents = voyageRepository.findAll().stream()
            .filter(v -> !v.isCession())
            .filter(v -> v.getDateDepart() != null &&
                        v.getDateArrivee() != null &&
                        !v.getDateDepart().toLocalDate().isBefore(previousStartDate) &&
                        !v.getDateDepart().toLocalDate().isAfter(previousEndDate) &&
                        (v.getStatut() == Voyage.StatutVoyage.DECHARGER))
            .toList();

        long tempsMoyenPrecedent = 0;
        if (!voyagesPrecedents.isEmpty()) {
            tempsMoyenPrecedent = voyagesPrecedents.stream()
                .mapToLong(v -> java.time.temporal.ChronoUnit.DAYS.between(
                    v.getDateDepart().toLocalDate(),
                    v.getDateArrivee().toLocalDate()))
                .sum() / voyagesPrecedents.size();
        }

        BigDecimal evolutionTemps = BigDecimal.ZERO;
        if (tempsMoyenPrecedent > 0) {
            evolutionTemps = BigDecimal.valueOf(tempsMoyenPrecedent - tempsMoyenActuel)
                .divide(BigDecimal.valueOf(tempsMoyenPrecedent), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
        String tendanceTemps = determinerTendance(evolutionTemps);
        
        AnalyseDTO.TendanceDTO tendanceT = new AnalyseDTO.TendanceDTO();
        tendanceT.setCategorie("Temps de livraison");
        tendanceT.setEvolution(evolutionTemps);
        tendanceT.setTendance(tendanceTemps);
        tendanceT.setCouleur(tendanceTemps.equals("baisse") ? "green" : tendanceTemps.equals("hausse") ? "red" : "gray");
        tendances.add(tendanceT);

        // Tendance Taux d'annulation (voyages annulés, hors cession)
        long voyagesTotalActuels = voyageRepository.findAll().stream()
            .filter(v -> !v.isCession())
            .filter(v -> v.getDateDepart() != null &&
                        !v.getDateDepart().toLocalDate().isBefore(startDate) &&
                        !v.getDateDepart().toLocalDate().isAfter(endDate))
            .count();
        // Note: Il n'y a pas de statut "ANNULE" dans StatutVoyage, donc on pourrait utiliser un autre critère
        BigDecimal tauxAnnulation = BigDecimal.ZERO;
        String tendanceAnnulation = "stable";
        
        AnalyseDTO.TendanceDTO tendanceA = new AnalyseDTO.TendanceDTO();
        tendanceA.setCategorie("Taux d'annulation");
        tendanceA.setEvolution(tauxAnnulation);
        tendanceA.setTendance(tendanceAnnulation);
        tendanceA.setCouleur("red");
        tendances.add(tendanceA);

        return tendances;
    }

    private List<AnalyseDTO.PerformanceDTO> calculerPerformances() {
        List<AnalyseDTO.PerformanceDTO> performances = new ArrayList<>();

        // Taux de remplissage (basé sur les voyages)
        List<Voyage> voyages = voyageRepository.findAll();
        long voyagesTotal = voyages.size();
        long voyagesLivre = voyages.stream()
            .filter(v -> v.getStatut() == Voyage.StatutVoyage.DECHARGER)
            .count();

        BigDecimal tauxRemplissage = BigDecimal.ZERO;
        if (voyagesTotal > 0) {
            tauxRemplissage = BigDecimal.valueOf(voyagesLivre)
                .divide(BigDecimal.valueOf(voyagesTotal), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
        BigDecimal cibleRemplissage = BigDecimal.valueOf(90);
        BigDecimal pourcentageRemplissage = tauxRemplissage.divide(cibleRemplissage, 4, RoundingMode.HALF_UP)
                                                          .multiply(BigDecimal.valueOf(100));

        AnalyseDTO.PerformanceDTO perf1 = new AnalyseDTO.PerformanceDTO();
        perf1.setIndicateur("Taux de remplissage");
        perf1.setValeur(tauxRemplissage.setScale(1, RoundingMode.HALF_UP));
        perf1.setCible(cibleRemplissage);
        perf1.setPourcentage(pourcentageRemplissage.setScale(1, RoundingMode.HALF_UP));
        perf1.setCouleur(pourcentageRemplissage.compareTo(BigDecimal.valueOf(95)) >= 0 ? "green" :
                        pourcentageRemplissage.compareTo(BigDecimal.valueOf(80)) >= 0 ? "orange" : "red");
        performances.add(perf1);

        // Ponctualité livraisons (factures payées à temps)
        List<Facture> factures = factureRepository.findAll();
        long facturesTotal = factures.size();
        long facturesPonctuelles = factures.stream()
            .filter(f -> f.getDateEcheance() != null &&
                        f.getStatut() == Facture.StatutFacture.PAYEE &&
                        (f.getDateEcheance().isAfter(f.getDate()) || 
                         f.getDateEcheance().equals(f.getDate())))
            .count();

        BigDecimal ponctualite = BigDecimal.ZERO;
        if (facturesTotal > 0) {
            ponctualite = BigDecimal.valueOf(facturesPonctuelles)
                .divide(BigDecimal.valueOf(facturesTotal), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
        BigDecimal ciblePonctualite = BigDecimal.valueOf(95);
        BigDecimal pourcentagePonctualite = ponctualite.divide(ciblePonctualite, 4, RoundingMode.HALF_UP)
                                                      .multiply(BigDecimal.valueOf(100));

        AnalyseDTO.PerformanceDTO perf2 = new AnalyseDTO.PerformanceDTO();
        perf2.setIndicateur("Ponctualité livraisons");
        perf2.setValeur(ponctualite.setScale(1, RoundingMode.HALF_UP));
        perf2.setCible(ciblePonctualite);
        perf2.setPourcentage(pourcentagePonctualite.setScale(1, RoundingMode.HALF_UP));
        perf2.setCouleur(pourcentagePonctualite.compareTo(BigDecimal.valueOf(95)) >= 0 ? "green" :
                        pourcentagePonctualite.compareTo(BigDecimal.valueOf(80)) >= 0 ? "orange" : "red");
        performances.add(perf2);

        // Satisfaction clients (basé sur les factures payées)
        BigDecimal satisfaction = ponctualite; // Réutiliser la même logique
        BigDecimal cibleSatisfaction = BigDecimal.valueOf(90);
        BigDecimal pourcentageSatisfaction = satisfaction.divide(cibleSatisfaction, 4, RoundingMode.HALF_UP)
                                                        .multiply(BigDecimal.valueOf(100));

        AnalyseDTO.PerformanceDTO perf3 = new AnalyseDTO.PerformanceDTO();
        perf3.setIndicateur("Satisfaction clients");
        perf3.setValeur(satisfaction.setScale(1, RoundingMode.HALF_UP));
        perf3.setCible(cibleSatisfaction);
        perf3.setPourcentage(pourcentageSatisfaction.setScale(1, RoundingMode.HALF_UP));
        perf3.setCouleur(pourcentageSatisfaction.compareTo(BigDecimal.valueOf(95)) >= 0 ? "green" :
                        pourcentageSatisfaction.compareTo(BigDecimal.valueOf(80)) >= 0 ? "orange" : "red");
        performances.add(perf3);

        // Efficacité opérationnelle (voyages livrés / total)
        BigDecimal efficacite = tauxRemplissage; // Réutiliser
        BigDecimal cibleEfficacite = BigDecimal.valueOf(85);
        BigDecimal pourcentageEfficacite = efficacite.divide(cibleEfficacite, 4, RoundingMode.HALF_UP)
                                                     .multiply(BigDecimal.valueOf(100));

        AnalyseDTO.PerformanceDTO perf4 = new AnalyseDTO.PerformanceDTO();
        perf4.setIndicateur("Efficacité opérationnelle");
        perf4.setValeur(efficacite.setScale(1, RoundingMode.HALF_UP));
        perf4.setCible(cibleEfficacite);
        perf4.setPourcentage(pourcentageEfficacite.setScale(1, RoundingMode.HALF_UP));
        perf4.setCouleur(pourcentageEfficacite.compareTo(BigDecimal.valueOf(95)) >= 0 ? "green" :
                        pourcentageEfficacite.compareTo(BigDecimal.valueOf(80)) >= 0 ? "orange" : "red");
        performances.add(perf4);

        return performances;
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

    private BigDecimal calculerEvolutionPourcentage(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current.compareTo(BigDecimal.ZERO) > 0) {
                return BigDecimal.valueOf(100);
            }
            return BigDecimal.ZERO;
        }
        
        BigDecimal difference = current.subtract(previous);
        return difference.divide(previous, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP);
    }

    private String determinerTendance(BigDecimal evolution) {
        if (evolution.compareTo(BigDecimal.valueOf(1)) > 0) {
            return "hausse";
        } else if (evolution.compareTo(BigDecimal.valueOf(-1)) < 0) {
            return "baisse";
        }
        return "stable";
    }
}

