import type { Role } from "./types";

/**
 * Table d'accès par rôle — chacun ne voit (et n'atteint) que son espace. Vérifiée côté client pour la
 * navigation/redirection ; les actions sensibles sont de toute façon re-vérifiées côté serveur
 * ({@code PharmacoreSession.exigerRole}, cf. backend) — ceci n'est qu'un confort de navigation, pas la
 * sécurité réelle.
 */
const ROUTES: { segment: string; roles: Role[] }[] = [
  { segment: "/admin", roles: ["TITULAIRE"] },
  { segment: "/medicaments", roles: ["TITULAIRE"] },
  { segment: "/fournisseurs", roles: ["TITULAIRE"] },
  { segment: "/commandes", roles: ["TITULAIRE"] },
  { segment: "/alertes", roles: ["TITULAIRE"] },
  { segment: "/ventes", roles: ["TITULAIRE"] },
  { segment: "/ordonnances", roles: ["TITULAIRE", "PHARMACIEN_RESPONSABLE"] },
  { segment: "/clients", roles: ["TITULAIRE", "PHARMACIEN_RESPONSABLE", "CAISSIER"] },
  { segment: "/vente", roles: ["TITULAIRE", "PHARMACIEN_RESPONSABLE", "CAISSIER"] },
];

/** Page d'accueil naturelle d'un rôle, utilisée après connexion et pour rediriger hors d'un espace interdit. */
export function homePour(role: Role | null): string {
  if (role === "TITULAIRE") return "/";
  if (role === "PHARMACIEN_RESPONSABLE" || role === "CAISSIER") return "/vente";
  return "/connexion";
}

/** {@code /vente} et {@code /ventes} sont des routes distinctes : on compare des segments, pas des préfixes bruts. */
export function estAutorise(pathname: string, role: Role | null): boolean {
  if (pathname === "/") return role === "TITULAIRE";
  const regle = ROUTES.find((r) => pathname === r.segment || pathname.startsWith(r.segment + "/"));
  if (!regle) return true; // route sans règle déclarée : pas de restriction connue
  return role !== null && regle.roles.includes(role);
}

export const LIBELLE_ROLE: Record<Role, string> = {
  TITULAIRE: "Pharmacien titulaire",
  PHARMACIEN_RESPONSABLE: "Pharmacien",
  CAISSIER: "Caissier",
};
