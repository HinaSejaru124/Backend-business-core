package com.pharmacore.pharmaciebackend.medicament;

import com.pharmacore.pharmaciebackend.bcaas.BcaasClient;
import com.pharmacore.pharmaciebackend.config.RessourceIntrouvableException;
import com.pharmacore.pharmaciebackend.medicament.MedicamentDtos.CreerMedicamentRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Créer un médicament = créer l'Offre correspondante côté Business Core PUIS la fiche locale,
 * dans cet ordre (cf. backend-test.md §1.3) : l'offreId réel de BCaaS est requis pour la fiche
 * locale, jamais l'inverse. En cas d'échec de la persistance locale après un appel BCaaS réussi,
 * l'Offre reste orpheline côté BCaaS (limitation connue en v1, pas de compensation cross-système —
 * acceptable pour une application de test).
 */
@Service
public class MedicamentService {

    private final MedicamentRepository repository;
    private final BcaasClient bcaas;

    public MedicamentService(MedicamentRepository repository, BcaasClient bcaas) {
        this.repository = repository;
        this.bcaas = bcaas;
    }

    public MedicamentDtos.MedicamentResponse creer(CreerMedicamentRequest req) {
        var offre = bcaas.declarerOffre(req.nom(), req.prixUnitaire(), true);

        Medicament medicament = new Medicament(
                offre.id(), req.nom(), req.dci(), req.formeGalenique(), req.codeCip(),
                req.categorie(), req.ordonnanceRequise(), req.prixUnitaire(),
                req.stockInitial(), req.seuilAlerte(), req.fournisseurId());

        return MedicamentDtos.MedicamentResponse.depuis(repository.save(medicament));
    }

    public List<MedicamentDtos.MedicamentResponse> lister() {
        return repository.findAll().stream().map(MedicamentDtos.MedicamentResponse::depuis).toList();
    }

    /** Médicaments dont le stock local est au ou sous le seuil d'alerte — calculé à la volée. */
    public List<MedicamentDtos.MedicamentResponse> alertesStock() {
        return repository.trouverSousSeuilAlerte().stream()
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
}
