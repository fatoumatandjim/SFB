package com.backend.gesy.email;

import com.backend.gesy.email.dto.EmailDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmailController {
    private final EmailService emailService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<List<EmailDTO>> getAllEmails() {
        return ResponseEntity.ok(emailService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailDTO> getEmailById(@PathVariable Long id) {
        return emailService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/folder/{folder}")
    public ResponseEntity<List<EmailDTO>> getEmailsByFolder(@PathVariable String folder) {
        return ResponseEntity.ok(emailService.findByFolder(folder));
    }

    @GetMapping("/folder/{folder}/unread")
    public ResponseEntity<List<EmailDTO>> getUnreadEmailsByFolder(@PathVariable String folder) {
        return ResponseEntity.ok(emailService.findUnreadByFolder(folder));
    }

    @GetMapping("/folder/{folder}/starred")
    public ResponseEntity<List<EmailDTO>> getStarredEmailsByFolder(@PathVariable String folder) {
        return ResponseEntity.ok(emailService.findStarredByFolder(folder));
    }

    @GetMapping("/folder/{folder}/count")
    public ResponseEntity<Long> countUnreadByFolder(@PathVariable String folder) {
        return ResponseEntity.ok(emailService.countUnreadByFolder(folder));
    }

    @GetMapping("/folder/{folder}/search")
    public ResponseEntity<List<EmailDTO>> searchInFolder(
            @PathVariable String folder,
            @RequestParam String query) {
        return ResponseEntity.ok(emailService.searchInFolder(query, folder));
    }

    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getFolderCounts() {
        return ResponseEntity.ok(emailService.getFolderCounts());
    }

    @PostMapping
    public ResponseEntity<EmailDTO> createEmail(@RequestBody EmailDTO emailDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(emailService.save(emailDTO));
    }

    @PostMapping("/send")
    public ResponseEntity<EmailDTO> sendEmail(@RequestBody EmailDTO emailDTO) {
        try {
            // Log des informations de l'email
            System.out.println("=== ENVOI EMAIL ===");
            System.out.println("Destinataire: " + emailDTO.getToEmail());
            System.out.println("Sujet: " + emailDTO.getSubject());
            System.out.println("Pièces jointes: " + (emailDTO.getAttachments() != null ? emailDTO.getAttachments() : "aucune"));
            
            if (emailDTO.getAttachments() != null && !emailDTO.getAttachments().isEmpty()) {
                System.out.println("Nombre de pièces jointes: " + emailDTO.getAttachments().size());
                for (String attachment : emailDTO.getAttachments()) {
                    System.out.println("  - " + attachment);
                }
            }
            
            EmailDTO sentEmail = emailService.sendEmail(emailDTO);
            return ResponseEntity.ok(sentEmail);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<List<EmailDTO>> syncEmails() {
        List<EmailDTO> newEmails = emailService.fetchNewEmails();
        return ResponseEntity.ok(newEmails);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailDTO> updateEmail(@PathVariable Long id, @RequestBody EmailDTO emailDTO) {
        return ResponseEntity.ok(emailService.update(id, emailDTO));
    }

    @PutMapping("/{id}/move/{folder}")
    public ResponseEntity<EmailDTO> moveToFolder(@PathVariable Long id, @PathVariable String folder) {
        return ResponseEntity.ok(emailService.moveToFolder(id, folder));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<EmailDTO> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(emailService.markAsRead(id));
    }

    @PutMapping("/{id}/unread")
    public ResponseEntity<EmailDTO> markAsUnread(@PathVariable Long id) {
        return ResponseEntity.ok(emailService.markAsUnread(id));
    }

    @PutMapping("/{id}/star")
    public ResponseEntity<EmailDTO> toggleStar(@PathVariable Long id) {
        return ResponseEntity.ok(emailService.toggleStar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmail(@PathVariable Long id) {
        emailService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Upload de fichiers
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = fileStorageService.storeFile(file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("originalName", file.getOriginalFilename());
            response.put("size", file.getSize());
            response.put("contentType", file.getContentType());
            response.put("downloadUri", "/api/emails/download/" + fileName);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Upload multiple fichiers
    @PostMapping("/upload-multiple")
    public ResponseEntity<List<Map<String, Object>>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        List<Map<String, Object>> responses = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                String fileName = fileStorageService.storeFile(file);
                
                Map<String, Object> response = new HashMap<>();
                response.put("fileName", fileName);
                response.put("originalName", file.getOriginalFilename());
                response.put("size", file.getSize());
                response.put("contentType", file.getContentType());
                response.put("downloadUri", "/api/emails/download/" + fileName);
                
                responses.add(response);
            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("originalName", file.getOriginalFilename());
                errorResponse.put("error", e.getMessage());
                responses.add(errorResponse);
            }
        }
        
        return ResponseEntity.ok(responses);
    }

    // Téléchargement de fichiers
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        try {
            Resource resource = fileStorageService.loadFileAsResource(fileName);
            
            String contentType = null;
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            } catch (IOException ex) {
                contentType = "application/octet-stream";
            }
            
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Test des fichiers uploadés
    @GetMapping("/test-files")
    public ResponseEntity<Map<String, Object>> testFiles() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            java.nio.file.Path uploadPath = fileStorageService.getFileStorageLocation();
            result.put("uploadDirectory", uploadPath.toAbsolutePath().toString());
            result.put("directoryExists", java.nio.file.Files.exists(uploadPath));
            result.put("directoryReadable", java.nio.file.Files.isReadable(uploadPath));
            result.put("directoryWritable", java.nio.file.Files.isWritable(uploadPath));
            
            // Lister les fichiers
            List<String> files = new ArrayList<>();
            if (java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.list(uploadPath).forEach(path -> {
                    java.io.File file = path.toFile();
                    files.add(file.getName() + " (" + file.length() + " bytes)");
                });
            }
            result.put("files", files);
            result.put("fileCount", files.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    // Test d'envoi avec pièce jointe
    @PostMapping("/test-send-attachment")
    public ResponseEntity<Map<String, Object>> testSendWithAttachment(
            @RequestParam String to,
            @RequestParam(required = false) String fileName) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Créer un email de test
            EmailDTO emailDTO = new EmailDTO();
            emailDTO.setToEmail(to);
            emailDTO.setSubject("Test pièce jointe - GesY");
            emailDTO.setContent("Ceci est un email de test avec pièce jointe envoyé depuis GesY.\n\nSi vous recevez ce fichier, l'envoi de pièces jointes fonctionne correctement.");
            
            if (fileName != null && !fileName.isEmpty()) {
                List<String> attachments = new ArrayList<>();
                attachments.add(fileName);
                emailDTO.setAttachments(attachments);
                
                // Vérifier que le fichier existe
                java.nio.file.Path filePath = fileStorageService.getFileStorageLocation().resolve(fileName).normalize();
                java.io.File file = filePath.toFile();
                
                result.put("fileExists", file.exists());
                result.put("fileSize", file.length());
                result.put("filePath", filePath.toAbsolutePath().toString());
                result.put("fileReadable", file.canRead());
            }
            
            // Envoyer l'email
            EmailDTO sent = emailService.sendEmail(emailDTO);
            
            result.put("success", true);
            result.put("emailId", sent.getId());
            result.put("message", "Email envoyé avec succès");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(result);
        }
    }
}

