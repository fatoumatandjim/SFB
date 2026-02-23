package com.backend.gesy.email.dto;

import com.backend.gesy.email.Email;
import org.springframework.stereotype.Component;

@Component
public class EmailMapper {

    public EmailDTO toDTO(Email email) {
        if (email == null) {
            return null;
        }

        EmailDTO dto = new EmailDTO();
        dto.setId(email.getId());
        dto.setFrom(email.getFrom());
        dto.setFromEmail(email.getFromEmail());
        dto.setToEmail(email.getToEmail());
        dto.setSubject(email.getSubject());
        dto.setPreview(email.getPreview());
        dto.setContent(email.getContent());
        dto.setDate(email.getDate());
        dto.setRead(email.getRead());
        dto.setStarred(email.getStarred());
        dto.setFolder(email.getFolder() != null ? email.getFolder().name().toLowerCase() : "inbox");
        dto.setAttachments(email.getAttachments());
        dto.setMessageId(email.getMessageId());
        dto.setInReplyTo(email.getInReplyTo());
        dto.setCc(email.getCc());
        dto.setBcc(email.getBcc());

        return dto;
    }

    public Email toEntity(EmailDTO dto) {
        if (dto == null) {
            return null;
        }

        Email email = new Email();
        email.setId(dto.getId());
        email.setFrom(dto.getFrom());
        email.setFromEmail(dto.getFromEmail());
        email.setToEmail(dto.getToEmail());
        email.setSubject(dto.getSubject());
        email.setPreview(dto.getPreview());
        email.setContent(dto.getContent());
        email.setDate(dto.getDate());
        email.setRead(dto.getRead() != null ? dto.getRead() : false);
        email.setStarred(dto.getStarred() != null ? dto.getStarred() : false);
        
        if (dto.getFolder() != null) {
            email.setFolder(Email.FolderType.valueOf(dto.getFolder().toUpperCase()));
        } else {
            email.setFolder(Email.FolderType.INBOX);
        }
        
        email.setAttachments(dto.getAttachments());
        email.setMessageId(dto.getMessageId());
        email.setInReplyTo(dto.getInReplyTo());
        email.setCc(dto.getCc());
        email.setBcc(dto.getBcc());

        return email;
    }
}

