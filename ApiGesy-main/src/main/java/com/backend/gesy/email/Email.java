package com.backend.gesy.email;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "emails")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_name")
    private String from;

    @Column(name = "from_email", nullable = false)
    private String fromEmail;

    @Column(name = "to_email", nullable = false)
    private String toEmail;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String preview;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "date_sent", nullable = false)
    private LocalDateTime date;

    @Column(name = "is_read")
    private Boolean read = false;

    @Column(name = "is_starred")
    private Boolean starred = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FolderType folder = FolderType.INBOX;

    @ElementCollection
    @CollectionTable(name = "email_attachments", joinColumns = @JoinColumn(name = "email_id"))
    @Column(name = "attachment")
    private List<String> attachments = new ArrayList<>();

    @Column(name = "message_id")
    private String messageId; // ID du message IMAP/SMTP

    @Column(name = "in_reply_to")
    private String inReplyTo; // Pour les réponses

    @Column(name = "cc")
    private String cc; // Copie

    @Column(name = "bcc")
    private String bcc; // Copie cachée

    public enum FolderType {
        INBOX,
        SENT,
        DRAFT,
        TRASH
    }
}

