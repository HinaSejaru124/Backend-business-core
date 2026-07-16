package com.pharmacore.pharmaciebackend.dashboard;

import com.pharmacore.pharmaciebackend.medicament.MedicamentRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final MedicamentRepository medicamentRepository;

    public DashboardController(MedicamentRepository medicamentRepository) {
        this.medicamentRepository = medicamentRepository;
    }

    @GetMapping
    public DashboardResponse tableau() {
        long total = medicamentRepository.count();
        long alertes = medicamentRepository.trouverSousSeuilAlerte().size();
        return new DashboardResponse(total, alertes, BigDecimal.ZERO, 0);
    }
}
