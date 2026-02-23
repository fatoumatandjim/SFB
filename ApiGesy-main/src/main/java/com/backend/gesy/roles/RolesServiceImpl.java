package com.backend.gesy.roles;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class RolesServiceImpl implements RolesService {
    private final RolesRepository rolesRepository;

    @Override
    public List<Roles> findAll() {
        return rolesRepository.findAll();
    }

    @Override
    public Optional<Roles> findById(Long id) {
        return rolesRepository.findById(id);
    }

    @Override
    public Optional<Roles> findByNom(String nom) {
        return rolesRepository.findByNom(nom);
    }

    @Override
    public Roles save(Roles roles) {
        return rolesRepository.save(roles);
    }

    @Override
    public Roles update(Long id, Roles roles) {
        Roles existingRoles = rolesRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rôle non trouvé avec l'id: " + id));
        roles.setId(existingRoles.getId());
        return rolesRepository.save(roles);
    }

    @Override
    public void deleteById(Long id) {
        rolesRepository.deleteById(id);
    }
}

