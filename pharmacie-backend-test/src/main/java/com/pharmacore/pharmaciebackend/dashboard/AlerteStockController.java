package com.pharmacore.pharmaciebackend.dashboard;

import com.pharmacore.pharmaciebackend.medicament.MedicamentDtos.MedicamentResponse;
import com.pharmacore.pharmaciebackend.medicament.MedicamentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alertes-stock")
public class AlerteStockController {

    private final MedicamentService medicamentService;

    public AlerteStockController(MedicamentService medicamentService) {
        this.medicamentService = medicamentService;
    }

    @GetMapping
    public List<MedicamentResponse> lister() {
        return medicamentService.alertesStock();
    }
}
