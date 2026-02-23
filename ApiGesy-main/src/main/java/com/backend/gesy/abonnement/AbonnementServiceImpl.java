package com.backend.gesy.abonnement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AbonnementServiceImpl implements AbonnementService {
    private final AbonnementRepository abonnementRepository;

    @Override
    public List<Abonnement> findAll() {
        return abonnementRepository.findAll();
    }

    @Override
    public Optional<Abonnement> findById(Long id) {
        return abonnementRepository.findById(id);
    }

    @Override
    public List<Abonnement> findByActif(Boolean actif) {
        return abonnementRepository.findByActif(actif);
    }

    @Override
    public Abonnement save(Abonnement abonnement) {
        return abonnementRepository.save(abonnement);
    }

    @Override
    public Abonnement update(Long id, Abonnement abonnement) {
        Abonnement existingAbonnement = abonnementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Abonnement non trouvé avec l'id: " + id));
        abonnement.setId(existingAbonnement.getId());
        return abonnementRepository.save(abonnement);
    }

    @Override
    public void deleteById(Long id) {
        abonnementRepository.deleteById(id);
    }
}

