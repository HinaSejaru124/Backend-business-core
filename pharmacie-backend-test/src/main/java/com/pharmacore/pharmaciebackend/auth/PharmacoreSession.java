package com.pharmacore.pharmaciebackend.auth;

import com.pharmacore.pharmaciebackend.bcaas.BcaasException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Identité de la personne connectée dans <b>ce navigateur</b> — remplace les anciens
 * {@code AdminSession}/{@code CaisseSession}, qui étaient des singletons partagés par tout le serveur
 * (une seule session titulaire et une seule session caisse possibles à la fois, pour tout le monde).
 * Portée {@link WebApplicationContext#SCOPE_SESSION} : Spring en crée une instance par session HTTP
 * (cookie {@code JSESSIONID}), donc un onglet Titulaire, un onglet Pharmacien et un onglet Caissier
 * peuvent être connectés en même temps sans se marcher dessus.
 *
 * <p>Deux natures d'identité, jamais mélangées :
 * <ul>
 *   <li><b>TITULAIRE</b> — JWT Business Core (design-time). Le titulaire est le compte développeur ;
 *       ce JWT est ce que Business Core exige réellement pour déclarer le catalogue/les règles, il n'y
 *       a pas de contournement possible (cf. FEUILLE-DE-ROUTE.md).</li>
 *   <li><b>PHARMACIEN_RESPONSABLE / CAISSIER</b> — compte propre à PharmaCore ({@code Personnel}), résolu
 *       une seule fois vers un {@code acteurKernelId} à la création du compte (voir
 *       {@code PersonnelAdminController}). Aucun mot de passe Kernel n'est jamais revu après cette
 *       création : la connexion quotidienne ne touche que la base locale de PharmaCore.</li>
 * </ul>
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS)
public class PharmacoreSession {

    public enum Role { TITULAIRE, PHARMACIEN_RESPONSABLE, CAISSIER }

    private volatile Role role;
    private volatile String identifiant;
    private volatile String nomAffichage;
    private volatile Instant expireLe;
    private volatile String jwt;
    private volatile String acteurKernelId;
    private volatile UUID personnelId;
    private volatile String titulaireActorId;

    /** Ouvre une session titulaire — JWT Business Core, expiration alignée sur celle du jeton kernel. */
    public synchronized void ouvrirTitulaire(String jwt, long expiresInSeconds, String principal, String actorId) {
        this.role = Role.TITULAIRE;
        this.jwt = jwt;
        this.acteurKernelId = null;
        this.personnelId = null;
        this.titulaireActorId = actorId;
        this.identifiant = principal;
        this.nomAffichage = principal;
        this.expireLe = Instant.now().plusSeconds(Math.max(0, expiresInSeconds));
    }

    /**
     * Ouvre une session personnel (Pharmacien ou Caissier) — identité 100% locale à PharmaCore. Durée
     * fixe raisonnable pour un poste de travail (8h) : pas de jeton kernel à faire expirer ici, la seule
     * limite naturelle serait celle de la session HTTP elle-même.
     */
    public synchronized void ouvrirPersonnel(Role role, UUID personnelId, String nomAffichage, String email,
                                             String acteurKernelId) {
        this.role = role;
        this.jwt = null;
        this.personnelId = personnelId;
        this.acteurKernelId = acteurKernelId;
        this.identifiant = email;
        this.nomAffichage = nomAffichage;
        this.expireLe = Instant.now().plusSeconds(8 * 3600L);
    }

    public synchronized void fermer() {
        this.role = null;
        this.jwt = null;
        this.acteurKernelId = null;
        this.personnelId = null;
        this.titulaireActorId = null;
        this.identifiant = null;
        this.nomAffichage = null;
        this.expireLe = null;
    }

    public boolean estActive() {
        return role != null && expireLe != null && Instant.now().isBefore(expireLe);
    }

    /** Rôle courant, ou {@code null} si personne n'est connecté (ou session expirée). */
    public Role role() {
        return estActive() ? role : null;
    }

    /**
     * Garde-fou générique : lève un {@link BcaasException} 401/403 si aucun des rôles autorisés n'est
     * celui de la session courante. La sécurité réelle reste côté Business Core (qui vérifie le vrai
     * rôle rattaché à l'acteur) ; ce garde-fou évite juste d'exposer les écrans/actions du mauvais rôle
     * côté PharmaCore.
     */
    public void exigerRole(Role... rolesAutorises) {
        Role actuel = role();
        if (actuel == null) {
            throw new BcaasException(401, "Non connecté", "Connectez-vous avant de continuer.", null, null, null);
        }
        boolean autorise = Arrays.stream(rolesAutorises).anyMatch(r -> r == actuel);
        if (!autorise) {
            String roles = Arrays.stream(rolesAutorises).map(Role::name).collect(Collectors.joining(", "));
            throw new BcaasException(403, "Action non autorisée",
                    "Cette action est réservée à : " + roles + ".", null, null, null);
        }
    }

    /** JWT titulaire si la session est active et de type TITULAIRE, sinon {@code null}. */
    public String jwtOuNull() {
        return estActive() && role == Role.TITULAIRE ? jwt : null;
    }

    /** Identité kernel de l'acteur (Pharmacien/Caissier) si active, sinon {@code null}. */
    public String acteurKernelIdOuNull() {
        return estActive() && role != Role.TITULAIRE ? acteurKernelId : null;
    }

    /** Identité kernel du titulaire connecté (résolue via {@code /v1/auth/me}), sinon {@code null}. */
    public String titulaireActorIdOuNull() {
        return estActive() && role == Role.TITULAIRE ? titulaireActorId : null;
    }

    public UUID personnelIdOuNull() {
        return estActive() ? personnelId : null;
    }

    public String identifiant() {
        return identifiant;
    }

    public String nomAffichage() {
        return nomAffichage;
    }

    public Instant expireLe() {
        return expireLe;
    }
}
