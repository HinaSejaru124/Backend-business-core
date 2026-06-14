package com.yowyob.businesscore.infrastructure.time;

import com.yowyob.businesscore.domain.port.internal.HorlogeSysteme;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Implémentation socle de l'horloge : temps système réel. */
@Component
public class SystemeHorloge implements HorlogeSysteme {

    @Override
    public Instant maintenant() {
        return Instant.now();
    }
}
