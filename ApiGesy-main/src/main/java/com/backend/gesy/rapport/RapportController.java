package com.backend.gesy.rapport;

import com.backend.gesy.rapport.dto.RapportFinancierDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/rapports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RapportController {
    private final RapportService rapportService;

    @GetMapping("/financier")
    public ResponseEntity<RapportFinancierDTO> getRapportFinancier(
            @RequestParam(required = false) String periode,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(rapportService.getRapportFinancier(periode, annee, dateDebut, dateFin));
    }
}

