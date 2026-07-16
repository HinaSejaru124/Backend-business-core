package com.pharmacore.pharmaciebackend.ordonnance;

import com.pharmacore.pharmaciebackend.ordonnance.OrdonnanceDtos.CreerOrdonnanceRequest;
import com.pharmacore.pharmaciebackend.ordonnance.OrdonnanceDtos.OrdonnanceResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ordonnances")
public class OrdonnanceController {

    private final OrdonnanceService service;

    public OrdonnanceController(OrdonnanceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdonnanceResponse creer(@Valid @RequestBody CreerOrdonnanceRequest req) {
        return service.creer(req);
    }

    @GetMapping
    public List<OrdonnanceResponse> lister() {
        return service.lister();
    }

    @GetMapping("/{id}")
    public OrdonnanceResponse trouver(@PathVariable UUID id) {
        return service.trouver(id);
    }

    @GetMapping("/{id}/document")
    public ResponseEntity<byte[]> telechargerDocument(@PathVariable UUID id) {
        Ordonnance ordonnance = service.trouverEntite(id);
        byte[] contenu = ordonnance.getDocumentContenu();
        if (contenu == null || contenu.length == 0) {
            return ResponseEntity.notFound().build();
        }
        MediaType type = ordonnance.getDocumentContentType() != null
                ? MediaType.parseMediaType(ordonnance.getDocumentContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        String nomFichier = ordonnance.getDocumentNom() != null ? ordonnance.getDocumentNom() : "document";
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nomFichier + "\"")
                .body(contenu);
    }
}
