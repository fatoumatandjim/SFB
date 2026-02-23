package com.backend.gesy.axe;

import com.backend.gesy.axe.dto.AxeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/axes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AxeController {
    
    private final AxeService axeService;

    @GetMapping
    public ResponseEntity<List<AxeDTO>> findAll() {
        List<AxeDTO> axes = axeService.findAll();
        return ResponseEntity.ok(axes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AxeDTO> findById(@PathVariable Long id) {
        Optional<AxeDTO> axe = axeService.findById(id);
        return axe.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AxeDTO> save(@RequestBody AxeDTO dto) {
        try {
            AxeDTO saved = axeService.save(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<AxeDTO> update(@PathVariable Long id, @RequestBody AxeDTO dto) {
        try {
            AxeDTO updated = axeService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        try {
            axeService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

