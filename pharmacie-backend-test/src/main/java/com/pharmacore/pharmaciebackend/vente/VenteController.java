package com.pharmacore.pharmaciebackend.vente;

import com.pharmacore.pharmaciebackend.vente.VenteDtos.CreerVenteRequest;
import com.pharmacore.pharmaciebackend.vente.VenteDtos.VenteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Encaissement — orchestre réellement {@code Vendre:execute} (runtime, clé API + acteur connecté).
 * Voir {@link VenteService} pour la logique d'exécution ligne par ligne.
 */
@RestController
@RequestMapping("/api/ventes")
public class VenteController {

    private final VenteService service;

    public VenteController(VenteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VenteResponse creer(@Valid @RequestBody CreerVenteRequest req) {
        return service.creer(req);
    }

    @GetMapping
    public List<VenteResponse> lister() {
        return service.lister();
    }

    @GetMapping("/{id}")
    public VenteResponse trouver(@PathVariable UUID id) {
        return service.trouver(id);
    }
}
