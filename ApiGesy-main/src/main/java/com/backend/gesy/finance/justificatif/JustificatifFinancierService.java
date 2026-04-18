package com.backend.gesy.finance.justificatif;

import com.backend.gesy.email.FileStorageService;
import com.backend.gesy.finance.justificatif.dto.JustificatifFinancierDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class JustificatifFinancierService {

    private final JustificatifFinancierRepository repository;
    private final FileStorageService fileStorageService;

    private static void validateOwnerType(String ownerType) {
        if (ownerType == null || ownerType.isBlank()) {
            throw new RuntimeException("ownerType requis (DEPENSE, PAIEMENT, TRANSACTION)");
        }
        String u = ownerType.trim().toUpperCase(Locale.ROOT);
        if (!JustificatifFinancier.OWNER_DEPENSE.equals(u)
                && !JustificatifFinancier.OWNER_PAIEMENT.equals(u)
                && !JustificatifFinancier.OWNER_TRANSACTION.equals(u)) {
            throw new RuntimeException("ownerType invalide: " + ownerType);
        }
    }

    public List<JustificatifFinancierDTO> list(String ownerType, Long ownerId) {
        validateOwnerType(ownerType);
        if (ownerId == null || ownerId <= 0) {
            throw new RuntimeException("ownerId requis");
        }
        String u = ownerType.trim().toUpperCase(Locale.ROOT);
        return repository.findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(u, ownerId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<JustificatifFinancierDTO> upload(String ownerType, Long ownerId, MultipartFile[] files) {
        validateOwnerType(ownerType);
        if (ownerId == null || ownerId <= 0) {
            throw new RuntimeException("ownerId requis");
        }
        if (files == null || files.length == 0) {
            throw new RuntimeException("Aucun fichier fourni");
        }
        String u = ownerType.trim().toUpperCase(Locale.ROOT);
        List<JustificatifFinancierDTO> out = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String stored = fileStorageService.storeFile(file);
            JustificatifFinancier j = new JustificatifFinancier();
            j.setOwnerType(u);
            j.setOwnerId(ownerId);
            j.setStoredFileName(stored);
            j.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : stored);
            j.setCreatedAt(LocalDateTime.now());
            out.add(toDto(repository.save(j)));
        }
        if (out.isEmpty()) {
            throw new RuntimeException("Aucun fichier valide fourni");
        }
        return out;
    }

    public void delete(Long id) {
        JustificatifFinancier j = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Justificatif non trouvé avec l'id: " + id));
        fileStorageService.deleteFile(j.getStoredFileName());
        repository.delete(j);
    }

    public Resource loadAsResource(Long id) {
        JustificatifFinancier j = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Justificatif non trouvé"));
        return fileStorageService.loadFileAsResource(j.getStoredFileName());
    }

    public JustificatifFinancier findEntityOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Justificatif non trouvé"));
    }

    private JustificatifFinancierDTO toDto(JustificatifFinancier e) {
        return new JustificatifFinancierDTO(
                e.getId(),
                e.getOwnerType(),
                e.getOwnerId(),
                e.getStoredFileName(),
                e.getOriginalFilename(),
                e.getCreatedAt());
    }
}
