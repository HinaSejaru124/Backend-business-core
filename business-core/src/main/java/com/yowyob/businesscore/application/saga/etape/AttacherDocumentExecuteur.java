package com.yowyob.businesscore.application.saga.etape;

import com.yowyob.businesscore.application.saga.ClesContexte;
import com.yowyob.businesscore.application.saga.Valeurs;
import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.port.internal.ExecuteurDEtape;
import com.yowyob.businesscore.domain.port.out.StockerDocument;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Étape {@code ATTACHER_DOCUMENT} — dépose une pièce (ordonnance, licence...) via le port
 * {@link StockerDocument} et pose son identifiant dans le contexte. Sans document fourni, l'étape est
 * neutre (un blocage éventuel relève d'une règle EXIGER, pas de cette étape).
 */
@Component
public class AttacherDocumentExecuteur implements ExecuteurDEtape {

    private static final String CONTENT_TYPE_DEFAUT = "application/octet-stream";

    private final StockerDocument stockerDocument;

    public AttacherDocumentExecuteur(StockerDocument stockerDocument) {
        this.stockerDocument = stockerDocument;
    }

    @Override
    public TypeEtape typeSupporte() {
        return TypeEtape.ATTACHER_DOCUMENT;
    }

    @Override
    public Mono<ContexteEtape> executer(ContexteEtape contexte) {
        Object nom = contexte.get(ClesContexte.DOCUMENT_NOM);
        if (nom == null) {
            return Mono.just(contexte);
        }
        String contentType = Valeurs.versTexteOuDefaut(
                contexte.get(ClesContexte.DOCUMENT_CONTENT_TYPE), CONTENT_TYPE_DEFAUT);
        byte[] contenu = Valeurs.versOctets(contexte.get(ClesContexte.DOCUMENT_CONTENU));
        return stockerDocument.stocker(nom.toString(), contentType, contenu)
                .map(documentId -> contexte.avec(ClesContexte.DOCUMENT_ID, documentId));
    }
}
