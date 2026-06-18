package com.yowyob.businesscore.application.saga;

import com.yowyob.businesscore.domain.port.internal.ContexteEtape;

/**
 * Issue d'une exécution par le {@link MoteurOperation}. Ne propage jamais d'erreur Reactor : porte
 * explicitement le succès ou l'échec + le contexte final (qui contient le point de compensation
 * éventuel), pour que la couche application décide de la trace et du code HTTP.
 */
public record ResultatMoteur(boolean succes, ContexteEtape contexte, Throwable erreur) {

    public static ResultatMoteur succes(ContexteEtape contexte) {
        return new ResultatMoteur(true, contexte, null);
    }

    public static ResultatMoteur echec(ContexteEtape contexte, Throwable erreur) {
        return new ResultatMoteur(false, contexte, erreur);
    }
}
