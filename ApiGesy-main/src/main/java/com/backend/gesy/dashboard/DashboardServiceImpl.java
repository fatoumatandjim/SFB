package com.backend.gesy.dashboard;

import com.backend.gesy.camion.Camion;
import com.backend.gesy.camion.CamionRepository;
import com.backend.gesy.caisse.Caisse;
import com.backend.gesy.caisse.CaisseRepository;
import com.backend.gesy.comptebancaire.CompteBancaire;
import com.backend.gesy.comptebancaire.CompteBancaireRepository;
import com.backend.gesy.dashboard.dto.DashboardDTO;
import com.backend.gesy.depot.Depot;
import com.backend.gesy.depot.DepotRepository;
import com.backend.gesy.facture.Facture;
import com.backend.gesy.facture.FactureRepository;
import com.backend.gesy.produit.Produit;
import com.backend.gesy.stock.Stock;
import com.backend.gesy.stock.StockRepository;
import com.backend.gesy.transaction.Transaction;
import com.backend.gesy.transaction.TransactionRepository;
import com.backend.gesy.voyage.ClientVoyage;
import com.backend.gesy.voyage.Voyage;
import com.backend.gesy.voyage.VoyageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

        private final CamionRepository camionRepository;
        private final VoyageRepository voyageRepository;
        private final FactureRepository factureRepository;
        private final StockRepository stockRepository;
        private final DepotRepository depotRepository;
        private final CompteBancaireRepository compteBancaireRepository;
        private final CaisseRepository caisseRepository;
        private final TransactionRepository transactionRepository;

        @Override
        public DashboardDTO getDashboardStats() {
                return DashboardDTO.builder()
                                .camionsActifs(calculateCamionsActifs())
                                .chiffreAffaires(calculateChiffreAffaires())
                                .facturesAttente(calculateFacturesAttente())
                                .unitesStock(calculateUnitesStock())
                                .finances(calculateFinances())
                                .voyagesStats(calculateVoyagesStats())
                                .douaneStats(calculateDouaneStats())
                                .build();
        }

        private DashboardDTO.CamionsActifsDTO calculateCamionsActifs() {
                List<Camion> allCamions = camionRepository.findAll();

                long totalActifs = allCamions.stream()
                                .filter(c -> c.getStatut() != Camion.StatutCamion.HORS_SERVICE)
                                .count();

                long enRoute = allCamions.stream()
                                .filter(c -> c.getStatut() == Camion.StatutCamion.EN_ROUTE)
                                .count();

                long disponibles = allCamions.stream()
                                .filter(c -> c.getStatut() == Camion.StatutCamion.DISPONIBLE)
                                .count();

                // Calculer le changement (comparaison avec le mois dernier - simplifié)
                String change = "+12%"; // TODO: Calculer réellement avec historique

                return DashboardDTO.CamionsActifsDTO.builder()
                                .value((int) totalActifs)
                                .change(change)
                                .enRoute((int) enRoute)
                                .disponibles((int) disponibles)
                                .build();
        }

        private DashboardDTO.ChiffreAffairesDTO calculateChiffreAffaires() {
                LocalDate startOfWeek = LocalDate.now().minusDays(7);
                List<Facture> facturesSemaine = factureRepository.findAll().stream()
                                .filter(f -> f.getDate() != null && f.getDate().isAfter(startOfWeek))
                                .filter(f -> f.getStatut() == Facture.StatutFacture.PAYEE ||
                                                f.getStatut() == Facture.StatutFacture.PARTIELLEMENT_PAYEE)
                                .toList();

                BigDecimal totalCA = facturesSemaine.stream()
                                .map(f -> f.getMontantPaye() != null ? f.getMontantPaye() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calculer la semaine précédente pour le changement
                LocalDate startOfLastWeek = startOfWeek.minusDays(7);
                List<Facture> facturesSemainePrecedente = factureRepository.findAll().stream()
                                .filter(f -> f.getDate() != null &&
                                                f.getDate().isAfter(startOfLastWeek) &&
                                                f.getDate().isBefore(startOfWeek))
                                .filter(f -> f.getStatut() == Facture.StatutFacture.PAYEE ||
                                                f.getStatut() == Facture.StatutFacture.PARTIELLEMENT_PAYEE)
                                .toList();

                BigDecimal totalCAPrecedent = facturesSemainePrecedente.stream()
                                .map(f -> f.getMontantPaye() != null ? f.getMontantPaye() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                String change = "+8%";
                if (totalCAPrecedent.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal diff = totalCA.subtract(totalCAPrecedent);
                        BigDecimal percentChange = diff.divide(totalCAPrecedent, 4, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100));
                        change = (percentChange.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") +
                                        percentChange.setScale(0, RoundingMode.HALF_UP) + "%";
                }

                BigDecimal increase = totalCA.subtract(totalCAPrecedent);

                return DashboardDTO.ChiffreAffairesDTO.builder()
                                .value(formatBigDecimal(totalCA))
                                .currency("F")
                                .change(change)
                                .period("Cette semaine")
                                .increase((increase.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "")
                                                + formatBigDecimal(increase) + " F")
                                .build();
        }

        private DashboardDTO.FacturesAttenteDTO calculateFacturesAttente() {
                List<Facture> facturesNonPayees = factureRepository.findUnpaidFactures();
                List<Facture> facturesEnRetard = factureRepository.findOverdueFactures();

                BigDecimal montantTotal = facturesNonPayees.stream()
                                .map(f -> {
                                        BigDecimal reste = f.getMontantTTC().subtract(
                                                        f.getMontantPaye() != null ? f.getMontantPaye()
                                                                        : BigDecimal.ZERO);
                                        return reste;
                                })
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return DashboardDTO.FacturesAttenteDTO.builder()
                                .value(facturesNonPayees.size())
                                .badge(facturesEnRetard.size())
                                .montant(formatBigDecimal(montantTotal) + " F")
                                .enRetard(facturesEnRetard.size())
                                .build();
        }

        private DashboardDTO.UnitesStockDTO calculateUnitesStock() {
                // Filtrer les stocks pour n'utiliser que ceux des dépôts ACTIF
                List<Stock> allStocks = stockRepository.findByDepotStatut(Depot.StatutDepot.ACTIF);

                // Calculer le total global (pour la compatibilité)
                BigDecimal totalQuantite = allStocks.stream()
                                .map(s -> BigDecimal.valueOf(s.getQuantite()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                String stockRestantFormate = formatBigDecimal(totalQuantite);

                // Grouper les stocks par produit
                Map<Produit, List<Stock>> stocksParProduitMap = allStocks.stream()
                                .filter(s -> s.getProduit() != null)
                                .collect(Collectors.groupingBy(Stock::getProduit));

                // Calculer le stock par produit
                List<DashboardDTO.StockParProduitDTO> stocksParProduit = stocksParProduitMap.entrySet().stream()
                                .map(entry -> {
                                        Produit produit = entry.getKey();
                                        List<Stock> stocks = entry.getValue();

                                        // Calculer la quantité totale pour ce produit
                                        BigDecimal quantiteTotale = stocks.stream()
                                                        .map(s -> BigDecimal.valueOf(s.getQuantite()))
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                                        // Vérifier si le stock est critique pour ce produit (seuil minimum)
                                        boolean alert = stocks.stream()
                                                        .anyMatch(s -> s.getSeuilMinimum() != null &&
                                                                        s.getQuantite() < s.getSeuilMinimum());

                                        // Compter le nombre de dépôts distincts ACTIF pour ce produit
                                        long nombreDepots = stocks.stream()
                                                        .filter(s -> s.getDepot() != null && 
                                                                        s.getDepot().getStatut() == Depot.StatutDepot.ACTIF)
                                                        .map(s -> s.getDepot().getId())
                                                        .distinct()
                                                        .count();

                                        return DashboardDTO.StockParProduitDTO.builder()
                                                        .produitId(produit.getId())
                                                        .produitNom(produit.getNom())
                                                        .typeProduit(produit.getTypeProduit() != null
                                                                        ? produit.getTypeProduit().name()
                                                                        : null)
                                                        .quantiteTotale(formatBigDecimal(quantiteTotale))
                                                        .quantiteTotaleValue(quantiteTotale.doubleValue())
                                                        .alert(alert)
                                                        .nombreDepots((int) nombreDepots)
                                                        .build();
                                })
                                .sorted((s1, s2) -> Double.compare(s2.getQuantiteTotaleValue(),
                                                s1.getQuantiteTotaleValue())) // Trier par quantité décroissante
                                .collect(Collectors.toList());

                // Compter les produits en niveau critique
                long niveauCritique = stocksParProduit.stream()
                                .filter(DashboardDTO.StockParProduitDTO::getAlert)
                                .count();

                // Utiliser uniquement les dépôts ACTIF
                List<Depot> allDepots = depotRepository.findByStatut(Depot.StatutDepot.ACTIF);

                return DashboardDTO.UnitesStockDTO.builder()
                                .value(formatBigDecimal(totalQuantite))
                                .stockRestant(stockRestantFormate)
                                .alert(niveauCritique > 0)
                                .niveauCritique((int) niveauCritique)
                                .depots(allDepots.size())
                                .stocksParProduit(stocksParProduit)
                                .build();
        }

        private DashboardDTO.FinancesDTO calculateFinances() {
                return DashboardDTO.FinancesDTO.builder()
                                .soldeBanque(calculateSoldeBanque())
                                .soldeCaisse(calculateSoldeCaisse())
                                .creancesClients(calculateCreancesClients())
                                .build();
        }

        private DashboardDTO.SoldeBanqueDTO calculateSoldeBanque() {
                List<CompteBancaire> comptes = compteBancaireRepository.findAll();

                BigDecimal totalSolde = comptes.stream()
                                .map(CompteBancaire::getSolde)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calculer le changement (simplifié)
                String change = "+5.2% vs mois dernier"; // TODO: Calculer réellement

                return DashboardDTO.SoldeBanqueDTO.builder()
                                .value(formatBigDecimal(totalSolde))
                                .currency("F")
                                .comptes(comptes.size())
                                .change(change)
                                .build();
        }

        private DashboardDTO.SoldeCaisseDTO calculateSoldeCaisse() {
                List<Caisse> caisses = caisseRepository.findAll();

                BigDecimal totalSolde = caisses.stream()
                                .map(Caisse::getSolde)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calculer les entrées d'aujourd'hui (simplifié)
                String entrees = "125,000 F"; // TODO: Calculer réellement depuis les transactions

                return DashboardDTO.SoldeCaisseDTO.builder()
                                .value(formatBigDecimal(totalSolde))
                                .currency("F")
                                .date("Arrêté aujourd'hui")
                                .entrees(entrees)
                                .build();
        }

        private DashboardDTO.CreancesClientsDTO calculateCreancesClients() {
                List<Facture> facturesNonPayees = factureRepository.findUnpaidFactures();

                BigDecimal totalCreances = facturesNonPayees.stream()
                                .map(f -> {
                                        BigDecimal reste = f.getMontantTTC().subtract(
                                                        f.getMontantPaye() != null ? f.getMontantPaye()
                                                                        : BigDecimal.ZERO);
                                        return reste;
                                })
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                List<Facture> facturesEnRetard = factureRepository.findOverdueFactures();
                BigDecimal totalRetard = facturesEnRetard.stream()
                                .map(f -> {
                                        BigDecimal reste = f.getMontantTTC().subtract(
                                                        f.getMontantPaye() != null ? f.getMontantPaye()
                                                                        : BigDecimal.ZERO);
                                        return reste;
                                })
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                long clientsConcernes = facturesNonPayees.stream()
                                .map(Facture::getClient)
                                .filter(client -> client != null)
                                .distinct()
                                .count();

                return DashboardDTO.CreancesClientsDTO.builder()
                                .value(formatBigDecimal(totalCreances))
                                .currency("F")
                                .clients((int) clientsConcernes)
                                .retard(formatBigDecimal(totalRetard) + " F")
                                .build();
        }

        private DashboardDTO.VoyagesStatsDTO calculateVoyagesStats() {
                // Hors cession : les voyages de type cession ne sont pas comptés dans les stats
                List<Voyage> allVoyages = voyageRepository.findAll().stream()
                                .filter(v -> !v.isCession())
                                .collect(Collectors.toList());

                // Voyages en cours : CHARGEMENT, CHARGE, DEPART, ARRIVER, DOUANE
                long voyagesEnCours = allVoyages.stream()
                                .filter(v -> v.getStatut() == Voyage.StatutVoyage.CHARGEMENT ||
                                                v.getStatut() == Voyage.StatutVoyage.CHARGE ||
                                                v.getStatut() == Voyage.StatutVoyage.DEPART ||
                                                v.getStatut() == Voyage.StatutVoyage.ARRIVER ||
                                                v.getStatut() == Voyage.StatutVoyage.DOUANE)
                                .count();

                // Voyages arrivés : ARRIVER
                long voyagesArrives = allVoyages.stream()
                                .filter(v -> v.getStatut() == Voyage.StatutVoyage.ARRIVER)
                                .count();

                // Voyages à la douane : DOUANE
                long voyagesALaDouane = allVoyages.stream()
                                .filter(v -> v.getStatut() == Voyage.StatutVoyage.DOUANE)
                                .count();

                // Voyages livrés : LIVRE
                long voyagesLivre = allVoyages.stream()
                                .filter(v -> v.getStatut() == Voyage.StatutVoyage.LIVRE)
                                .count();

                // Voyages récents (5 derniers)
                List<DashboardDTO.VoyageDetailDTO> voyagesRecents = allVoyages.stream()
                                .filter(v -> v.getDateDepart() != null)
                                .sorted((v1, v2) -> v2.getDateDepart().compareTo(v1.getDateDepart()))
                                .limit(5)
                                .map(v -> DashboardDTO.VoyageDetailDTO.builder()
                                                .id(v.getId())
                                                .numeroVoyage(v.getNumeroVoyage())
                                                .camionImmatriculation(v.getCamion() != null
                                                                ? v.getCamion().getImmatriculation()
                                                                : "N/A")
                                                .clientNom(nomsClient(v.getClientVoyages()))
                                                .destination(v.getDestination())
                                                .statut(v.getStatut().name())
                                                .dateDepart(v.getDateDepart().format(
                                                                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                                                .build())
                                .collect(Collectors.toList());

                return DashboardDTO.VoyagesStatsDTO.builder()
                                .totalVoyagesEnCours((int) voyagesEnCours)
                                .voyagesArrives((int) voyagesArrives)
                                .voyagesALaDouane((int) voyagesALaDouane)
                                .voyagesLivre((int) voyagesLivre)
                                .voyagesRecents(voyagesRecents)
                                .build();
        }

        private String nomsClient(List<ClientVoyage> clientVoyages) {
                if (clientVoyages == null || clientVoyages.isEmpty()) {
                        return "N/A";
                }
                return clientVoyages.stream()
                                .map(cv -> cv.getClient() != null ? cv.getClient().getNom() : "N/A")
                                .collect(Collectors.joining(", "));
        }

        private DashboardDTO.DouaneStatsDTO calculateDouaneStats() {
                LocalDate now = LocalDate.now();
                LocalDate startOfMonth = now.withDayOfMonth(1);
                LocalDateTime startOfMonthDateTime = startOfMonth.atStartOfDay();
                LocalDateTime endOfMonthDateTime = now.atTime(23, 59, 59);

                BigDecimal montantFraisDouane = transactionRepository
                                .sumMontantByTypeAndDateRange(Transaction.TypeTransaction.FRAIS_DOUANE,
                                                startOfMonthDateTime, endOfMonthDateTime);
                if (montantFraisDouane == null) montantFraisDouane = BigDecimal.ZERO;

                BigDecimal montantT1 = transactionRepository
                                .sumMontantByTypeAndDateRange(Transaction.TypeTransaction.FRAIS_T1,
                                                startOfMonthDateTime, endOfMonthDateTime);
                if (montantT1 == null) montantT1 = BigDecimal.ZERO;

                BigDecimal montantTotalFrais = montantFraisDouane.add(montantT1);

                // Compter le nombre de voyages distincts avec des transactions FRAIS_DOUANE ce
                // mois
                Long nombreCamionsDeclares = transactionRepository
                                .countDistinctVoyagesAvecFraisDouane(startOfMonthDateTime, endOfMonthDateTime);

                if (nombreCamionsDeclares == null) {
                        nombreCamionsDeclares = 0L;
                }

                // Compter les voyages non déclarés ce mois (voyages avec dateDepart ce mois et
                // declarer = false ou null, hors cession)
                long nombreCamionsNonDeclares = voyageRepository.findAll().stream()
                                .filter(v -> v.getDateDepart() != null)
                                .filter(v -> {
                                        LocalDateTime dateDepart = v.getDateDepart();
                                        return !dateDepart.isBefore(startOfMonthDateTime)
                                                        && !dateDepart.isAfter(endOfMonthDateTime);
                                })
                                .filter(v -> v.getDeclarer() == null || !v.getDeclarer())
                                .count();

                return DashboardDTO.DouaneStatsDTO.builder()
                                .nombreCamionsDeclares(nombreCamionsDeclares.intValue())
                                .nombreCamionsNonDeclares((int) nombreCamionsNonDeclares)
                                .montantFraisDouane(formatBigDecimal(montantFraisDouane) + " F")
                                .montantT1(formatBigDecimal(montantT1) + " F")
                                .montantFraisPayes(formatBigDecimal(montantTotalFrais) + " F")
                                .currency("F")
                                .build();
        }

        private String formatBigDecimal(BigDecimal value) {
                if (value == null) {
                        return "0";
                }
                return String.format("%,.0f", value.doubleValue()).replace(",", " ");
        }
}
