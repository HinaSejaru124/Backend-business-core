package com.pharmacore.pharmaciebackend.fournisseur;

import com.pharmacore.pharmaciebackend.auth.PharmacoreSession;
import com.pharmacore.pharmaciebackend.config.RessourceIntrouvableException;
import com.pharmacore.pharmaciebackend.fournisseur.FournisseurDtos.CreerFournisseurRequest;
import com.pharmacore.pharmaciebackend.fournisseur.FournisseurDtos.FournisseurResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Fournisseurs — back-office (réapprovisionnement), réservé au titulaire. */
@RestController
@RequestMapping("/api/fournisseurs")
public class FournisseurController {

    private final FournisseurRepository repository;
    private final PharmacoreSession session;

    public FournisseurController(FournisseurRepository repository, PharmacoreSession session) {
        this.repository = repository;
        this.session = session;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FournisseurResponse creer(@Valid @RequestBody CreerFournisseurRequest req) {
        session.exigerRole(PharmacoreSession.Role.TITULAIRE);
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
