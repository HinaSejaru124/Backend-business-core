/**
 * Types alignés exactement sur les DTOs réels exposés par pharmacie-backend-test (:9090).
 * Aucun champ inventé — voir backend-test.md §4 et le code source des *Dtos.java correspondants.
 */

export type Medicament = {
  id: string;
  offreId: string;
  nom: string;
  dci: string | null;
  formeGalenique: string | null;
  codeCip: string | null;
  categorie: "medicament_libre" | "medicament_prescription";
  ordonnanceRequise: boolean;
  prixUnitaire: number;
  stockActuel: number;
  seuilAlerte: number;
  fournisseurId: string | null;
  statut: string;
  creeLe: string;
};

export type CreerMedicamentRequest = {
  nom: string;
  dci?: string;
  formeGalenique?: string;
  codeCip?: string;
  categorie: "medicament_libre" | "medicament_prescription";
  ordonnanceRequise: boolean;
  prixUnitaire: number;
  stockInitial: number;
  seuilAlerte: number;
  fournisseurId?: string;
};

export type Fournisseur = {
  id: string;
  nom: string;
  contactNom: string | null;
  contactTelephone: string | null;
  email: string | null;
  delaiLivraisonJours: number | null;
  creeLe: string;
};

export type CreerFournisseurRequest = {
  nom: string;
  contactNom?: string;
  contactTelephone?: string;
  email?: string;
  delaiLivraisonJours?: number;
};

export type Client = {
  id: string;
  nom: string;
  prenom: string | null;
  telephone: string | null;
  email: string | null;
  adresse: string | null;
  beneficiaireId: string | null;
  creeLe: string;
};

export type CreerClientRequest = {
  nom: string;
  prenom?: string;
  telephone?: string;
  email?: string;
  adresse?: string;
};

export type OrdonnanceLigne = {
  id: string;
  medicamentId: string;
  quantitePrescrite: number;
  posologie: string | null;
};

export type Ordonnance = {
  id: string;
  clientId: string;
  medecinNom: string;
  medecinNumeroOrdre: string | null;
  dateEmission: string;
  documentNom: string | null;
  /** true si un fichier a réellement été stocké (pas juste son nom) — cf. GET /api/ordonnances/{id}/document. */
  documentDisponible: boolean;
  documentIdBcaas: string | null;
  statut: string;
  creeLe: string;
  lignes: OrdonnanceLigne[];
};

export type CreerOrdonnanceRequest = {
  clientId: string;
  medecinNom: string;
  medecinNumeroOrdre?: string;
  dateEmission: string;
  documentNom?: string;
  documentContentType?: string;
  /** Contenu du fichier encodé en base64 (lu via FileReader côté navigateur). */
  documentContenuBase64?: string;
  lignes: { medicamentId: string; quantitePrescrite: number; posologie?: string }[];
};

export type CommandeLigne = {
  id: string;
  medicamentId: string;
  quantiteCommandee: number;
  quantiteRecue: number | null;
  prixUnitaireAchat: number;
};

export type CommandeFournisseur = {
  id: string;
  fournisseurId: string;
  statut: "BROUILLON" | "ENVOYEE" | "RECUE" | "ANNULEE";
  dateCommande: string;
  dateReceptionPrevue: string | null;
  dateReceptionReelle: string | null;
  creeLe: string;
  lignes: CommandeLigne[];
};

export type CreerCommandeRequest = {
  fournisseurId: string;
  dateCommande: string;
  dateReceptionPrevue?: string;
  lignes: { medicamentId: string; quantiteCommandee: number; prixUnitaireAchat: number }[];
};

export type Dashboard = {
  totalMedicaments: number;
  alertesStockActives: number;
  chiffreAffairesDuJour: number;
  nombreVentesDuJour: number;
};

/** RFC 7807 — format d'erreur réel du backend Pharmacie (relayé tel quel depuis BCaaS le cas échéant). */
export type ProblemDetail = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  violatedRule?: string;
  requiredAction?: string;
  requiredDocument?: string;
};
