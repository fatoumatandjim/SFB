package com.backend.gesy.finance.justificatif;

import com.backend.gesy.finance.justificatif.dto.JustificatifFinancierDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/justificatifs-financiers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JustificatifFinancierController {

    private final JustificatifFinancierService justificatifFinancierService;

    @GetMapping
    public ResponseEntity<List<JustificatifFinancierDTO>> list(
            @RequestParam String ownerType,
            @RequestParam Long ownerId) {
        return ResponseEntity.ok(justificatifFinancierService.list(ownerType, ownerId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<JustificatifFinancierDTO>> upload(
            @RequestParam String ownerType,
            @RequestParam Long ownerId,
            @RequestParam("files") MultipartFile[] files) {
        return ResponseEntity.ok(justificatifFinancierService.upload(ownerType, ownerId, files));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        JustificatifFinancier j = justificatifFinancierService.findEntityOrThrow(id);
        Resource resource = justificatifFinancierService.loadAsResource(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + j.getOriginalFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        justificatifFinancierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
