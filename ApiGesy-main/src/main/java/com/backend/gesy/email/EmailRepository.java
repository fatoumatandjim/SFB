package com.backend.gesy.email;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {
    List<Email> findByFolder(Email.FolderType folder);
    List<Email> findByFolderOrderByDateDesc(Email.FolderType folder);
    List<Email> findByFromEmailContainingIgnoreCase(String fromEmail);
    List<Email> findBySubjectContainingIgnoreCase(String subject);
    Optional<Email> findByMessageId(String messageId);
    
    @Query("SELECT e FROM Email e WHERE e.folder = :folder AND e.read = false")
    List<Email> findUnreadByFolder(@Param("folder") Email.FolderType folder);
    
    @Query("SELECT e FROM Email e WHERE e.folder = :folder AND e.starred = true")
    List<Email> findStarredByFolder(@Param("folder") Email.FolderType folder);
    
    @Query("SELECT COUNT(e) FROM Email e WHERE e.folder = :folder AND e.read = false")
    Long countUnreadByFolder(@Param("folder") Email.FolderType folder);
    
    @Query("SELECT e FROM Email e WHERE " +
           "(LOWER(e.subject) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.from) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.fromEmail) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.content) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "e.folder = :folder")
    List<Email> searchInFolder(@Param("query") String query, @Param("folder") Email.FolderType folder);
}

