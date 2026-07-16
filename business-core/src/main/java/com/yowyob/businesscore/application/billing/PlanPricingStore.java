package com.yowyob.businesscore.application.billing;

import com.yowyob.businesscore.adapter.out.persistence.billing.PlanPricingEntity;
import com.yowyob.businesscore.adapter.out.persistence.billing.PlanPricingRepository;
import com.yowyob.businesscore.application.billing.BillingProperties.PlanDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Source de vérité <b>vivante</b> de la tarification des plans, éditable par l'administrateur.
 *
 * <p>Amorcée depuis {@link BillingProperties} (valeurs de {@code application.yml}) au démarrage, puis
 * <b>recouverte</b> par les lignes persistées en base ({@code plan_pricing}). Toute modification admin
 * met à jour la carte en mémoire ET la base — c'est réel et durable au redémarrage.
 *
 * <p>Lecture <b>synchrone</b> ({@link #snapshot()}) : {@link PlanCatalogue} et toute la logique de quota
 * l'utilisent sans devenir réactives. Le chargement initial depuis la base est fait une fois au
 * démarrage (thread principal, {@link ApplicationRunner}) — jamais sur un thread d'event-loop.
 */
@Component
public class PlanPricingStore implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlanPricingStore.class);

    private final PlanPricingRepository repository;
    private final Map<String, PlanDef> cache = new ConcurrentHashMap<>();

    public PlanPricingStore(PlanPricingRepository repository, BillingProperties properties) {
        this.repository = repository;
        // Amorçage depuis la config (valeurs par défaut).
        properties.plans().forEach((code, def) -> cache.put(normaliser(code), def));
    }

    /** Chargement initial depuis la base (recouvre les valeurs de config). Bloquant au démarrage — OK. */
    @Override
    public void run(ApplicationArguments args) {
        try {
            repository.findAll()
                    .doOnNext(e -> cache.put(normaliser(e.getCode()),
                            new PlanDef(e.getQuotaMensuel(), e.getPrixMensuel(), e.getDevise())))
                    .then()
                    .block();
        } catch (RuntimeException ex) {
            log.warn("Chargement de la tarification depuis la base impossible (valeurs de config conservées) : {}",
                    ex.toString());
        }
    }

    /** Copie immuable des plans courants (lecture synchrone). */
    public Map<String, PlanDef> snapshot() {
        return new LinkedHashMap<>(cache);
    }

    /** Définit/écrase la tarification d'un plan : met à jour la mémoire ET persiste en base. */
    public Mono<Void> definir(String code, long quotaMensuel, long prixMensuel, String devise) {
        String norm = normaliser(code);
        String dev = (devise == null || devise.isBlank()) ? "XAF" : devise.trim().toUpperCase(Locale.ROOT);
        cache.put(norm, new PlanDef(quotaMensuel, prixMensuel, dev));
        return repository.existsById(norm)
                .flatMap(existe -> repository.save(
                        PlanPricingEntity.de(norm, quotaMensuel, prixMensuel, dev, !existe)))
                .then();
    }

    private static String normaliser(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }
}
