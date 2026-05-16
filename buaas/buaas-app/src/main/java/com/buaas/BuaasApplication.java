package com.buaas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BuaaS — Business Assistance as a Service
 *
 * Premier consommateur du Business Core générique (bcaas-core).
 * Cette application implémente la couche 5 (Business Capabilities)
 * de la pile protocolaire BCaaS pour le domaine de l'orientation
 * professionnelle.
 *
 * @see <a href="docs/adr/ADR-003-pile-protocolaire-metier.md">ADR-003</a>
 */
@SpringBootApplication
public class BuaasApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuaasApplication.class, args);
    }
}
