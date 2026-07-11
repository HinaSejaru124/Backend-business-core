package com.pharmacore.pharmaciebackend.fournisseur;

import com.pharmacore.pharmaciebackend.config.RessourceIntrouvableException;
import com.pharmacore.pharmaciebackend.fournisseur.FournisseurDtos.CreerFournisseurRequest;
import com.pharmacore.pharmaciebackend.fournisseur.FournisseurDtos.FournisseurResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fournisseurs")
public class FournisseurController {

    private final FournisseurRepository repository;

    public FournisseurController(FournisseurRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FournisseurResponse creer(@Valid @RequestBody CreerFournisseurRequest req) {
        Fournisseur f = new Fournisseur(req.nom(), req.contactNom(), req.contactTelephone(),
                req.email(), req.delaiLivraisonJours());
        return FournisseurResponse.depuis(repository.save(f));
    }

    @GetMapping
    public List<FournisseurResponse> lister() {
        return repository.findAll().stream().map(FournisseurResponse::depuis).toList();
    }

    @GetMapping("/{id}")
    public FournisseurResponse trouver(@PathVariable UUID id) {
        return repository.findById(id).map(FournisseurResponse::depuis)
                .orElseThrow(() -> new RessourceIntrouvableException("Fournisseur", id));
    }
}
