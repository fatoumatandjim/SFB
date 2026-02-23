package com.backend.gesy.categoriedepense;

import com.backend.gesy.categoriedepense.dto.CategorieDepenseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories-depenses")
@RequiredArgsConstructor
public class CategorieDepenseController {
    
    private final CategorieDepenseService categorieDepenseService;

    @GetMapping
    public ResponseEntity<List<CategorieDepenseDTO>> getAll() {
        return ResponseEntity.ok(categorieDepenseService.findAll());
    }

    @GetMapping("/actives")
    public ResponseEntity<List<CategorieDepenseDTO>> getAllActives() {
        return ResponseEntity.ok(categorieDepenseService.findAllActives());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategorieDepenseDTO> getById(@PathVariable Long id) {
        return categorieDepenseService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CategorieDepenseDTO> create(@RequestBody CategorieDepenseDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categorieDepenseService.save(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategorieDepenseDTO> update(@PathVariable Long id, @RequestBody CategorieDepenseDTO dto) {
        return ResponseEntity.ok(categorieDepenseService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categorieDepenseService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

