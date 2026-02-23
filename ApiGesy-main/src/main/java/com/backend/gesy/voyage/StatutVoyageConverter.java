package com.backend.gesy.voyage;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StatutVoyageConverter implements AttributeConverter<Voyage.StatutVoyage, String> {

    @Override
    public String convertToDatabaseColumn(Voyage.StatutVoyage statut) {
        if (statut == null) {
            return null;
        }
        return statut.name();
    }

    @Override
    public Voyage.StatutVoyage convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        
        // Essayer d'abord de convertir directement (nouveaux statuts)
        try {
            return Voyage.StatutVoyage.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            // Si échec, convertir les anciens statuts vers les nouveaux
            return convertOldStatutToNew(dbData);
        }
    }

    /**
     * Convertit un ancien statut vers le nouveau format
     */
    private Voyage.StatutVoyage convertOldStatutToNew(String oldStatut) {
        switch (oldStatut) {
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
                // Si aucun mapping trouvé, retourner CHARGEMENT par défaut
                return Voyage.StatutVoyage.CHARGEMENT;
        }
    }
}

