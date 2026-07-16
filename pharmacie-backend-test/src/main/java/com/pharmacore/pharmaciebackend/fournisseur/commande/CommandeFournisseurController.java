package com.pharmacore.pharmaciebackend.fournisseur.commande;

import com.pharmacore.pharmaciebackend.fournisseur.commande.CommandeFournisseurDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/commandes-fournisseurs")
public class CommandeFournisseurController {

    private final CommandeFournisseurService service;

    public CommandeFournisseurController(CommandeFournisseurService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommandeResponse creer(@Valid @RequestBody CreerCommandeRequest req) {
        return service.creer(req);
    }

    @GetMapping
    public List<CommandeResponse> lister() {
        return service.lister();
    }

    @GetMapping("/{id}")
    public CommandeResponse trouver(@PathVariable UUID id) {
        return service.trouver(id);
    }

    @PostMapping("/{id}/reception")
    public CommandeResponse receptionner(@PathVariable UUID id,
                                         @RequestBody(required = false) ReceptionRequest req) {
        return service.receptionner(id, req);
    }
}
