package com.pharmacore.pharmaciebackend.medicament;

import com.pharmacore.pharmaciebackend.medicament.MedicamentDtos.CreerMedicamentRequest;
import com.pharmacore.pharmaciebackend.medicament.MedicamentDtos.MedicamentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medicaments")
public class MedicamentController {

    private final MedicamentService service;

    public MedicamentController(MedicamentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicamentResponse creer(@Valid @RequestBody CreerMedicamentRequest req) {
        return service.creer(req);
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
