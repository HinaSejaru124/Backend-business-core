package com.pharmacore.pharmaciebackend.medicament;

import com.pharmacore.pharmaciebackend.config.RessourceIntrouvableException;
import com.pharmacore.pharmaciebackend.medicament.MedicamentDtos.CreerMedicamentRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Lecture et gestion locale du catalogue (accessible en runtime, clé API). La <b>création</b> d'un
 * médicament déclare une Offre Business Core, ce qui est design-time (JWT) depuis la séparation
 * design-time/runtime — voir {@code AdminMedicamentController} / {@code BcaasAdminClient.declarerOffre}.
 * Ce service ne sait donc plus créer : seulement lire, alerter, réapprovisionner et enregistrer une
 * fiche locale une fois l'offre déjà déclarée côté design-time ({@link #sauvegarderDepuisOffre}).
 */
@Service
public class MedicamentService {

    private final MedicamentRepository repository;

    public MedicamentService(MedicamentRepository repository) {
        this.repository = repository;
    }

    /**
     * Enregistre la fiche locale d'un médicament dont l'Offre a déjà été déclarée côté Business Core
     * (design-time). L'{@code offreId} réel est requis en entrée, jamais deviné ni généré ici.
     */
    public MedicamentDtos.MedicamentResponse sauvegarderDepuisOffre(CreerMedicamentRequest req, UUID offreId) {
        Medicament medicament = new Medicament(
                offreId, req.nom(), req.dci(), req.formeGalenique(), req.codeCip(),
                req.categorie(), req.ordonnanceRequise(), req.prixUnitaire(),
                req.stockInitial(), req.seuilAlerte(), req.fournisseurId());

        return MedicamentDtos.MedicamentResponse.depuis(repository.save(medicament));
    }

    /** Catalogue actif — un médicament {@code RETIRE} (cf. {@link #retirer}) reste en base mais n'est
     * plus proposé à la vente ni au catalogue admin (ordonnances existantes non affectées). */
    public List<MedicamentDtos.MedicamentResponse> lister() {
        return repository.findAll().stream()
                .filter(m -> !"RETIRE".equals(m.getStatut()))
                .map(MedicamentDtos.MedicamentResponse::depuis).toList();
    }

    /** Médicaments actifs dont le stock local est au ou sous le seuil d'alerte — calculé à la volée. */
    public List<MedicamentDtos.MedicamentResponse> alertesStock() {
        return repository.trouverSousSeuilAlerte().stream()
                .filter(m -> !"RETIRE".equals(m.getStatut()))
                .map(MedicamentDtos.MedicamentResponse::depuis).toList();
    }

    public MedicamentDtos.MedicamentResponse trouver(UUID id) {
        return repository.findById(id)
                .map(MedicamentDtos.MedicamentResponse::depuis)
                .orElseThrow(() -> new RessourceIntrouvableException("Médicament", id));
    }

    /** Réception fournisseur : augmente le stock local. Aucun équivalent BCaaS (cf. backend-test.md §5). */
    public MedicamentDtos.MedicamentResponse reapprovisionner(UUID id, int quantite) {
        Medicament medicament = repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Médicament", id));
        medicament.reapprovisionner(quantite);
        return MedicamentDtos.MedicamentResponse.depuis(repository.save(medicament));
    }

    /** {@code offreId} de la fiche locale — nécessaire pour supprimer l'Offre côté Business Core avant. */
    public UUID offreIdDe(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Médicament", id))
                .getOffreId();
    }

    /** Supprime uniquement la fiche locale — appeler après suppression réussie de l'Offre côté BCaaS. */
    public void supprimer(UUID id) {
        if (!repository.existsById(id)) {
            throw new RessourceIntrouvableException("Médicament", id);
        }
        repository.deleteById(id);
    }

    /** Retire la fiche du catalogue actif sans la supprimer (référencée par une ordonnance réelle). */
    public void retirer(UUID id) {
        Medicament medicament = repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Médicament", id));
        medicament.retirer();
        repository.save(medicament);
    }
}
