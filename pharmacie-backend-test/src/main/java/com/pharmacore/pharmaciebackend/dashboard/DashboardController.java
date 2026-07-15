package com.pharmacore.pharmaciebackend.dashboard;

import com.pharmacore.pharmaciebackend.medicament.MedicamentService;
import com.pharmacore.pharmaciebackend.vente.VenteRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final MedicamentService medicamentService;
    private final VenteRepository venteRepository;

    public DashboardController(MedicamentService medicamentService, VenteRepository venteRepository) {
        this.medicamentService = medicamentService;
        this.venteRepository = venteRepository;
    }

    @GetMapping
    public DashboardResponse tableau() {
        long total = medicamentService.lister().size();
        long alertes = medicamentService.alertesStock().size();
        Instant debutJour = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant();
        return new DashboardResponse(total, alertes, venteRepository.sommeMontantDepuis(debutJour),
                venteRepository.countByCreeLeAfter(debutJour));
    }
}
