package com.yowyob.businesscore.adapter.out.kernel.actor;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.ResoudrePersonne;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Opérateur (interne) → actor-core kernel : {@code POST /api/actors}. Le kernel exige
 * {@code firstName}/{@code lastName} ; on dérive du libellé via {@link NomPersonne}.
 */
@Component
public class ResoudrePersonneKernelAdapter implements ResoudrePersonne {

    private final KernelClient kernel;

    public ResoudrePersonneKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    record KernelId(UUID id) {}

    record CreerActeurRequest(String firstName, String lastName, String name) {}

    @Override
    public Mono<UUID> resoudreOperateur(String identifiant, String nom) {
        NomPersonne nomPersonne = NomPersonne.de(nom);
        CreerActeurRequest requete = new CreerActeurRequest(
                nomPersonne.prenom(), nomPersonne.nomFamille(), nom);
        return kernel.post("/api/actors", requete, KernelId.class)
                .map(kernelId -> kernelId.id());
    }
}
