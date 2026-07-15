/**
 * Client HTTP vers pharmacie-backend-test UNIQUEMENT (jamais Business Core directement —
 * cf. frontend-test.md §1). Aucun secret BCaaS ne transite jamais par ce fichier.
 */
import type {
  Medicament,
  CreerMedicamentRequest,
  Fournisseur,
  CreerFournisseurRequest,
  Client,
  CreerClientRequest,
  Ordonnance,
  CreerOrdonnanceRequest,
  CommandeFournisseur,
  CreerCommandeRequest,
  Dashboard,
  ProblemDetail,
  StatutSession,
  RapportProvisioning,
  Personnel,
  CreerPersonnelRequest,
  Vente,
  CreerVenteRequest,
} from "./types";

export const API_BASE = (
  process.env.NEXT_PUBLIC_PHARMACORE_API_URL || "http://localhost:9090"
).replace(/\/+$/, "");

export class ApiError extends Error {
  status: number;
  title: string;
  detail?: string;
  violatedRule?: string;
  requiredAction?: string;
  requiredDocument?: string;

  constructor(status: number, problem: ProblemDetail) {
    super(problem.detail || problem.title || "Erreur");
    this.status = status;
    this.title = problem.title || "Erreur";
    this.detail = problem.detail;
    this.violatedRule = problem.violatedRule;
    this.requiredAction = problem.requiredAction;
    this.requiredDocument = problem.requiredDocument;
  }
}

async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  // La session (PharmacoreSession) vit dans le cookie JSESSIONID — "include" est nécessaire pour
  // qu'il soit envoyé/reçu en cross-origin (frontend et backend sur des ports différents en dev).
  const res = await fetch(`${API_BASE}${path}`, { ...options, headers, credentials: "include" });
  if (!res.ok) {
    let problem: ProblemDetail = { title: res.statusText };
    try {
      problem = await res.json();
    } catch {
      /* corps non-JSON */
    }
    throw new ApiError(res.status, problem);
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

// ─── Médicaments (lecture — runtime) ─────────────────────────────────────────
// La création n'est plus ici : déclarer une Offre est design-time (JWT), voir §Admin ci-dessous.
export const listMedicaments = () => apiFetch<Medicament[]>("/api/medicaments");
export const getMedicament = (id: string) => apiFetch<Medicament>(`/api/medicaments/${id}`);

// ─── Authentification unifiée (un seul écran, 3 rôles) ───────────────────────

export const login = (email: string, motDePasse: string) =>
  apiFetch<StatutSession>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, motDePasse }),
  });
export const logout = () => apiFetch<StatutSession>("/api/auth/logout", { method: "POST" });
export const statutSession = () => apiFetch<StatutSession>("/api/auth/status");

// ─── Espace admin (titulaire) — modélisation, catalogue, personnel ───────────

export const provisionnerModele = () =>
  apiFetch<RapportProvisioning>("/api/admin/modele:provisionner", { method: "POST" });
export const creerMedicamentAdmin = (body: CreerMedicamentRequest) =>
  apiFetch<Medicament>("/api/admin/medicaments", { method: "POST", body: JSON.stringify(body) });
export const supprimerMedicamentAdmin = (id: string) =>
  apiFetch<void>(`/api/admin/medicaments/${id}`, { method: "DELETE" });

export const listPersonnel = () => apiFetch<Personnel[]>("/api/admin/personnel");
export const creerPersonnel = (body: CreerPersonnelRequest) =>
  apiFetch<Personnel>("/api/admin/personnel", { method: "POST", body: JSON.stringify(body) });
export const desactiverPersonnel = (id: string) =>
  apiFetch<void>(`/api/admin/personnel/${id}`, { method: "DELETE" });

// ─── Ventes (runtime, clé API + acteur connecté) ─────────────────────────────

export const creerVente = (body: CreerVenteRequest) =>
  apiFetch<Vente>("/api/ventes", { method: "POST", body: JSON.stringify(body) });
export const listVentes = () => apiFetch<Vente[]>("/api/ventes");
export const getVente = (id: string) => apiFetch<Vente>(`/api/ventes/${id}`);

// ─── Fournisseurs ───────────────────────────────────────────────────────────
export const listFournisseurs = () => apiFetch<Fournisseur[]>("/api/fournisseurs");
export const getFournisseur = (id: string) => apiFetch<Fournisseur>(`/api/fournisseurs/${id}`);
export const creerFournisseur = (body: CreerFournisseurRequest) =>
  apiFetch<Fournisseur>("/api/fournisseurs", { method: "POST", body: JSON.stringify(body) });

// ─── Clients ────────────────────────────────────────────────────────────────
export const listClients = () => apiFetch<Client[]>("/api/clients");
export const getClient = (id: string) => apiFetch<Client>(`/api/clients/${id}`);
export const creerClient = (body: CreerClientRequest) =>
  apiFetch<Client>("/api/clients", { method: "POST", body: JSON.stringify(body) });

// ─── Ordonnances ────────────────────────────────────────────────────────────
export const listOrdonnances = () => apiFetch<Ordonnance[]>("/api/ordonnances");
export const getOrdonnance = (id: string) => apiFetch<Ordonnance>(`/api/ordonnances/${id}`);
export const creerOrdonnance = (body: CreerOrdonnanceRequest) =>
  apiFetch<Ordonnance>("/api/ordonnances", { method: "POST", body: JSON.stringify(body) });

// ─── Commandes fournisseurs ─────────────────────────────────────────────────
export const listCommandes = () => apiFetch<CommandeFournisseur[]>("/api/commandes-fournisseurs");
export const getCommande = (id: string) =>
  apiFetch<CommandeFournisseur>(`/api/commandes-fournisseurs/${id}`);
export const creerCommande = (body: CreerCommandeRequest) =>
  apiFetch<CommandeFournisseur>("/api/commandes-fournisseurs", {
    method: "POST",
    body: JSON.stringify(body),
  });
export const receptionnerCommande = (id: string) =>
  apiFetch<CommandeFournisseur>(`/api/commandes-fournisseurs/${id}/reception`, { method: "POST" });

// ─── Alertes stock & Dashboard ──────────────────────────────────────────────
export const listAlertesStock = () => apiFetch<Medicament[]>("/api/alertes-stock");
export const getDashboard = () => apiFetch<Dashboard>("/api/dashboard");
