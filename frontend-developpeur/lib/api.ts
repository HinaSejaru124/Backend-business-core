/**
 * Client d'appel à l'API Business Core.
 * Base URL via NEXT_PUBLIC_API_BASE_URL. Auth = JWT kernel (login) rejoué en `Authorization: Bearer`.
 * Erreurs au format RFC 7807 (application/problem+json). AUCUN endpoint inventé — cf. lib/endpoints.ts.
 */

export const API_BASE = (process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080").replace(/\/+$/, "");

const TOKEN_KEY = "bc_access_token";
const PRINCIPAL_KEY = "bc_principal";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_KEY);
}
export function setToken(token: string): void {
  if (typeof window !== "undefined") window.localStorage.setItem(TOKEN_KEY, token);
}
export function clearToken(): void {
  if (typeof window !== "undefined") {
    window.localStorage.removeItem(TOKEN_KEY);
    window.localStorage.removeItem(PRINCIPAL_KEY);
  }
}

/** E-mail utilisé au login (identité affichée dans l'UI — jamais inventée). */
export function getPrincipal(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(PRINCIPAL_KEY);
}
function setPrincipal(principal: string): void {
  if (typeof window !== "undefined") window.localStorage.setItem(PRINCIPAL_KEY, principal);
}

export class ApiError extends Error {
  status: number;
  title: string;
  detail?: string;
  constructor(status: number, title: string, detail?: string) {
    super(detail || title);
    this.status = status;
    this.title = title;
    this.detail = detail;
  }
}

async function toError(res: Response): Promise<ApiError> {
  let title = res.statusText || "Erreur";
  let detail: string | undefined;
  try {
    const data = await res.json();
    title = data.title || title;
    detail = data.detail;
  } catch {
    /* corps non-JSON */
  }
  return new ApiError(res.status, title, detail);
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit & { auth?: boolean } = {}
): Promise<T> {
  const { auth = true, headers, body, ...rest } = options;
  const h = new Headers(headers);
  if (body && !h.has("Content-Type")) h.set("Content-Type", "application/json");
  if (auth) {
    const token = getToken();
    if (token) h.set("Authorization", `Bearer ${token}`);
  }
  const res = await fetch(`${API_BASE}${path}`, { ...rest, body, headers: h });
  if (res.status === 401 && auth) {
    clearToken();
    if (typeof window !== "undefined") {
      window.dispatchEvent(new Event("bc:session-expired"));
    }
  }
  if (!res.ok) throw await toError(res);
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

// ─── Types alignés sur le backend ─────────────────────────────────────────

export type Organisation = {
  organizationId: string;
  organizationCode: string;
  displayName: string;
  services: string[];
};

export type LoginResult = {
  accessToken: string;
  tokenType?: string;
  expiresInSeconds: number;
  authorities: string[];
  organisations: Organisation[];
  owner: boolean;
};

export type Me = {
  tenantId: string;
  actorId: string | null;
  permissions: string[];
  owner: boolean;
  /** Identifiant développeur BC stable — valeur de X-BC-Client-Id. */
  developerId: string;
  email: string;
  plan: string;
};

// ─── Appels auth ──────────────────────────────────────────────────────────

/** POST /v1/auth/login — connexion (public). Stocke le token + l'identité en cas de succès. */
export async function login(principal: string, password: string): Promise<LoginResult> {
  const result = await apiFetch<LoginResult>("/v1/auth/login", {
    method: "POST",
    auth: false,
    body: JSON.stringify({ principal, password }),
  });
  setToken(result.accessToken);
  setPrincipal(principal);
  return result;
}

/** GET /v1/auth/me — profil courant (Bearer). */
export function me(): Promise<Me> {
  return apiFetch<Me>("/v1/auth/me");
}

export function logout(): void {
  clearToken();
}

// ─── Inscription développeur ──────────────────────────────────────────────
// POST /v1/registration crée le compte kernel + compte développeur local.
// Aucune clé API n'est émise : le développeur se connecte, crée une entreprise,
// puis génère une clé scopée à cette entreprise.

export type InscriptionResponse = {
  plan: string;
  message: string;
};

/** POST /v1/registration — crée un compte développeur (public). */
export function registerDeveloper(
  firstName: string,
  lastName: string,
  email: string,
  password: string,
  planCode?: string
): Promise<InscriptionResponse> {
  return apiFetch<InscriptionResponse>("/v1/registration", {
    method: "POST",
    auth: false,
    body: JSON.stringify({ firstName, lastName, email, password, planCode }),
  });
}

// ─── Clés API par entreprise (console développeur, JWT) ─────────────────────
// Une seule clé ACTIVE par entreprise. X-BC-Client-Id = developerId (GET /v1/auth/me).

/** Clé API d'une entreprise (jamais le secret). */
export type BusinessApiKey = {
  id: string;
  name: string;
  status: "ACTIVE" | "REVOKED";
  createdAt: string | null;
  lastUsedAt: string | null;
  entrepriseId: string;
};

/** Clé fraîchement créée — le secret (`apiKey`) n'est renvoyé qu'ici, une seule fois. */
export type BusinessApiKeyCreated = {
  id: string;
  apiKey: string;
  name: string;
  entrepriseId: string;
};

/** GET /v1/businesses/{businessId}/api-keys — clé de l'entreprise (Bearer). */
export function getBusinessApiKey(businessId: string): Promise<BusinessApiKey> {
  return apiFetch<BusinessApiKey>(`/v1/businesses/${encodeURIComponent(businessId)}/api-keys`);
}

/** POST /v1/businesses/{businessId}/api-keys — crée une clé. Secret affiché une seule fois. */
export function createBusinessApiKey(businessId: string, name?: string): Promise<BusinessApiKeyCreated> {
  return apiFetch<BusinessApiKeyCreated>(`/v1/businesses/${encodeURIComponent(businessId)}/api-keys`, {
    method: "POST",
    body: JSON.stringify({ name: name && name.trim() ? name.trim() : null }),
  });
}

/** PATCH /v1/businesses/{businessId}/api-keys — renomme la clé active. */
export function renameBusinessApiKey(businessId: string, name: string): Promise<BusinessApiKey> {
  return apiFetch<BusinessApiKey>(`/v1/businesses/${encodeURIComponent(businessId)}/api-keys`, {
    method: "PATCH",
    body: JSON.stringify({ name }),
  });
}

/** POST /v1/businesses/{businessId}/api-keys:revoke — révocation immédiate. */
export function revokeBusinessApiKey(businessId: string): Promise<BusinessApiKey> {
  return apiFetch<BusinessApiKey>(
    `/v1/businesses/${encodeURIComponent(businessId)}/api-keys:revoke`,
    { method: "POST" }
  );
}

// ─── Tableau de bord d'usage (console développeur, JWT) ─────────────────────

export type UsagePoint = { jour: string; total: number };

export type TopOperation = { nom: string; total: number };

export type TopEntreprise = { entrepriseId: string; nom: string; total: number };

export type ActiviteItem = {
  entrepriseId: string;
  entrepriseNom: string;
  operationNom: string;
  statut: string;
  creeLe: string;
};

export type Dashboard = {
  plan: string;
  quotaMensuel: number;
  requetesRestantes: number;
  bloque: boolean;
  requetesCeMois: number;
  requetesAujourdhui: number;
  erreursAujourdhui: number;
  tauxErreur: number;
  sparkline: UsagePoint[];
  nombreEntreprises: number;
  nombreClesActives: number;
  topOperations: TopOperation[];
  topEntreprises: TopEntreprise[];
  activiteRecente: ActiviteItem[];
};

/** GET /v1/dashboard — synthèse d'usage + métriques agrégées (Bearer). */
export function getDashboard(): Promise<Dashboard> {
  return apiFetch<Dashboard>("/v1/dashboard");
}

/**
 * GET /v1/dashboard/sparkline?jours=N — courbe d'usage sur une fenêtre choisie (sélecteur de période).
 * Granularité réelle journalière (nos compteurs n'ont pas de grain horaire) — n'affecte jamais le quota
 * mensuel, toujours calculé sur le mois calendaire réel par GET /v1/dashboard.
 */
export function getSparkline(jours: number): Promise<UsagePoint[]> {
  return apiFetch<UsagePoint[]>(`/v1/dashboard/sparkline?jours=${jours}`);
}

// ─── Plans & facturation ────────────────────────────────────────────────────

export type Plan = {
  code: string;
  quotaMensuel: number;
  illimite: boolean;
  prixMensuel: number;
  devise: string;
};

/** GET /v1/plans — catalogue des plans (public). */
export function getPlans(): Promise<Plan[]> {
  return apiFetch<Plan[]>("/v1/plans", { auth: false });
}

export type UpgradeResult = {
  plan: string;
  statut: "CONFIRME" | "EN_ATTENTE";
  urlPaiement: string | null;
  reference: string | null;
};

/** POST /v1/plan/upgrade — change de plan (Bearer). */
export function upgradePlan(targetPlan: string): Promise<UpgradeResult> {
  return apiFetch<UpgradeResult>("/v1/plan/upgrade", {
    method: "POST",
    body: JSON.stringify({ targetPlan }),
  });
}

// ─── Types métier & Entreprises ─────────────────────────────────────────────

export type BusinessType = {
  id: string;
  code: string;
  nom: string;
  statut: string;
};

/** GET /v1/business-types — types métier du tenant courant (Bearer). */
export function listBusinessTypes(): Promise<BusinessType[]> {
  return apiFetch<BusinessType[]>("/v1/business-types");
}

export type BusinessTypeVersion = {
  id: string;
  typeMetierId: string;
  numero: number;
  immuable: boolean;
  publieeLe: string | null;
  libelle: string;
};

/** GET /v1/business-types/{typeId}/versions — versions d'un type (Bearer). */
export function listBusinessTypeVersions(typeId: string): Promise<BusinessTypeVersion[]> {
  return apiFetch<BusinessTypeVersion[]>(
    `/v1/business-types/${encodeURIComponent(typeId)}/versions`
  );
}

export type Business = {
  id: string;
  nom: string;
  typeId: string;
  versionNumber: number;
  organizationId: string | null;
  cycleVie: string;
};

export type CreateBusinessRequest = {
  typeId: string;
  versionNumber: number;
  nom: string;
};

/** GET /v1/businesses — entreprises du tenant courant (Bearer). */
export function listBusinesses(): Promise<Business[]> {
  return apiFetch<Business[]>("/v1/businesses");
}

/** POST /v1/businesses — crée une entreprise (Bearer). */
export function createBusiness(req: CreateBusinessRequest): Promise<Business> {
  return apiFetch<Business>("/v1/businesses", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

// ─── Traces d'opération (Audit) ─────────────────────────────────────────────

export type StatutTrace = "EN_COURS" | "COMPLETEE" | "COMPENSEE";

export type OperationTrace = {
  id: string;
  operationNom: string;
  cleIdempotence: string | null;
  transactionKernelId: string | null;
  statut: StatutTrace;
  creeLe: string | null;
  resoluLe: string | null;
};

/** GET /v1/businesses/{businessId}/traces — traces d'opérations (Bearer). */
export function listTraces(businessId: string): Promise<OperationTrace[]> {
  return apiFetch<OperationTrace[]>(`/v1/businesses/${encodeURIComponent(businessId)}/traces`);
}

// ─── Journal détaillé des requêtes (Audit / Requêtes) ────────────────────────
// Deux catégories réelles seulement : KNL_CORE (Business Core → Kernel) et BUSINESS_CORE
// (clé API → Business Core) — le backend propre du développeur n'est jamais visible depuis
// Business Core, donc jamais journalisé ni affiché ici. Ce sont les 2 seules catégories facturables.

export type CategorieRequete = "KNL_CORE" | "BUSINESS_CORE";

export type RequeteLog = {
  id: string;
  categorie: CategorieRequete;
  methode: string;
  endpoint: string;
  statutHttp: number;
  dureeMs: number;
  creeLe: string;
};

export type RequeteLogPage = {
  items: RequeteLog[];
  total: number;
  page: number;
  taille: number;
};

/** GET /v1/requetes — journal détaillé, paginé, filtrable par catégorie (Bearer). */
export function listRequetes(
  categorie?: CategorieRequete | null,
  page: number = 0,
  taille: number = 20
): Promise<RequeteLogPage> {
  const params = new URLSearchParams();
  if (categorie) params.set("categorie", categorie);
  params.set("page", String(page));
  params.set("taille", String(taille));
  return apiFetch<RequeteLogPage>(`/v1/requetes?${params.toString()}`);
}
