package com.backend.gesy.client;

import com.backend.gesy.client.dto.ClientDTO;
import com.backend.gesy.client.dto.ClientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientServiceImpl implements ClientService {
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Override
    public List<ClientDTO> findAll() {
        return clientRepository.findAll().stream()
            .map(clientMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<ClientDTO> findClientsWithAtLeastOneAchat() {
        return clientRepository.findClientsWithAtLeastOneAchat().stream()
            .map(clientMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<ClientDTO> findById(Long id) {
        return clientRepository.findById(id)
            .map(clientMapper::toDTO);
    }

    @Override
    public Optional<ClientDTO> findByEmail(String email) {
        return clientRepository.findByEmail(email)
            .map(clientMapper::toDTO);
    }

    @Override
    public Optional<ClientDTO> findByCodeClient(String codeClient) {
        return clientRepository.findByCodeClient(codeClient)
            .map(clientMapper::toDTO);
    }

    @Override
    public ClientDTO save(ClientDTO clientDTO) {
        Client client = clientMapper.toEntity(clientDTO);
        
        // Générer un code client unique si non fourni
        if (client.getCodeClient() == null || client.getCodeClient().trim().isEmpty()) {
            String codeClient = generateUniqueCodeClient();
            client.setCodeClient(codeClient);
        } else {
            // Vérifier l'unicité du code fourni
            if (clientRepository.findByCodeClient(client.getCodeClient()).isPresent()) {
                throw new RuntimeException("Le code client " + client.getCodeClient() + " existe déjà");
            }
        }
        
        Client savedClient = clientRepository.save(client);
        return clientMapper.toDTO(savedClient);
    }
    
    /**
     * Génère un code client unique au format CLT-XXX
     */
    private String generateUniqueCodeClient() {
        String prefix = "CLT-";
        int nextNumber = 1;
        
        // Trouver le dernier numéro
        List<Client> clients = clientRepository.findAll();
        for (Client client : clients) {
            if (client.getCodeClient() != null && client.getCodeClient().startsWith(prefix)) {
                try {
                    String numberPart = client.getCodeClient().substring(prefix.length());
                    int num = Integer.parseInt(numberPart);
                    if (num >= nextNumber) {
                        nextNumber = num + 1;
                    }
                } catch (NumberFormatException e) {
                    // Ignorer les codes mal formatés
                }
            }
        }
        
        String codeClient = prefix + String.format("%03d", nextNumber);
        
        // Vérifier l'unicité
        while (clientRepository.findByCodeClient(codeClient).isPresent()) {
            nextNumber++;
            codeClient = prefix + String.format("%03d", nextNumber);
        }
        
        return codeClient;
    }

    @Override
    public ClientDTO update(Long id, ClientDTO clientDTO) {
        Client existingClient = clientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + id));
        Client client = clientMapper.toEntity(clientDTO);
        client.setId(existingClient.getId());
        
        // Si le code client change, vérifier l'unicité
        if (client.getCodeClient() != null && !client.getCodeClient().equals(existingClient.getCodeClient())) {
            if (clientRepository.findByCodeClient(client.getCodeClient()).isPresent()) {
                throw new RuntimeException("Le code client " + client.getCodeClient() + " existe déjà");
            }
        } else {
            // Conserver le code existant si non modifié
            client.setCodeClient(existingClient.getCodeClient());
        }
        
        Client updatedClient = clientRepository.save(client);
        return clientMapper.toDTO(updatedClient);
    }

    @Override
    public void deleteById(Long id) {
        clientRepository.deleteById(id);
    }
}

