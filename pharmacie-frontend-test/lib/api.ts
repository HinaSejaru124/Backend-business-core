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
  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
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

// ─── Médicaments ────────────────────────────────────────────────────────────
export const listMedicaments = () => apiFetch<Medicament[]>("/api/medicaments");
export const getMedicament = (id: string) => apiFetch<Medicament>(`/api/medicaments/${id}`);
export const creerMedicament = (body: CreerMedicamentRequest) =>
  apiFetch<Medicament>("/api/medicaments", { method: "POST", body: JSON.stringify(body) });

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
