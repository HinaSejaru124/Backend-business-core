package com.pharmacore.pharmaciebackend.admin;

import com.pharmacore.pharmaciebackend.auth.PharmacoreSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Espace admin — modélisation du métier (design-time), réservée au titulaire. Les appels vers Business
 * Core s'appuient sur la session titulaire (JWT) via {@link BcaasAdminClient}.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminModelController {

    private final BcaasAdminClient adminClient;
    private final ModeleProvisioningService provisioning;
    private final PharmacoreSession session;

    public AdminModelController(BcaasAdminClient adminClient, ModeleProvisioningService provisioning,
                                PharmacoreSession session) {
        this.adminClient = adminClient;
        this.provisioning = provisioning;
        this.session = session;
    }

    @GetMapping("/business-types")
    public List<Map<String, Object>> listerTypesMetier() {
        session.exigerRole(PharmacoreSession.Role.TITULAIRE);
        return adminClient.listerTypesMetier();
    }

    /**
     * Déclare (idempotent) le modèle Pharmacie : rôles, règle « ordonnance requise », opération « Vendre ».
     * Le titulaire déclenche cette action une fois pour « installer » son métier sur Business Core.
     */
    @PostMapping("/modele:provisionner")
    public ModeleProvisioningService.Rapport provisionner() {
        session.exigerRole(PharmacoreSession.Role.TITULAIRE);
        return provisioning.provisionner();
    }
}
