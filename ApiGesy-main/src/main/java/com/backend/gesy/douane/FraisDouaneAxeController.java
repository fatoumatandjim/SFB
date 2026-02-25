package com.backend.gesy.douane;

import com.backend.gesy.douane.dto.FraisDouaneAxeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/douane/frais-axe")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FraisDouaneAxeController {
    private final FraisDouaneAxeService fraisDouaneAxeService;

    @GetMapping
    public ResponseEntity<List<FraisDouaneAxeDTO>> findAll() {
        return ResponseEntity.ok(fraisDouaneAxeService.findAll());
    }

    @GetMapping("/axe/{axeId}")
    public ResponseEntity<FraisDouaneAxeDTO> findByAxeId(@PathVariable Long axeId) {
        return fraisDouaneAxeService.findByAxeId(axeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FraisDouaneAxeDTO> findById(@PathVariable Long id) {
        return fraisDouaneAxeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FraisDouaneAxeDTO> create(@RequestBody FraisDouaneAxeDTO dto) {
        try {
            FraisDouaneAxeDTO saved = fraisDouaneAxeService.save(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<FraisDouaneAxeDTO> update(@PathVariable Long id, @RequestBody FraisDouaneAxeDTO dto) {
        try {
            return ResponseEntity.ok(fraisDouaneAxeService.update(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            fraisDouaneAxeService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
