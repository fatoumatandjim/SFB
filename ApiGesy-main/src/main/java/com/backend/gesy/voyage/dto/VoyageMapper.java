package com.backend.gesy.voyage.dto;

import com.backend.gesy.transaction.dto.TransactionMapper;
import com.backend.gesy.voyage.ClientVoyage;
import com.backend.gesy.voyage.Voyage;
import com.backend.gesy.compte.CompteRepository;
import com.backend.gesy.depot.DepotRepository;
import com.backend.gesy.axe.AxeRepository;
import com.backend.gesy.facture.dto.FactureMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VoyageMapper {
    private final TransactionMapper transactionMapper;
    private final CompteRepository compteRepository;
    private final DepotRepository depotRepository;
    private final AxeRepository axeRepository;
    private final ClientVoyageMapper clientVoyageMapper;
    private final FactureMapper factureMapper;

    public VoyageDTO toDTO(Voyage voyage) {
        if (voyage == null) {
            return null;
        }

        VoyageDTO dto = new VoyageDTO();
        dto.setId(voyage.getId());
        dto.setNumeroVoyage(voyage.getNumeroVoyage());
        dto.setCamionId(voyage.getCamion() != null ? voyage.getCamion().getId() : null);
        dto.setCamionImmatriculation(voyage.getCamion() != null ? voyage.getCamion().getImmatriculation() : null);
        // Utiliser le premier ClientVoyage pour les informations client (compatibilité)
        if (voyage.getClientVoyages() != null && !voyage.getClientVoyages().isEmpty()) {
            ClientVoyage premierClientVoyage = voyage.getClientVoyages().get(0);
            dto.setClientId(premierClientVoyage.getClient() != null ? premierClientVoyage.getClient().getId() : null);
            dto.setClientNom(premierClientVoyage.getClient() != null ? premierClientVoyage.getClient().getNom() : null);
            dto.setClientEmail(premierClientVoyage.getClient() != null ? premierClientVoyage.getClient().getEmail() : null);
        } else {
            dto.setClientId(null);
            dto.setClientNom(null);
            dto.setClientEmail(null);
        }
        dto.setTransitaireId(voyage.getTransitaire() != null ? voyage.getTransitaire().getId() : null);
        dto.setTransitaireNom(voyage.getTransitaire() != null ? voyage.getTransitaire().getNom() : null);
        dto.setTransitaireIdentifiant(
                voyage.getTransitaire() != null ? voyage.getTransitaire().getIdentifiant() : null);
        dto.setTransitairePhone(voyage.getTransitaire() != null ? voyage.getTransitaire().getTelephone() : null);
        dto.setAxeId(voyage.getAxe() != null ? voyage.getAxe().getId() : null);
        dto.setAxeNom(voyage.getAxe() != null ? voyage.getAxe().getNom() : null);
        dto.setDateDepart(voyage.getDateDepart());
        dto.setDateArrivee(voyage.getDateArrivee());
        dto.setDestination(voyage.getDestination());
        dto.setLieuDepart(voyage.getLieuDepart());
        dto.setStatut(voyage.getStatut() != null ? voyage.getStatut().name() : null);
        dto.setQuantite(voyage.getQuantite());
        dto.setManquant(voyage.getManquant());
        dto.setPrixUnitaire(voyage.getPrixUnitaire());
        dto.setProduitId(voyage.getProduit() != null ? voyage.getProduit().getId() : null);
        dto.setProduitNom(voyage.getProduit() != null ? voyage.getProduit().getNom() : null);
        dto.setTypeProduit(voyage.getProduit() != null && voyage.getProduit().getTypeProduit() != null
                ? voyage.getProduit().getTypeProduit().name()
                : null);
        dto.setDepotId(voyage.getDepot() != null ? voyage.getDepot().getId() : null);
        dto.setDepotNom(voyage.getDepot() != null ? voyage.getDepot().getNom() : null);
        dto.setResponsableId(voyage.getResponsable() != null ? voyage.getResponsable().getId() : null);
        dto.setResponsableIdentifiant(voyage.getResponsable() != null ? voyage.getResponsable().getIdentifiant() : null);
        dto.setNumeroBonEnlevement(voyage.getNumeroBonEnlevement());
        dto.setCession(voyage.isCession());
        dto.setLiberer(voyage.getLiberer() != null ? voyage.getLiberer() : false);

        // Calculer le coût du voyage (0 en cas de cession)
        java.math.BigDecimal coutVoyage;
        if (voyage.isCession() || voyage.getPrixUnitaire() == null) {
            coutVoyage = java.math.BigDecimal.ZERO;
        } else if (voyage.getManquant() != null && voyage.getManquant() > 0) {
            Double quantiteFacturee = voyage.getManquant() - voyage.getQuantite();
            coutVoyage = voyage.getPrixUnitaire()
                    .multiply(java.math.BigDecimal.valueOf(quantiteFacturee));
        } else {
            coutVoyage = voyage.getPrixUnitaire()
                    .multiply(java.math.BigDecimal.valueOf(voyage.getQuantite()));
        }
        dto.setCoutVoyage(coutVoyage);

        dto.setNotes(voyage.getNotes());
        dto.setDeclarer(voyage.getDeclarer() != null ? voyage.getDeclarer() : false);
        dto.setPassager(voyage.getPassager());
        dto.setChauffeur(voyage.getChauffeur());
        dto.setNumeroChauffeur(voyage.getNumeroChauffeur());

        // Mapper la première facture si présente (pour compatibilité avec le DTO)
        if (voyage.getFactures() != null && !voyage.getFactures().isEmpty()) {
            com.backend.gesy.facture.Facture facture = voyage.getFactures().get(0);
            dto.setFactureId(facture.getId());
            dto.setFactureNumero(facture.getNumero());
            dto.setFactureMontant(facture.getMontant());
            dto.setFactureMontantPaye(facture.getMontantPaye());
            dto.setFactureStatut(
                    facture.getStatut() != null ? facture.getStatut().name() : null);
        }

        // Mapper la liste complète des ClientVoyages
        if (voyage.getClientVoyages() != null) {
            dto.setClientVoyages(voyage.getClientVoyages().stream()
                    .map(clientVoyageMapper::toDTO)
                    .collect(Collectors.toList()));
        }

        // Mapper la liste complète des Factures
        if (voyage.getFactures() != null) {
            dto.setFactures(voyage.getFactures().stream()
                    .map(factureMapper::toDTO)
                    .collect(Collectors.toList()));
        }

        // Mapper les transactions
        if (voyage.getTransactions() != null) {
            dto.setTransactions(voyage.getTransactions().stream()
                    .map(transactionMapper::toDTO)
                    .collect(Collectors.toList()));
        }

        // Mapper les états
        if (voyage.getEtats() != null) {
            dto.setEtats(voyage.getEtats().stream()
                    .map(etat -> {
                        EtatVoyageDTO etatDTO = new EtatVoyageDTO();
                        etatDTO.setId(etat.getId());
                        etatDTO.setEtat(etat.getEtat());
                        etatDTO.setDateHeure(etat.getDateHeure());
                        etatDTO.setValider(etat.getValider());
                        return etatDTO;
                    })
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public Voyage toEntity(VoyageDTO dto) {
        if (dto == null) {
            return null;
        }

        Voyage voyage = new Voyage();
        voyage.setId(dto.getId());
        voyage.setNumeroVoyage(dto.getNumeroVoyage());
        voyage.setDateDepart(dto.getDateDepart());
        voyage.setDateArrivee(dto.getDateArrivee());
        voyage.setDestination(dto.getDestination());
        voyage.setLieuDepart(dto.getLieuDepart());
        if (dto.getStatut() != null) {
            voyage.setStatut(convertStatut(dto.getStatut()));
        }
        voyage.setQuantite(dto.getQuantite());
        voyage.setManquant(dto.getManquant());
        voyage.setPrixUnitaire(dto.getPrixUnitaire());
        voyage.setCession(Boolean.TRUE.equals(dto.getCession()));
        voyage.setLiberer(Boolean.TRUE.equals(dto.getLiberer()));
        voyage.setNotes(dto.getNotes());
        voyage.setNumeroBonEnlevement(dto.getNumeroBonEnlevement());
        voyage.setDeclarer(dto.getDeclarer() != null ? dto.getDeclarer() : false);
        voyage.setPassager(dto.getPassager());
        voyage.setChauffeur(dto.getChauffeur());

        // Mapper le dépôt si fourni
        if (dto.getDepotId() != null && dto.getDepotId() > 0) {
            depotRepository.findById(dto.getDepotId())
                    .ifPresent(voyage::setDepot);
        }

        // Mapper l'axe si fourni
        if (dto.getAxeId() != null && dto.getAxeId() > 0) {
            axeRepository.findById(dto.getAxeId())
                    .ifPresent(voyage::setAxe);
        }

        // Mapper le responsable (compte) si fourni
        if (dto.getResponsableId() != null && dto.getResponsableId() > 0) {
            compteRepository.findById(dto.getResponsableId())
                    .ifPresent(voyage::setResponsable);
        }

        return voyage;
    }

    /**
     * Convertit un statut (peut être ancien ou nouveau) vers le nouveau format
     */
    private Voyage.StatutVoyage convertStatut(String statut) {
        if (statut == null) {
            return null;
        }

        // Mapping des anciens statuts vers les nouveaux
        switch (statut) {
            case "ASSIGNE_AU_CHARGEMENT":
            case "EN_CHARGEMENT":
                return Voyage.StatutVoyage.CHARGEMENT;
            case "DEPART":
                return Voyage.StatutVoyage.DEPART;
            case "EN_ROUTE_VERS_BAMAKO":
            case "EN_DEPOT_SORTIE_DEPOT":
            case "EN_TRANSIT_VERS_STATION":
                return Voyage.StatutVoyage.ARRIVER;
            case "A_LA_DOUANE":
            case "A_LA_FRONTIERE":
            case "SORTIE_A_LA_DOUANE":
                return Voyage.StatutVoyage.DOUANE;
            case "DECLARE":
                return Voyage.StatutVoyage.RECEPTIONNER;
            case "LIVRE":
            case "DEPOTE_EN_STATION":
                return Voyage.StatutVoyage.LIVRE;
            default:
                // Essayer de convertir directement si c'est déjà un nouveau statut
                try {
                    return Voyage.StatutVoyage.valueOf(statut);
                } catch (IllegalArgumentException e) {
                    // Si le statut n'existe pas, retourner CHARGEMENT par défaut
                    return Voyage.StatutVoyage.CHARGEMENT;
                }
        }
    }
}
