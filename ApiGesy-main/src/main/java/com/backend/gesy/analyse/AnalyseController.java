package com.backend.gesy.analyse;

import com.backend.gesy.analyse.dto.AnalyseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyseController {
    private final AnalyseService analyseService;

    @GetMapping
    public ResponseEntity<AnalyseDTO> getAnalyse(
            @RequestParam(required = false) String periode,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(analyseService.getAnalyse(periode, annee, dateDebut, dateFin));
    }
}

