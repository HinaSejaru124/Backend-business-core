package com.pharmacore.pharmaciebackend.medicament;

import com.pharmacore.pharmaciebackend.medicament.MedicamentDtos.MedicamentResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Catalogue en <b>lecture</b> (runtime, accessible clé API — la caisse a besoin de voir les
 * médicaments pour vendre). La création est design-time (JWT) : voir {@code AdminMedicamentController}.
 */
@RestController
@RequestMapping("/api/medicaments")
public class MedicamentController {

    private final MedicamentService service;

    public MedicamentController(MedicamentService service) {
        this.service = service;
    }

    @GetMapping
    public List<MedicamentResponse> lister() {
        return service.lister();
    }

    @GetMapping("/{id}")
    public MedicamentResponse trouver(@PathVariable UUID id) {
        return service.trouver(id);
    }
}
