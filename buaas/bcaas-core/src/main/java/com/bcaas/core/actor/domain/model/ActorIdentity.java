package com.bcaas.core.actor.domain.model;

/**
 * Value Object encapsulant l'identité d'un acteur.
 * Séparé de l'entité Actor pour respecter SRP.
 */
public record ActorIdentity(
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String locale
) {
    public ActorIdentity {
        if (email == null || !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Email invalide : " + email);
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("Le prénom est obligatoire");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Le nom est obligatoire");
        }
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public ActorIdentity withLocale(String newLocale) {
        return new ActorIdentity(email, firstName, lastName, phoneNumber, newLocale);
    }
}
