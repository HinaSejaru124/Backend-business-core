package com.yowyob.businesscore.application.saga;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Conversions tolérantes des valeurs du {@code ContexteEtape} (sac d'{@code Object}) vers des types
 * Java. Le payload JSON d'une opération étant faiblement typé, ces helpers acceptent String/Number/UUID.
 */
public final class Valeurs {

    private Valeurs() {
    }

    public static UUID versUuid(Object valeur) {
        if (valeur == null) return null;
        if (valeur instanceof UUID u) return u;
        return UUID.fromString(valeur.toString());
    }

    public static Integer versEntier(Object valeur) {
        if (valeur == null) return null;
        if (valeur instanceof Number n) return n.intValue();
        return Integer.parseInt(valeur.toString());
    }

    public static int versEntierOuDefaut(Object valeur, int defaut) {
        Integer i = versEntier(valeur);
        return i == null ? defaut : i;
    }

    public static BigDecimal versDecimal(Object valeur) {
        if (valeur == null) return null;
        if (valeur instanceof BigDecimal b) return b;
        if (valeur instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(valeur.toString());
    }

    public static String versTexteOuDefaut(Object valeur, String defaut) {
        return valeur == null ? defaut : valeur.toString();
    }

    /** Accepte un {@code byte[]} ou une chaîne (Base64, sinon texte brut UTF-8). */
    public static byte[] versOctets(Object valeur) {
        if (valeur == null) return new byte[0];
        if (valeur instanceof byte[] b) return b;
        String s = valeur.toString();
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException notBase64) {
            return s.getBytes(StandardCharsets.UTF_8);
        }
    }
}
