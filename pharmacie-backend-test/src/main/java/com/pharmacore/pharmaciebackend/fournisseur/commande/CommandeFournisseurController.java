package com.pharmacore.pharmaciebackend.fournisseur.commande;

import com.pharmacore.pharmaciebackend.auth.PharmacoreSession;
import com.pharmacore.pharmaciebackend.fournisseur.commande.CommandeFournisseurDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Commandes fournisseurs — back-office (réapprovisionnement), réservé au titulaire. */
@RestController
@RequestMapping("/api/commandes-fournisseurs")
public class CommandeFournisseurController {

    private final CommandeFournisseurService service;
    private final PharmacoreSession session;

    public CommandeFournisseurController(CommandeFournisseurService service, PharmacoreSession session) {
        this.service = service;
        this.session = session;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommandeResponse creer(@Valid @RequestBody CreerCommandeRequest req) {
        session.exigerRole(PharmacoreSession.Role.TITULAIRE);
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
        session.exigerRole(PharmacoreSession.Role.TITULAIRE);
        return service.receptionner(id, req);
    }
}
