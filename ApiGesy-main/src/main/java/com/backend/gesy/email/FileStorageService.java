package com.backend.gesy.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads/email-attachments}")
    private String uploadDir;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("Répertoire de stockage créé: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Impossible de créer le répertoire de stockage.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        // Générer un nom de fichier unique
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

        try {
            // Vérifier le nom du fichier
            if (fileName.contains("..")) {
                throw new RuntimeException("Nom de fichier invalide: " + fileName);
            }

            // Copier le fichier vers le répertoire de stockage
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("Fichier stocké: {}", fileName);
            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Erreur lors du stockage du fichier " + fileName, ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("Fichier non trouvé: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Fichier non trouvé: " + fileName, ex);
        }
    }

    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
            log.info("Fichier supprimé: {}", fileName);
        } catch (IOException ex) {
            log.error("Erreur lors de la suppression du fichier: " + fileName, ex);
        }
    }
}

