package com.backend.gesy.capitale;

import com.backend.gesy.achat.Achat;
import com.backend.gesy.achat.AchatRepository;
import com.backend.gesy.caisse.Caisse;
import com.backend.gesy.caisse.CaisseRepository;
import com.backend.gesy.categoriedepense.CategorieDepense;
import com.backend.gesy.categoriedepense.CategorieDepenseRepository;
import com.backend.gesy.comptebancaire.CompteBancaire;
import com.backend.gesy.comptebancaire.CompteBancaireRepository;
import com.backend.gesy.depense.Depense;
import com.backend.gesy.depense.DepenseRepository;
import com.backend.gesy.produit.Produit;
import com.backend.gesy.stock.Stock;
import com.backend.gesy.stock.StockRepository;
import com.backend.gesy.voyage.Voyage;
import com.backend.gesy.voyage.VoyageRepository;
import com.backend.gesy.capitale.dto.CapitaleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service

@Transactional
public class CapitaleServiceImpl implements CapitaleService {

    @Autowired
    private CompteBancaireRepository compteBancaireRepository;
    @Autowired
    private CaisseRepository caisseRepository;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private VoyageRepository voyageRepository;
    @Autowired
    private AchatRepository achatRepository;
    @Autowired
    private DepenseRepository depenseRepository;
    @Autowired
    private CategorieDepenseRepository categorieDepenseRepository;

    private static final DecimalFormat DECIMAL_FORMAT;

    static {
        DECIMAL_FORMAT = new DecimalFormat("#,###.00");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMAT.setDecimalFormatSymbols(symbols);
    }

    @Override
    @Transactional(readOnly = true)
    public CapitaleDTO calculateCapitale() {
        return calculateCapitaleInternal(null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CapitaleDTO calculateCapitaleByMonth(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return calculateCapitaleInternal(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public CapitaleDTO calculateCapitaleByDateRange(LocalDate startDate, LocalDate endDate) {
        return calculateCapitaleInternal(startDate, endDate);
    }

    private CapitaleDTO calculateCapitaleInternal(LocalDate startDate, LocalDate endDate) {
        // Calculer les fonds (toujours actuels, pas de filtre par date)
        CapitaleDTO.FondsDTO fonds = calculateFonds();

        // Calculer les stocks (avec filtre par date si fourni)
        CapitaleDTO.StocksDTO stocks = calculateStocks(startDate, endDate);

        // Calculer les dépenses investissement (avec filtre par date si fourni)
        CapitaleDTO.DepensesInvestissementDTO depensesInvestissement = calculateDepensesInvestissement(startDate, endDate);

        // Calculer le total capital
        BigDecimal totalCapital = fonds.getTotalGeneralValue()
                .add(stocks.getTotalStocksValue())
                .subtract(depensesInvestissement.getTotalValue());

        return CapitaleDTO.builder()
                .fonds(fonds)
                .stocks(stocks)
                .depensesInvestissement(depensesInvestissement)
                .totalCapital(formatBigDecimal(totalCapital))
                .totalCapitalValue(totalCapital)
                .build();
    }

    private CapitaleDTO.FondsDTO calculateFonds() {
        // Total des comptes bancaires (type BANQUE uniquement)
        BigDecimal totalBanques = compteBancaireRepository.findAll().stream()
                .filter(cb -> cb.getType() == CompteBancaire.TypeCompte.BANQUE && cb.getStatut() == CompteBancaire.StatutCompte.ACTIF)
                .map(CompteBancaire::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total des caisses
        BigDecimal totalCaisses = caisseRepository.findAll().stream()
                .filter(c -> c.getStatut() == Caisse.StatutCaisse.ACTIF)
                .map(Caisse::getSolde)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total général
        BigDecimal totalGeneral = totalBanques.add(totalCaisses);

        return CapitaleDTO.FondsDTO.builder()
                .totalBanques(formatBigDecimal(totalBanques))
                .totalBanquesValue(totalBanques)
                .totalCaisses(formatBigDecimal(totalCaisses))
                .totalCaissesValue(totalCaisses)
                .totalGeneral(formatBigDecimal(totalGeneral))
                .totalGeneralValue(totalGeneral)
                .build();
    }

    private CapitaleDTO.StocksDTO calculateStocks(LocalDate startDate, LocalDate endDate) {
        // Stocks en dépôt par produit (toujours actuels, pas de filtre par date)
        // Filtrer pour n'utiliser que les stocks des dépôts ACTIF
        List<Stock> stocksDepot = stockRepository.findByDepotStatut(com.backend.gesy.depot.Depot.StatutDepot.ACTIF).stream()
                .filter(s -> !s.isCiterne() && s.getDepot() != null)
                .collect(Collectors.toList());

        Map<Produit, List<Stock>> stocksParProduitDepot = stocksDepot.stream()
                .collect(Collectors.groupingBy(Stock::getProduit));

        List<CapitaleDTO.StockParProduitDTO> stocksDepotList = stocksParProduitDepot.entrySet().stream()
                .map(entry -> {
                    Produit produit = entry.getKey();
                    List<Stock> stocks = entry.getValue();
                    Double quantiteTotale = stocks.stream()
                            .mapToDouble(s -> s.getQuantite() != null ? s.getQuantite() : 0.0)
                            .sum();
                    BigDecimal valeur = stocks.stream()
                            .map(s -> {
                                Double qte = s.getQuantite() != null ? s.getQuantite() : 0.0;
                                Double prix = s.getPrixUnitaire() != null ? s.getPrixUnitaire() : 0.0;
                                return BigDecimal.valueOf(qte).multiply(BigDecimal.valueOf(prix));
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return CapitaleDTO.StockParProduitDTO.builder()
                            .produitId(produit.getId())
                            .produitNom(produit.getNom())
                            .typeProduit(produit.getTypeProduit() != null ? produit.getTypeProduit().name() : null)
                            .quantite(quantiteTotale)
                            .valeur(formatBigDecimal(valeur))
                            .valeurValue(valeur)
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalStocksDepot = stocksDepotList.stream()
                .map(CapitaleDTO.StockParProduitDTO::getValeurValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Stocks en camion (voyages non livrés, hors cession) par produit
        // Si une date de fin est fournie, on filtre les voyages créés avant cette date
        List<Voyage> voyagesNonLivres = voyageRepository.findAll().stream()
                .filter(v -> !v.isCession())
                .filter(v -> v.getStatut() != Voyage.StatutVoyage.LIVRE)
                .filter(v -> {
                    // Si une date de fin est fournie, on ne prend que les voyages créés avant cette date
                    if (endDate != null) {
                        // Utiliser la date de départ si disponible, sinon la date de création (via l'ID ou autre)
                        // Pour simplifier, on utilise la date de départ du voyage
                        if (v.getDateDepart() != null) {
                            LocalDate dateDepart = v.getDateDepart().toLocalDate();
                            return dateDepart.isBefore(endDate) || dateDepart.isEqual(endDate);
                        }
                        // Si pas de date de départ, on inclut le voyage (cas par défaut)
                        return true;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        Map<Produit, List<Voyage>> voyagesParProduit = voyagesNonLivres.stream()
                .filter(v -> v.getProduit() != null)
                .collect(Collectors.groupingBy(Voyage::getProduit));

        // Calculer le prix d'achat moyen par produit depuis les achats (filtrés par date si fournie)
        Map<Long, BigDecimal> prixAchatMoyenParProduit = calculatePrixAchatMoyenParProduit(endDate);

        List<CapitaleDTO.StockParProduitDTO> stocksCamionList = voyagesParProduit.entrySet().stream()
                .map(entry -> {
                    Produit produit = entry.getKey();
                    List<Voyage> voyages = entry.getValue();
                    Double quantiteTotale = voyages.stream()
                            .mapToDouble(v -> v.getQuantite() != null ? v.getQuantite() : 0.0)
                            .sum();
                    BigDecimal prixAchatMoyen = prixAchatMoyenParProduit.getOrDefault(produit.getId(), BigDecimal.ZERO);
                    BigDecimal valeur = BigDecimal.valueOf(quantiteTotale).multiply(prixAchatMoyen);

                    return CapitaleDTO.StockParProduitDTO.builder()
                            .produitId(produit.getId())
                            .produitNom(produit.getNom())
                            .typeProduit(produit.getTypeProduit() != null ? produit.getTypeProduit().name() : null)
                            .quantite(quantiteTotale)
                            .valeur(formatBigDecimal(valeur))
                            .valeurValue(valeur)
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalStocksCamion = stocksCamionList.stream()
                .map(CapitaleDTO.StockParProduitDTO::getValeurValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalStocks = totalStocksDepot.add(totalStocksCamion);

        return CapitaleDTO.StocksDTO.builder()
                .stocksDepot(stocksDepotList)
                .totalStocksDepot(formatBigDecimal(totalStocksDepot))
                .totalStocksDepotValue(totalStocksDepot)
                .stocksCamion(stocksCamionList)
                .totalStocksCamion(formatBigDecimal(totalStocksCamion))
                .totalStocksCamionValue(totalStocksCamion)
                .totalStocks(formatBigDecimal(totalStocks))
                .totalStocksValue(totalStocks)
                .build();
    }

    private Map<Long, BigDecimal> calculatePrixAchatMoyenParProduit(LocalDate endDate) {
        List<Achat> achats;
        // Si une date de fin est fournie, on filtre les achats avant ou à cette date
        if (endDate != null) {
            LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
            achats = achatRepository.findAll().stream()
                    .filter(a -> a.getDateAchat() != null && 
                                 (a.getDateAchat().isBefore(endDateTime) || 
                                  a.getDateAchat().toLocalDate().isEqual(endDate)))
                    .collect(Collectors.toList());
        } else {
            achats = achatRepository.findAll();
        }
        Map<Long, List<Achat>> achatsParProduit = achats.stream()
                .filter(a -> a.getProduit() != null && a.getPrixUnitaire() != null)
                .collect(Collectors.groupingBy(a -> a.getProduit().getId()));

        Map<Long, BigDecimal> prixMoyenParProduit = new HashMap<>();
        for (Map.Entry<Long, List<Achat>> entry : achatsParProduit.entrySet()) {
            List<Achat> achatsProduit = entry.getValue();
            BigDecimal sommePrix = achatsProduit.stream()
                    .map(a -> a.getPrixUnitaire())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal prixMoyen = sommePrix.divide(BigDecimal.valueOf(achatsProduit.size()), 2, RoundingMode.HALF_UP);
            prixMoyenParProduit.put(entry.getKey(), prixMoyen);
        }

        return prixMoyenParProduit;
    }

    private CapitaleDTO.DepensesInvestissementDTO calculateDepensesInvestissement(LocalDate startDate, LocalDate endDate) {
        Optional<CategorieDepense> categorieInvestissement = categorieDepenseRepository.findByNom("Investissement");
        
        if (categorieInvestissement.isEmpty()) {
            return CapitaleDTO.DepensesInvestissementDTO.builder()
                    .total(formatBigDecimal(BigDecimal.ZERO))
                    .totalValue(BigDecimal.ZERO)
                    .build();
        }

        List<Depense> depenses;
        if (startDate != null && endDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
            depenses = depenseRepository.findByCategorieAndDateRangeList(
                    categorieInvestissement.get(), startDateTime, endDateTime);
        } else {
            depenses = depenseRepository.findByCategorieOrderByDateDepenseDesc(categorieInvestissement.get());
        }

        BigDecimal total = depenses.stream()
                .map(Depense::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CapitaleDTO.DepensesInvestissementDTO.builder()
                .total(formatBigDecimal(total))
                .totalValue(total)
                .build();
    }

    private String formatBigDecimal(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return DECIMAL_FORMAT.format(value.doubleValue()) + " F";
    }
}
