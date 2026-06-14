package com.yowyob.businesscore.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Chiffrement au repos des secrets kernel (S.4) en AES-256-GCM.
 *
 * <p>La clé maîtresse provient d'une variable d'environnement ({@code businesscore.security.encryption-key}),
 * normalisée en 32 octets via SHA-256 (accepte n'importe quelle longueur d'entrée). Un IV aléatoire de
 * 12 octets est généré par message et préfixé au texte chiffré ; la sortie est encodée en Base64.
 * Cible prod : clé fournie par un coffre (Spring Cloud Vault), sans changer ce code.
 */
@Component
public class SecretCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    public SecretCipher(@Value("${businesscore.security.encryption-key:}") String masterKey) {
        if (masterKey == null || masterKey.isBlank()) {
            throw new IllegalStateException(
                    "businesscore.security.encryption-key est obligatoire (variable d'environnement / vault)");
        }
        this.key = sha256(masterKey);
    }

    public String chiffrer(String clair) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] chiffre = cipher.doFinal(clair.getBytes(StandardCharsets.UTF_8));

            byte[] sortie = new byte[iv.length + chiffre.length];
            System.arraycopy(iv, 0, sortie, 0, iv.length);
            System.arraycopy(chiffre, 0, sortie, iv.length, chiffre.length);
            return Base64.getEncoder().encodeToString(sortie);
        } catch (Exception e) {
            throw new IllegalStateException("Échec du chiffrement du secret", e);
        }
    }

    public String dechiffrer(String base64) {
        try {
            byte[] entree = Base64.getDecoder().decode(base64);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(entree, 0, iv, 0, IV_LENGTH);
            byte[] chiffre = new byte[entree.length - IV_LENGTH];
            System.arraycopy(entree, IV_LENGTH, chiffre, 0, chiffre.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(chiffre), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Échec du déchiffrement du secret", e);
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
