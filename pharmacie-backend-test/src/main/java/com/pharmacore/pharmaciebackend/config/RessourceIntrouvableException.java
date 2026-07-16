package com.pharmacore.pharmaciebackend.config;

import java.util.UUID;

public class RessourceIntrouvableException extends RuntimeException {
    public RessourceIntrouvableException(String type, UUID id) {
        super(type + " introuvable : " + id);
    }
}
