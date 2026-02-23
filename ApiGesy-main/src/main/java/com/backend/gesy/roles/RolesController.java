package com.backend.gesy.roles;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RolesController {
    private final RolesService rolesService;

    @GetMapping
    public ResponseEntity<List<Roles>> getAllRoles() {
        return ResponseEntity.ok(rolesService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Roles> getRoleById(@PathVariable Long id) {
        return rolesService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Roles> createRole(@RequestBody Roles roles) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolesService.save(roles));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Roles> updateRole(@PathVariable Long id, @RequestBody Roles roles) {
        return ResponseEntity.ok(rolesService.update(id, roles));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        rolesService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

