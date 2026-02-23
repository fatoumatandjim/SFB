package com.backend.gesy.client;

import com.backend.gesy.client.dto.ClientDTO;

import java.util.List;
import java.util.Optional;

public interface ClientService {
    List<ClientDTO> findAll();
    List<ClientDTO> findClientsWithAtLeastOneAchat();
    Optional<ClientDTO> findById(Long id);
    Optional<ClientDTO> findByEmail(String email);
    Optional<ClientDTO> findByCodeClient(String codeClient);
    ClientDTO save(ClientDTO clientDTO);
    ClientDTO update(Long id, ClientDTO clientDTO);
    void deleteById(Long id);
}

