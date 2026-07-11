package com.pharmacore.pharmaciebackend.client;

import com.pharmacore.pharmaciebackend.client.ClientDtos.ClientResponse;
import com.pharmacore.pharmaciebackend.client.ClientDtos.CreerClientRequest;
import com.pharmacore.pharmaciebackend.config.RessourceIntrouvableException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientRepository repository;

    public ClientController(ClientRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse creer(@Valid @RequestBody CreerClientRequest req) {
        Client c = new Client(req.nom(), req.prenom(), req.telephone(), req.email(), req.adresse());
        return ClientResponse.depuis(repository.save(c));
    }

    @GetMapping
    public List<ClientResponse> lister() {
        return repository.findAll().stream().map(ClientResponse::depuis).toList();
    }

    @GetMapping("/{id}")
    public ClientResponse trouver(@PathVariable UUID id) {
        return repository.findById(id).map(ClientResponse::depuis)
                .orElseThrow(() -> new RessourceIntrouvableException("Client", id));
    }
}
