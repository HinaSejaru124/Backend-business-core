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
    // Token expiré/invalide : on purge la session locale ET on prévient l'app (AuthProvider)
    // pour que l'état affiché (navbar, garde console) ne reste pas figé sur "connecté" alors
    // que le token réel est mort — sinon l'UI ment jusqu'au prochain rechargement complet.
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
};

// ─── Appels ───────────────────────────────────────────────────────────────

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

// ─── Clé d'API (inscription développeur = création de compte + émission de clé) ─
// Le backend expose aussi POST /v1/auth/register (compte kernel seul, sans clé) ;
// le frontend utilise exclusivement le flux unifié ci-dessous (POST /v1/registration).

export type ApiKeyResponse = {
  clientId: string;
  apiKey: string;
  plan: string;
};

/** POST /v1/registration — émet une clé Business Core (affichée une seule fois). Public. */
export function requestApiKey(
  firstName: string,
  lastName: string,
  email: string,
  password: string,
  planCode?: string
): Promise<ApiKeyResponse> {
  return apiFetch<ApiKeyResponse>("/v1/registration", {
    method: "POST",
    auth: false,
    body: JSON.stringify({ firstName, lastName, email, password, planCode }),
  });
}

// ─── Gestion multi-clés (console développeur, JWT) ──────────────────────────
// Chaque appel est authentifié par le Bearer JWT du développeur connecté ; le
// backend (ResoudreDeveloppeurCourant) résout SON compte à partir du token, si
// bien que chaque développeur ne voit et ne gère QUE ses propres clés. Aucune
// isolation à faire côté frontend — elle est garantie côté serveur.

/** Une clé API telle que listée par le backend (jamais le secret). */
export type ApiKey = {
  id: string;
  /** Préfixe public = `X-BC-Client-Id`. */
  prefix: string;
  name: string;
  status: "ACTIVE" | "REVOKED";
  createdAt: string | null;
  lastUsedAt: string | null;
};

/** Clé fraîchement créée — le secret (`apiKey`) n'est renvoyé qu'ici, une seule fois. */
export type ApiKeyCreated = {
  id: string;
  clientId: string;
  apiKey: string;
  name: string;
};

/** GET /v1/api-keys — clés ACTIVE du développeur courant (Bearer). */
export function listApiKeys(): Promise<ApiKey[]> {
  return apiFetch<ApiKey[]>("/v1/api-keys");
}

/** POST /v1/api-keys — crée une clé. Le secret n'est présent que dans cette réponse. */
export function createApiKey(name?: string): Promise<ApiKeyCreated> {
  return apiFetch<ApiKeyCreated>("/v1/api-keys", {
    method: "POST",
    body: JSON.stringify({ name: name && name.trim() ? name.trim() : null }),
  });
}

/** PATCH /v1/api-keys/{id} — renomme une clé. */
export function renameApiKey(id: string, name: string): Promise<ApiKey> {
  return apiFetch<ApiKey>(`/v1/api-keys/${encodeURIComponent(id)}`, {
    method: "PATCH",
    body: JSON.stringify({ name }),
  });
}

/** POST /v1/api-keys/{id}:revoke — révocation immédiate d'une clé. */
export function revokeApiKey(id: string): Promise<ApiKey> {
  return apiFetch<ApiKey>(`/v1/api-keys/${encodeURIComponent(id)}:revoke`, {
    method: "POST",
  });
}

// ─── Tableau de bord d'usage (console développeur, JWT) ─────────────────────
// Alimenté par le comptage réel des requêtes authentifiées par clé API
// (UsageTrackingWebFilter → Redis → api_key_usage_daily). Zéro tant qu'aucune
// requête n'a été faite — c'est réel, pas simulé.

export type UsagePoint = { jour: string; total: number };

export type Dashboard = {
  /** Plan tarifaire courant (FREE / PRO / ENTERPRISE). */
  plan: string;
  /** Quota mensuel de requêtes (-1 = illimité). */
  quotaMensuel: number;
  /** Requêtes restantes ce mois (-1 = illimité). */
  requetesRestantes: number;
  /** Quota atteint : le compte est bloqué en clé API jusqu'à l'upgrade. */
  bloque: boolean;
  requetesCeMois: number;
  requetesAujourdhui: number;
  erreursAujourdhui: number;
  /** Taux d'erreur du jour, 0–1. */
  tauxErreur: number;
  /** 30 derniers jours, du plus ancien au plus récent. */
  sparkline: UsagePoint[];
  cles: ApiKey[];
};

/** GET /v1/dashboard — synthèse d'usage (30 j) + plan/quota + clés du développeur courant (Bearer). */
export function getDashboard(): Promise<Dashboard> {
  return apiFetch<Dashboard>("/v1/dashboard");
}

// ─── Plans & facturation ────────────────────────────────────────────────────

export type Plan = {
  code: string;
  /** Quota mensuel (-1 = illimité). */
  quotaMensuel: number;
  illimite: boolean;
  /** Prix d'affichage (le paiement réel est géré par Kernel Core). */
  prixMensuel: number;
  devise: string;
};

/** GET /v1/plans — catalogue des plans (public). */
export function getPlans(): Promise<Plan[]> {
  return apiFetch<Plan[]>("/v1/plans", { auth: false });
}

export type UpgradeResult = {
  plan: string;
  /** CONFIRME = plan actif tout de suite ; EN_ATTENTE = paiement à finaliser. */
  statut: "CONFIRME" | "EN_ATTENTE";
  urlPaiement: string | null;
  reference: string | null;
};

/** POST /v1/plan/upgrade — change de plan (paiement via Kernel Core, simulé pour l'instant). Bearer. */
export function upgradePlan(targetPlan: string): Promise<UpgradeResult> {
  return apiFetch<UpgradeResult>("/v1/plan/upgrade", {
    method: "POST",
    body: JSON.stringify({ targetPlan }),
  });
}


// ─── Types métier & Entreprises (données réelles du tenant) ─────────────────

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

export type Business = {
  id: string;
  nom: string;
  typeId: string;
  versionNumber: number;
  organizationId: string | null;
  cycleVie: string;
};

/** GET /v1/businesses — entreprises du tenant courant (Bearer). */
export function listBusinesses(): Promise<Business[]> {
  return apiFetch<Business[]>("/v1/businesses");
}

// ─── Traces d'opération (Audit) ──────────────────────────────────────────────

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
