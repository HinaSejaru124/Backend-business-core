package com.yowyob.businesscore.adapter.out.kernel.actor;

/**
 * Découpe un identifiant/nom unique du domaine en {@code firstName}/{@code lastName}, tous deux exigés
 * par le kernel ({@code CreateActorRequest}). Le domaine ne porte qu'un libellé : on prend le premier
 * mot comme prénom et le reste comme nom (à défaut, le libellé entier pour les deux).
 */
public record NomPersonne(String prenom, String nomFamille) {

    public static NomPersonne de(String libelle) {
        String valeur = libelle == null ? "" : libelle.trim();
        if (valeur.isBlank()) {
            return new NomPersonne("N/A", "N/A");
        }
        String[] parties = valeur.split("\\s+", 2);
        return parties.length > 1
                ? new NomPersonne(parties[0], parties[1])
                : new NomPersonne(valeur, valeur);
    }
}
