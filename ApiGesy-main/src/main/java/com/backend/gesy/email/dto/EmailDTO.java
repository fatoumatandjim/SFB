package com.backend.gesy.email.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailDTO {
    private Long id;
    private String from;
    private String fromEmail;
    private String toEmail;
    private String subject;
    private String preview;
    private String content;
    private LocalDateTime date;
    private Boolean read;
    private Boolean starred;
    private String folder; // "inbox", "sent", "draft", "trash"
    private List<String> attachments = new ArrayList<>();
    private String messageId;
    private String inReplyTo;
    private String cc;
    private String bcc;
}

