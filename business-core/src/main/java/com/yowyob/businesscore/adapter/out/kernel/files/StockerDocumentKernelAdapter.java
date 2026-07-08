package com.yowyob.businesscore.adapter.out.kernel.files;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.StockerDocument;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.UUID;

/**
 * Adapter kernel — implémente {@link StockerDocument}. Dépose une pièce (ordonnance, licence...) sur
 * le core files du kernel ({@code POST /api/files}) via {@link KernelClient}. Le contenu binaire est
 * transmis encodé en Base64.
 */
@Component
public class StockerDocumentKernelAdapter implements StockerDocument {

    private final KernelClient kernel;

    public StockerDocumentKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    @Override
    public Mono<UUID> stocker(String nom, String contentType, byte[] contenu) {
        String base64 = Base64.getEncoder().encodeToString(contenu == null ? new byte[0] : contenu);
        DeposerFichierRequest requete = new DeposerFichierRequest(nom, contentType, base64);
        return kernel.post("/api/files", requete, FichierResponse.class)
                .map(fichier -> fichier.id());
    }

    @Override
    public Mono<byte[]> lireContenu(UUID fichierId) {
        return kernel.getBytes("/api/files/" + fichierId + "/content");
    }
}

record DeposerFichierRequest(String name, String contentType, String content) {
}

record FichierResponse(UUID id) {
}
