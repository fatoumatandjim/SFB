package com.backend.gesy.capitale;

import com.backend.gesy.capitale.dto.CapitaleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/capitale")
@CrossOrigin(origins = "*")
public class CapitaleController {
    @Autowired
    private  CapitaleService capitaleService;

    @GetMapping
    public ResponseEntity<CapitaleDTO> getCapitale() {
        CapitaleDTO capitale = capitaleService.calculateCapitale();
        return ResponseEntity.ok(capitale);
    }

    @GetMapping("/month")
    public ResponseEntity<CapitaleDTO> getCapitaleByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        CapitaleDTO capitale = capitaleService.calculateCapitaleByMonth(year, month);
        return ResponseEntity.ok(capitale);
    }

    @GetMapping("/range")
    public ResponseEntity<CapitaleDTO> getCapitaleByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        CapitaleDTO capitale = capitaleService.calculateCapitaleByDateRange(startDate, endDate);
        return ResponseEntity.ok(capitale);
    }
}
