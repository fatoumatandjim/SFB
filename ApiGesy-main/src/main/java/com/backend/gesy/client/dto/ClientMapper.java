package com.backend.gesy.client.dto;

import com.backend.gesy.client.Client;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {
    public ClientDTO toDTO(Client client) {
        if (client == null) {
            return null;
        }
        
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setNom(client.getNom());
        dto.setEmail(client.getEmail());
        dto.setTelephone(client.getTelephone());
        dto.setAdresse(client.getAdresse());
        dto.setType(client.getType() != null ? client.getType().name() : null);
        dto.setCodeClient(client.getCodeClient());
        dto.setVille(client.getVille());
        dto.setPays(client.getPays());
        
        return dto;
    }

    public Client toEntity(ClientDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Client client = new Client();
        client.setId(dto.getId());
        client.setNom(dto.getNom());
        client.setEmail(dto.getEmail());
        client.setTelephone(dto.getTelephone());
        client.setAdresse(dto.getAdresse());
        if (dto.getType() != null) {
            client.setType(Client.TypeClient.valueOf(dto.getType()));
        }
        client.setCodeClient(dto.getCodeClient());
        client.setVille(dto.getVille());
        client.setPays(dto.getPays());
        
        return client;
    }
}

