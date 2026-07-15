package com.pharmacore.pharmaciebackend.admin;

import com.pharmacore.pharmaciebackend.auth.PharmacoreSession;
import com.pharmacore.pharmaciebackend.config.BcaasProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vue brute du catalogue Business Core (brique 2, design-time) — distincte de {@code /api/medicaments}
 * qui ne montre que les offres pour lesquelles PharmaCore a une fiche locale. Sert au nettoyage
 * d'offres déclarées directement côté Business Core pendant les tests (diagnostics, sans passer par
 * {@code AdminMedicamentController}), donc invisibles autrement.
 */
@RestController
@RequestMapping("/api/admin/offres")
public class AdminOffreController {

    private final BcaasAdminClient adminClient;
    private final BcaasProperties properties;
    private final PharmacoreSession session;

    public AdminOffreController(BcaasAdminClient adminClient, BcaasProperties properties, PharmacoreSession session) {
        this.adminClient = adminClient;
        this.properties = properties;
        this.session = session;
    }

    @GetMapping
    public List<Map<String, Object>> lister() {
        session.exigerRole(PharmacoreSession.Role.TITULAIRE);
        return adminClient.listerOffres(properties.typeId(), properties.versionNumber());
    }

    /** Suppression directe côté Business Core — pour les offres sans fiche locale PharmaCore. */
    @DeleteMapping("/{offreId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable UUID offreId) {
        session.exigerRole(PharmacoreSession.Role.TITULAIRE);
        adminClient.supprimerOffre(properties.typeId(), properties.versionNumber(), offreId);
    }
}
