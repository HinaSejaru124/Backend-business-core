/**
 * Liste RÉELLE des endpoints exposés par le backend Business Core (source : contrôleurs REST).
 * Utilisée par la page Documentation. Ne rien inventer ici.
 */

export type HttpMethod = "GET" | "POST" | "PUT" | "DELETE" | "PATCH";

export type Endpoint = {
  method: HttpMethod;
  path: string;
  usage: string;
  auth: "public" | "bearer";
};

export type EndpointGroup = {
  group: string;
  items: Endpoint[];
};

export const ENDPOINTS: EndpointGroup[] = [
  {
    group: "Accès & Authentification",
    items: [
      {
        method: "POST",
        path: "/v1/registration",
        usage: "Créer un compte développeur → { plan, message } (pas de clé API)",
        auth: "public",
      },
      { method: "POST", path: "/v1/auth/login", usage: "Connexion → { accessToken, authorities, organisations, owner }", auth: "public" },
      {
        method: "GET",
        path: "/v1/auth/me",
        usage: "Profil courant { tenantId, actorId, permissions, owner, developerId, email, plan }",
        auth: "bearer",
      },
      { method: "GET", path: "/v1/dashboard", usage: "Tableau de bord développeur (usage, top ops, activité)", auth: "bearer" },
      { method: "GET", path: "/v1/plans", usage: "Catalogue des plans tarifaires", auth: "public" },
      { method: "POST", path: "/v1/plan/upgrade", usage: "Changer de plan → { plan, statut, urlPaiement, reference }", auth: "bearer" },
    ],
  },
  {
    group: "Types métier & Configuration",
    items: [
      { method: "GET", path: "/v1/business-types", usage: "Lister les types métier", auth: "bearer" },
      { method: "POST", path: "/v1/business-types", usage: "Créer un type métier", auth: "bearer" },
      { method: "GET", path: "/v1/business-types/{typeId}", usage: "Consulter un type", auth: "bearer" },
      { method: "POST", path: "/v1/business-types/{typeId}/publish", usage: "Publier un type", auth: "bearer" },
      { method: "POST", path: "/v1/business-types/{typeId}/archive", usage: "Archiver un type", auth: "bearer" },
      { method: "POST", path: "/v1/business-types/{typeId}/versions", usage: "Créer une version", auth: "bearer" },
      { method: "GET", path: "/v1/business-types/{typeId}/versions", usage: "Lister les versions", auth: "bearer" },
      { method: "POST", path: "/v1/business-types/{typeId}/versions/{n}/config", usage: "Définir un paramètre", auth: "bearer" },
    ],
  },
  {
    group: "Contenu de version",
    items: [
      { method: "POST", path: "/v1/business-types/{typeId}/versions/{n}/offers", usage: "Déclarer une offre", auth: "bearer" },
      { method: "POST", path: "/v1/business-types/{typeId}/versions/{n}/roles", usage: "Déclarer un rôle métier", auth: "bearer" },
      { method: "POST", path: "/v1/business-types/{typeId}/versions/{n}/rules", usage: "Déclarer une règle", auth: "bearer" },
      { method: "POST", path: "/v1/business-types/{typeId}/versions/{n}/operations", usage: "Déclarer une opération", auth: "bearer" },
    ],
  },
  {
    group: "Entreprises & Acteurs",
    items: [
      { method: "POST", path: "/v1/businesses", usage: "Créer une entreprise", auth: "bearer" },
      { method: "GET", path: "/v1/businesses", usage: "Lister les entreprises", auth: "bearer" },
      { method: "GET", path: "/v1/businesses/{businessId}", usage: "Consulter une entreprise", auth: "bearer" },
      { method: "PUT", path: "/v1/businesses/{businessId}/lifecycle", usage: "Changer le cycle de vie", auth: "bearer" },
      { method: "POST", path: "/v1/businesses/{businessId}/actors", usage: "Rattacher un acteur", auth: "bearer" },
      { method: "GET", path: "/v1/businesses/{businessId}/actors", usage: "Lister les acteurs", auth: "bearer" },
      { method: "POST", path: "/v1/businesses/{businessId}/rules", usage: "Ajouter une règle locale", auth: "bearer" },
      {
        method: "POST",
        path: "/v1/businesses/{businessId}/api-keys",
        usage: "Créer une clé API pour l'entreprise (secret affiché une fois)",
        auth: "bearer",
      },
      { method: "GET", path: "/v1/businesses/{businessId}/api-keys", usage: "Consulter la clé API de l'entreprise", auth: "bearer" },
      { method: "PATCH", path: "/v1/businesses/{businessId}/api-keys", usage: "Renommer la clé API", auth: "bearer" },
      { method: "POST", path: "/v1/businesses/{businessId}/api-keys:revoke", usage: "Révoquer la clé API", auth: "bearer" },
    ],
  },
  {
    group: "Opérations & Consultation",
    items: [
      { method: "GET", path: "/v1/businesses/{businessId}/operations", usage: "Lister les opérations", auth: "bearer" },
      { method: "POST", path: "/v1/businesses/{businessId}/operations/{name}:execute", usage: "Exécuter une opération (200 / 202)", auth: "bearer" },
      { method: "GET", path: "/v1/businesses/{businessId}/transactions", usage: "Historique des transactions", auth: "bearer" },
      { method: "GET", path: "/v1/businesses/{businessId}/traces", usage: "Lister les traces (audit)", auth: "bearer" },
      { method: "GET", path: "/v1/businesses/{businessId}/traces/{traceId}", usage: "Suivre une opération différée", auth: "bearer" },
    ],
  },
];
