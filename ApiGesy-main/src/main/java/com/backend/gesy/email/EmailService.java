package com.backend.gesy.email;

import com.backend.gesy.email.dto.EmailDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EmailService {
    // CRUD de base
    List<EmailDTO> findAll();
    Optional<EmailDTO> findById(Long id);
    EmailDTO save(EmailDTO emailDTO);
    EmailDTO update(Long id, EmailDTO emailDTO);
    void deleteById(Long id);
    
    // Gestion des dossiers
    List<EmailDTO> findByFolder(String folder);
    EmailDTO moveToFolder(Long id, String folder);
    
    // Gestion de la lecture
    EmailDTO markAsRead(Long id);
    EmailDTO markAsUnread(Long id);
    List<EmailDTO> findUnreadByFolder(String folder);
    Long countUnreadByFolder(String folder);
    
    // Gestion des favoris
    EmailDTO toggleStar(Long id);
    List<EmailDTO> findStarredByFolder(String folder);
    
    // Recherche
    List<EmailDTO> searchInFolder(String query, String folder);
    
    // Envoi d'email
    EmailDTO sendEmail(EmailDTO emailDTO);
    
    // Synchronisation avec le serveur mail
    List<EmailDTO> fetchNewEmails();
    void syncEmails();
    
    // Statistiques
    Map<String, Long> getFolderCounts();
}

