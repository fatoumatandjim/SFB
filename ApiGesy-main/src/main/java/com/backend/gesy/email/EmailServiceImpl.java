package com.backend.gesy.email;

import com.backend.gesy.email.dto.EmailDTO;
import com.backend.gesy.email.dto.EmailMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmailServiceImpl implements EmailService {
    private final EmailRepository emailRepository;
    private final EmailMapper emailMapper;
    private final JavaMailSender mailSender;
    private final FileStorageService fileStorageService;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.imap.port:993}")
    private String imapPort;

    @Value("${spring.mail.password}")
    private String mailPassword;

    @Override
    public List<EmailDTO> findAll() {
        return emailRepository.findAll().stream()
            .map(emailMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<EmailDTO> findById(Long id) {
        return emailRepository.findById(id)
            .map(emailMapper::toDTO);
    }

    @Override
    public EmailDTO save(EmailDTO emailDTO) {
        if (emailDTO.getDate() == null) {
            emailDTO.setDate(LocalDateTime.now());
        }
        if (emailDTO.getPreview() == null && emailDTO.getContent() != null) {
            emailDTO.setPreview(generatePreview(emailDTO.getContent()));
        }
        Email email = emailMapper.toEntity(emailDTO);
        Email savedEmail = emailRepository.save(email);
        return emailMapper.toDTO(savedEmail);
    }

    @Override
    public EmailDTO update(Long id, EmailDTO emailDTO) {
        Email existingEmail = emailRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Email non trouvé avec l'id: " + id));
        
        Email email = emailMapper.toEntity(emailDTO);
        email.setId(existingEmail.getId());
        
        Email updatedEmail = emailRepository.save(email);
        return emailMapper.toDTO(updatedEmail);
    }

    @Override
    public void deleteById(Long id) {
        Email email = emailRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Email non trouvé avec l'id: " + id));
        
        if (email.getFolder() == Email.FolderType.TRASH) {
            // Suppression définitive si déjà dans la corbeille
            emailRepository.deleteById(id);
        } else {
            // Déplacement vers la corbeille
            email.setFolder(Email.FolderType.TRASH);
            emailRepository.save(email);
        }
    }

    @Override
    public List<EmailDTO> findByFolder(String folder) {
        Email.FolderType folderType = Email.FolderType.valueOf(folder.toUpperCase());
        return emailRepository.findByFolderOrderByDateDesc(folderType).stream()
            .map(emailMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public EmailDTO moveToFolder(Long id, String folder) {
        Email email = emailRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Email non trouvé avec l'id: " + id));
        
        email.setFolder(Email.FolderType.valueOf(folder.toUpperCase()));
        Email updatedEmail = emailRepository.save(email);
        return emailMapper.toDTO(updatedEmail);
    }

    @Override
    public EmailDTO markAsRead(Long id) {
        Email email = emailRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Email non trouvé avec l'id: " + id));
        
        email.setRead(true);
        Email updatedEmail = emailRepository.save(email);
        return emailMapper.toDTO(updatedEmail);
    }

    @Override
    public EmailDTO markAsUnread(Long id) {
        Email email = emailRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Email non trouvé avec l'id: " + id));
        
        email.setRead(false);
        Email updatedEmail = emailRepository.save(email);
        return emailMapper.toDTO(updatedEmail);
    }

    @Override
    public List<EmailDTO> findUnreadByFolder(String folder) {
        Email.FolderType folderType = Email.FolderType.valueOf(folder.toUpperCase());
        return emailRepository.findUnreadByFolder(folderType).stream()
            .map(emailMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Long countUnreadByFolder(String folder) {
        Email.FolderType folderType = Email.FolderType.valueOf(folder.toUpperCase());
        return emailRepository.countUnreadByFolder(folderType);
    }

    @Override
    public EmailDTO toggleStar(Long id) {
        Email email = emailRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Email non trouvé avec l'id: " + id));
        
        email.setStarred(!email.getStarred());
        Email updatedEmail = emailRepository.save(email);
        return emailMapper.toDTO(updatedEmail);
    }

    @Override
    public List<EmailDTO> findStarredByFolder(String folder) {
        Email.FolderType folderType = Email.FolderType.valueOf(folder.toUpperCase());
        return emailRepository.findStarredByFolder(folderType).stream()
            .map(emailMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<EmailDTO> searchInFolder(String query, String folder) {
        Email.FolderType folderType = Email.FolderType.valueOf(folder.toUpperCase());
        return emailRepository.searchInFolder(query, folderType).stream()
            .map(emailMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public EmailDTO sendEmail(EmailDTO emailDTO) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // Activer multipart pour supporter les pièces jointes
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailUsername);
            helper.setTo(emailDTO.getToEmail());
            helper.setSubject(emailDTO.getSubject());
            
            // Définir le contenu du message AVANT d'attacher les fichiers
            helper.setText(emailDTO.getContent(), false);

            // Copie (CC) : plusieurs adresses séparées par virgule, point-virgule ou espace
            if (emailDTO.getCc() != null && !emailDTO.getCc().trim().isEmpty()) {
                String[] ccAddresses = parseEmailAddresses(emailDTO.getCc());
                if (ccAddresses.length > 0) {
                    helper.setCc(ccAddresses);
                }
            }

            // Copie cachée (BCC) : plusieurs adresses séparées par virgule, point-virgule ou espace
            if (emailDTO.getBcc() != null && !emailDTO.getBcc().trim().isEmpty()) {
                String[] bccAddresses = parseEmailAddresses(emailDTO.getBcc());
                if (bccAddresses.length > 0) {
                    helper.setBcc(bccAddresses);
                }
            }

            // Attacher les fichiers APRÈS setText() avec FileSystemResource
            if (emailDTO.getAttachments() != null && !emailDTO.getAttachments().isEmpty()) {
                log.info("Nombre de pièces jointes à attacher: {}", emailDTO.getAttachments().size());
                
                for (String fileName : emailDTO.getAttachments()) {
                    try {
                        // Récupérer le chemin complet du fichier
                        java.nio.file.Path filePath = fileStorageService.getFileStorageLocation().resolve(fileName).normalize();
                        java.io.File file = filePath.toFile();
                        
                        // Vérifier que le fichier existe et est lisible
                        if (!file.exists()) {
                            log.error("Fichier non trouvé: {} (chemin: {})", fileName, filePath.toAbsolutePath());
                            continue;
                        }
                        
                        if (!file.canRead()) {
                            log.error("Fichier non lisible: {} (chemin: {})", fileName, filePath.toAbsolutePath());
                            continue;
                        }
                        
                        // Utiliser FileSystemResource pour garantir l'accès au fichier
                        org.springframework.core.io.FileSystemResource fileResource = 
                            new org.springframework.core.io.FileSystemResource(file);
                        
                        // Extraire le nom original du fichier (après l'UUID_)
                        String originalFileName = fileName;
                        if (fileName.contains("_")) {
                            originalFileName = fileName.substring(fileName.indexOf("_") + 1);
                        }
                        
                        // Attacher le fichier avec son nom original
                        helper.addAttachment(originalFileName, fileResource);
                        
                        log.info("Pièce jointe attachée avec succès: {} (taille: {} bytes)", 
                                originalFileName, file.length());
                        
                    } catch (Exception ex) {
                        log.error("Erreur lors de l'attachement du fichier: " + fileName, ex);
                        // Ne pas interrompre l'envoi si un fichier pose problème
                    }
                }
            }

            // Envoyer l'email avec les pièces jointes
            mailSender.send(message);
            log.info("Email envoyé avec succès à: {} (avec {} pièce(s) jointe(s))", 
                    emailDTO.getToEmail(), 
                    emailDTO.getAttachments() != null ? emailDTO.getAttachments().size() : 0);

            // Sauvegarder l'email envoyé dans la base de données
            emailDTO.setFromEmail(mailUsername);
            emailDTO.setFrom(mailUsername);
            emailDTO.setFolder("sent");
            emailDTO.setDate(LocalDateTime.now());
            emailDTO.setRead(true);
            emailDTO.setPreview(generatePreview(emailDTO.getContent()));

            return save(emailDTO);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email", e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
    }

    /**
     * Parse une chaîne d'adresses email (séparées par virgule, point-virgule ou espace) en tableau.
     */
    private static String[] parseEmailAddresses(String addresses) {
        if (addresses == null || addresses.trim().isEmpty()) {
            return new String[0];
        }
        return Arrays.stream(addresses.split("[,\\s;]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public List<EmailDTO> fetchNewEmails() {
        List<EmailDTO> newEmails = new ArrayList<>();
        
        try {
            Properties props = new Properties();
            props.setProperty("mail.store.protocol", "imaps");
            props.setProperty("mail.imaps.host", mailHost);
            props.setProperty("mail.imaps.port", imapPort);
            props.setProperty("mail.imaps.ssl.enable", "true");

            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            store.connect(mailHost, mailUsername, mailPassword);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // Récupérer seulement les 50 derniers messages
            Message[] messages = inbox.getMessages();
            int start = Math.max(1, messages.length - 49);
            Message[] recentMessages = inbox.getMessages(start, messages.length);

            for (Message message : recentMessages) {
                try {
                    String messageId = getMessageId(message);
                    
                    // Vérifier si le message existe déjà
                    if (messageId != null && emailRepository.findByMessageId(messageId).isPresent()) {
                        continue;
                    }

                    EmailDTO emailDTO = new EmailDTO();
                    emailDTO.setMessageId(messageId);
                    emailDTO.setFromEmail(getFromEmail(message));
                    emailDTO.setFrom(getFromName(message));
                    emailDTO.setSubject(message.getSubject());
                    emailDTO.setContent(getTextFromMessage(message));
                    emailDTO.setPreview(generatePreview(emailDTO.getContent()));
                    emailDTO.setDate(message.getSentDate() != null 
                        ? message.getSentDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : LocalDateTime.now());
                    emailDTO.setRead(false);
                    emailDTO.setStarred(false);
                    emailDTO.setFolder("inbox");
                    emailDTO.setToEmail(mailUsername);

                    EmailDTO savedEmail = save(emailDTO);
                    newEmails.add(savedEmail);
                } catch (Exception e) {
                    log.error("Erreur lors du traitement d'un message", e);
                }
            }

            inbox.close(false);
            store.close();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des emails", e);
        }

        return newEmails;
    }

    @Override
    public void syncEmails() {
        fetchNewEmails();
    }

    @Override
    public Map<String, Long> getFolderCounts() {
        Map<String, Long> counts = new HashMap<>();
        
        for (Email.FolderType folder : Email.FolderType.values()) {
            if (folder == Email.FolderType.INBOX) {
                // Pour inbox, compter seulement les non lus
                counts.put(folder.name().toLowerCase(), countUnreadByFolder(folder.name()));
            } else {
                // Pour les autres, compter tous les emails
                counts.put(folder.name().toLowerCase(), (long) findByFolder(folder.name()).size());
            }
        }
        
        return counts;
    }

    // Méthodes utilitaires privées
    private String generatePreview(String content) {
        if (content == null) return "";
        String cleanContent = content.replaceAll("\\s+", " ").trim();
        return cleanContent.length() > 100 ? cleanContent.substring(0, 100) + "..." : cleanContent;
    }

    private String getMessageId(Message message) throws MessagingException {
        String[] headers = message.getHeader("Message-ID");
        return (headers != null && headers.length > 0) ? headers[0] : null;
    }

    private String getFromEmail(Message message) throws MessagingException {
        Address[] from = message.getFrom();
        if (from != null && from.length > 0) {
            return from[0].toString().replaceAll(".*<(.+)>.*", "$1");
        }
        return "unknown@unknown.com";
    }

    private String getFromName(Message message) throws MessagingException {
        Address[] from = message.getFrom();
        if (from != null && from.length > 0) {
            String fullAddress = from[0].toString();
            if (fullAddress.contains("<")) {
                return fullAddress.substring(0, fullAddress.indexOf("<")).trim();
            }
            return fullAddress;
        }
        return "Unknown";
    }

    private String getTextFromMessage(Message message) throws Exception {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            result = getTextFromMultipart(multipart);
        }
        return result;
    }

    private String getTextFromMultipart(Multipart multipart) throws Exception {
        StringBuilder result = new StringBuilder();
        int count = multipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
                break;
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result.append(html.replaceAll("<[^>]*>", ""));
            } else if (bodyPart.getContent() instanceof Multipart) {
                result.append(getTextFromMultipart((Multipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }
}

