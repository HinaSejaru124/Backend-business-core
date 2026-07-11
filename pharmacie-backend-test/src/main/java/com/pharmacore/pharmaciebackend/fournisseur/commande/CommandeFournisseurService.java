package com.pharmacore.pharmaciebackend.fournisseur.commande;

import com.pharmacore.pharmaciebackend.config.RessourceIntrouvableException;
import com.pharmacore.pharmaciebackend.fournisseur.commande.CommandeFournisseurDtos.*;
import com.pharmacore.pharmaciebackend.medicament.MedicamentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Réception d'une commande fournisseur = seul moyen d'augmenter {@code medicament.stock_actuel}.
 * Aucun équivalent Business Core : le stock reste une donnée locale à PharmaCore
 * (cf. backend-test.md §5 et GUIDE-PROJET-PHARMACORE.md §6).
 */
@Service
public class CommandeFournisseurService {

    private final CommandeFournisseurRepository repository;
    private final CommandeFournisseurLigneRepository ligneRepository;
    private final MedicamentService medicamentService;

    public CommandeFournisseurService(CommandeFournisseurRepository repository,
                                      CommandeFournisseurLigneRepository ligneRepository,
                                      MedicamentService medicamentService) {
        this.repository = repository;
        this.ligneRepository = ligneRepository;
        this.medicamentService = medicamentService;
    }

    @Transactional
    public CommandeResponse creer(CreerCommandeRequest req) {
        CommandeFournisseur commande = repository.save(new CommandeFournisseur(
                req.fournisseurId(), req.dateCommande(), req.dateReceptionPrevue()));

        List<CommandeFournisseurLigne> lignes = req.lignes().stream()
                .map(l -> ligneRepository.save(new CommandeFournisseurLigne(
                        commande.getId(), l.medicamentId(), l.quantiteCommandee(), l.prixUnitaireAchat())))
                .toList();

        return CommandeResponse.depuis(commande, lignes);
    }

    public List<CommandeResponse> lister() {
        return repository.findAll().stream()
                .map(c -> CommandeResponse.depuis(c, ligneRepository.findByCommandeFournisseurId(c.getId())))
                .toList();
    }

    public CommandeResponse trouver(UUID id) {
        CommandeFournisseur commande = charger(id);
        return CommandeResponse.depuis(commande, ligneRepository.findByCommandeFournisseurId(id));
    }

    /**
     * Réceptionne la commande : incrémente {@code medicament.stock_actuel} pour chaque ligne
     * (quantité précisée dans la requête, ou quantité commandée par défaut), marque la commande RECUE.
     */
    @Transactional
    public CommandeResponse receptionner(UUID id, ReceptionRequest req) {
        CommandeFournisseur commande = charger(id);
        List<CommandeFournisseurLigne> lignes = ligneRepository.findByCommandeFournisseurId(id);

        Map<UUID, Integer> quantitesPrecisees = req == null || req.lignes() == null
                ? Map.of()
                : req.lignes().stream().collect(Collectors.toMap(
                        ReceptionLigne::ligneId, ReceptionLigne::quantiteRecue));

        for (CommandeFournisseurLigne ligne : lignes) {
            int quantiteRecue = quantitesPrecisees.getOrDefault(ligne.getId(), ligne.getQuantiteCommandee());
            ligne.marquerRecue(quantiteRecue);
            ligneRepository.save(ligne);
            medicamentService.reapprovisionner(ligne.getMedicamentId(), quantiteRecue);
        }

        commande.marquerRecue(LocalDate.now());
        repository.save(commande);

        return CommandeResponse.depuis(commande, ligneRepository.findByCommandeFournisseurId(id));
    }

    private CommandeFournisseur charger(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Commande fournisseur", id));
    }
}
