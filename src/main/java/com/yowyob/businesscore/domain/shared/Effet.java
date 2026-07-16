package com.yowyob.businesscore.domain.shared;

/**
 * Les six effets de règle, regroupés en trois familles.
 * <ul>
 *   <li>Bloquants : {@link #BLOQUER}, {@link #EXIGER}, {@link #VALIDER}</li>
 *   <li>Mutateur : {@link #AJUSTER} (jamais silencieux, toujours tracé)</li>
 *   <li>Traçants : {@link #ALERTER}, {@link #DEROGER} (limité aux rôles autorisés)</li>
 * </ul>
 */
public enum Effet {
    BLOQUER(Famille.BLOQUANT),
    EXIGER(Famille.BLOQUANT),
    VALIDER(Famille.BLOQUANT),
    AJUSTER(Famille.MUTATEUR),
    ALERTER(Famille.TRACANT),
    DEROGER(Famille.TRACANT);

    public enum Famille {
        BLOQUANT,
        MUTATEUR,
        TRACANT
    }

    private final Famille famille;

    Effet(Famille famille) {
        this.famille = famille;
    }

    public Famille famille() {
        return famille;
    }

    public boolean estBloquant() {
        return famille == Famille.BLOQUANT;
    }
}
