package com.backend.gesy.roles;

import java.util.List;
import java.util.Optional;

public interface RolesService {
    List<Roles> findAll();
    Optional<Roles> findById(Long id);
    Optional<Roles> findByNom(String nom);
    Roles save(Roles roles);
    Roles update(Long id, Roles roles);
    void deleteById(Long id);
}

