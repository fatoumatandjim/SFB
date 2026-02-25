package com.backend.gesy.axe;

import com.backend.gesy.pays.Pays;
import com.backend.gesy.pays.PaysRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Order(10)
@Slf4j
public class AxeDataInitializer implements CommandLineRunner {

    private final AxeRepository axeRepository;
    private final PaysRepository paysRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (paysRepository.count() > 0) {
            migrateExistingAxes();
            return;
        }

        log.info("Initialisation des pays et axes par défaut...");

        Pays senegal = createPays("Sénégal", new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("150000"));
        Pays coteIvoire = createPays("Côte d'Ivoire", new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("150000"));

        createAxe("Diboli", senegal);
        createAxe("Moussala", senegal);
        createAxe("Kadiana", coteIvoire);
        createAxe("Zegoua", coteIvoire);

        log.info("2 pays et 4 axes par défaut créés avec succès.");
    }

    private Pays createPays(String nom, BigDecimal fraisParLitre, BigDecimal fraisParLitreGasoil, BigDecimal fraisT1) {
        Pays pays = new Pays();
        pays.setNom(nom);
        pays.setFraisParLitre(fraisParLitre);
        pays.setFraisParLitreGasoil(fraisParLitreGasoil);
        pays.setFraisT1(fraisT1);
        return paysRepository.save(pays);
    }

    private void createAxe(String nom, Pays pays) {
        if (!axeRepository.existsByNom(nom)) {
            Axe axe = new Axe();
            axe.setNom(nom);
            axe.setPays(pays);
            axeRepository.save(axe);
        }
    }

    private void migrateExistingAxes() {
        for (Axe axe : axeRepository.findAll()) {
            if (axe.getPays() != null) continue;
            String nom = axe.getNom().toLowerCase().trim();
            Pays pays = null;
            if (nom.equals("diboli") || nom.equals("moussala")) {
                pays = paysRepository.findByNom("Sénégal").orElse(null);
            } else if (nom.equals("kadiana") || nom.equals("zegoua")) {
                pays = paysRepository.findByNom("Côte d'Ivoire").orElse(null);
            }
            if (pays != null) {
                axe.setPays(pays);
                axeRepository.save(axe);
            }
        }
    }
}
