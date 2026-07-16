package com.pharmacore.pharmaciebackend.ordonnance;

import com.pharmacore.pharmaciebackend.config.RessourceIntrouvableException;
import com.pharmacore.pharmaciebackend.ordonnance.OrdonnanceDtos.CreerOrdonnanceRequest;
import com.pharmacore.pharmaciebackend.ordonnance.OrdonnanceDtos.OrdonnanceResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class OrdonnanceService {

    private final OrdonnanceRepository repository;
    private final OrdonnanceLigneRepository ligneRepository;

    public OrdonnanceService(OrdonnanceRepository repository, OrdonnanceLigneRepository ligneRepository) {
        this.repository = repository;
        this.ligneRepository = ligneRepository;
    }

    @Transactional
    public OrdonnanceResponse creer(CreerOrdonnanceRequest req) {
        byte[] contenu = req.documentContenuBase64() != null && !req.documentContenuBase64().isBlank()
                ? Base64.getDecoder().decode(req.documentContenuBase64())
                : null;
        Ordonnance ordonnance = repository.save(new Ordonnance(
                req.clientId(), req.medecinNom(), req.medecinNumeroOrdre(), req.dateEmission(),
                req.documentNom(), req.documentContentType(), contenu));

        List<OrdonnanceLigne> lignes = req.lignes().stream()
                .map(l -> ligneRepository.save(new OrdonnanceLigne(
                        ordonnance.getId(), l.medicamentId(), l.quantitePrescrite(), l.posologie())))
                .toList();

        return OrdonnanceResponse.depuis(ordonnance, lignes);
    }

    public List<OrdonnanceResponse> lister() {
        return repository.findAll().stream()
                .map(o -> OrdonnanceResponse.depuis(o, ligneRepository.findByOrdonnanceId(o.getId())))
                .toList();
    }

    public OrdonnanceResponse trouver(UUID id) {
        Ordonnance ordonnance = repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Ordonnance", id));
        return OrdonnanceResponse.depuis(ordonnance, ligneRepository.findByOrdonnanceId(id));
    }

    public Ordonnance trouverEntite(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Ordonnance", id));
    }
}
