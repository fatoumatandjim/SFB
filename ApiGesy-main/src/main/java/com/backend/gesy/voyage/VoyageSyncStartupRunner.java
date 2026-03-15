package com.backend.gesy.voyage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Au démarrage, synchronise le statut des voyages déclarés avec état "Décharger" validé
 * en mettant statut = DECHARGER (une seule source de vérité backend).
 */
@Component
@Order(100)
public class VoyageSyncStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VoyageSyncStartupRunner.class);

    private final VoyageService voyageService;

    public VoyageSyncStartupRunner(VoyageService voyageService) {
        this.voyageService = voyageService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int synced = voyageService.syncStatutDechargerForDeclarerValides();
            if (synced > 0) {
                log.info("VoyageSyncStartupRunner: {} voyage(s) synchronisé(s) (statut DECHARGER).", synced);
            }
        } catch (Exception e) {
            log.warn("VoyageSyncStartupRunner: sync statut DECHARGER ignorée: {}", e.getMessage());
        }
    }
}
