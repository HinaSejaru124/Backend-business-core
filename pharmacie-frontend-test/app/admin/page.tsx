"use client";

import { useEffect, useState, type FormEvent } from "react";
import {
  provisionnerModele,
  listPersonnel,
  creerPersonnel,
  desactiverPersonnel,
  ApiError,
} from "@/lib/api";
import type { RapportProvisioning, Personnel } from "@/lib/types";
import { LIBELLE_ROLE } from "@/lib/roles";
import PageHeader from "@/components/PageHeader";
import Card from "@/components/Card";
import Field from "@/components/Field";
import Select from "@/components/Select";
import { Button } from "@/components/Button";
import { IconCheck, IconTrash } from "@/components/icons";
import { useToast } from "@/components/Toast";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T };

/**
 * Personnel — le titulaire est déjà pharmacien : la modélisation du catalogue se fait directement
 * sur /medicaments. Cette page ne garde que ce qui est propre au titulaire en tant qu'administrateur :
 * le modèle métier (déclaration idempotente, une fois) et les comptes du personnel.
 */
export default function AdminPage() {
  const toast = useToast();

  return (
    <div className="animate-fade-up">
      <PageHeader
        eyebrow="Espace titulaire"
        title="Personnel"
        description="Crée les comptes du personnel (Pharmacien, Caissier) et vérifie le modèle métier."
      />

      <div className="mt-8 space-y-8">
        <PersonnelCard toast={toast} />
        <ProvisioningCard toast={toast} />
      </div>
    </div>
  );
}

function ProvisioningCard({ toast }: { toast: (type: "success" | "error", message: string) => void }) {
  const [rapport, setRapport] = useState<RapportProvisioning | null>(null);
  const [running, setRunning] = useState(false);

  async function onProvisionner() {
    setRunning(true);
    try {
      const r = await provisionnerModele();
      setRapport(r);
      toast("success", "Modèle vérifié / déclaré.");
    } catch (err) {
      const msg = err instanceof ApiError ? err.detail || err.title : "Provisioning impossible.";
      toast("error", msg);
    } finally {
      setRunning(false);
    }
  }

  return (
    <Card
      title="Modèle métier"
      action={
        <Button size="sm" onClick={onProvisionner} disabled={running}>
          {running ? "Vérification…" : "Provisionner le modèle"}
        </Button>
      }
    >
      <p className="text-sm text-muted">
        Déclare (de façon idempotente) les rôles CAISSIER / PHARMACIEN_RESPONSABLE / CLIENT, la règle
        « ordonnance requise » (dérogation réservée au Pharmacien Responsable, motif obligatoire),
        l&apos;opération « Vendre » et la configuration (devise, TVA, seuil d&apos;alerte). Sans effet
        si déjà déclaré — relançable sans risque.
      </p>
      {rapport && (
        <ul className="mt-4 space-y-1.5 border-t border-line pt-4">
          {rapport.actions.map((a, i) => (
            <li key={i} className="flex items-start gap-2 text-[13px] text-ink">
              <IconCheck className="mt-0.5 h-3.5 w-3.5 flex-none text-brand" />
              {a}
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}

/**
 * Crée un membre du personnel (Pharmacien Responsable ou Caissier) — compte 100% propre à PharmaCore.
 * L'identité Kernel n'est résolue qu'une fois, côté serveur (cf. PersonnelService) : ce formulaire ne
 * demande qu'un mot de passe PharmaCore, jamais un mot de passe Kernel.
 */
function PersonnelCard({ toast }: { toast: (type: "success" | "error", message: string) => void }) {
  const [personnel, setPersonnel] = useState<Charge<Personnel[]>>({ state: "loading", data: [] });

  function recharger() {
    listPersonnel()
      .then((data) => setPersonnel({ state: "ok", data }))
      .catch(() => setPersonnel({ state: "error", data: [] }));
  }

  useEffect(() => {
    recharger();
  }, []);

  async function onDesactiver(id: string) {
    try {
      await desactiverPersonnel(id);
      recharger();
      toast("success", "Compte désactivé.");
    } catch (err) {
      toast("error", err instanceof ApiError ? err.detail || err.title : "Désactivation impossible.");
    }
  }

  return (
    <Card title="Personnel">
      <NouveauPersonnelForm
        onSuccess={() => {
          recharger();
          toast("success", "Compte créé — connexion possible immédiatement sur /connexion.");
        }}
        onError={(msg) => toast("error", msg)}
      />

      <div className="mt-6 border-t border-line pt-4">
        <h3 className="mb-3 font-mono text-[11px] uppercase tracking-wider text-muted">
          Personnel actuel
        </h3>
        {personnel.state === "loading" && <p className="text-sm text-muted">Chargement…</p>}
        {personnel.state === "ok" && personnel.data.length === 0 && (
          <p className="text-sm text-muted">Aucun membre du personnel — créez-en un ci-dessus.</p>
        )}
        {personnel.state === "ok" && personnel.data.length > 0 && (
          <ul className="divide-y divide-line">
            {personnel.data.map((p) => (
              <li key={p.id} className="flex items-center justify-between py-2.5 text-sm">
                <div>
                  <span className="font-medium text-ink">
                    {p.prenom} {p.nom}
                  </span>
                  <span className="ml-2 rounded-full border border-brand/25 bg-brand-tint px-2.5 py-0.5 font-mono text-[11px] font-semibold text-brand">
                    {LIBELLE_ROLE[p.role]}
                  </span>
                  {!p.actif && (
                    <span className="ml-2 rounded-full border border-line px-2.5 py-0.5 font-mono text-[11px] font-semibold text-muted">
                      désactivé
                    </span>
                  )}
                  <div className="mt-0.5 text-xs text-muted">{p.email}</div>
                </div>
                {p.actif && (
                  <button
                    onClick={() => onDesactiver(p.id)}
                    className="text-muted hover:text-danger"
                    title="Désactiver ce compte"
                  >
                    <IconTrash className="h-4 w-4" />
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </Card>
  );
}

function NouveauPersonnelForm({
  onSuccess,
  onError,
}: {
  onSuccess: () => void;
  onError: (msg: string) => void;
}) {
  const [nom, setNom] = useState("");
  const [prenom, setPrenom] = useState("");
  const [email, setEmail] = useState("");
  const [motDePasse, setMotDePasse] = useState("");
  const [role, setRole] = useState<"PHARMACIEN_RESPONSABLE" | "CAISSIER">("CAISSIER");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await creerPersonnel({ nom, prenom, email, motDePasse, role });
      setNom("");
      setPrenom("");
      setEmail("");
      setMotDePasse("");
      onSuccess();
    } catch (err) {
      const msg = err instanceof ApiError ? err.detail || err.title : "Création impossible.";
      setError(msg);
      onError(msg);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Prénom" id="p-prenom" value={prenom} onChange={(e) => setPrenom(e.target.value)} required />
        <Field label="Nom" id="p-nom" value={nom} onChange={(e) => setNom(e.target.value)} required />
      </div>
      <div className="grid gap-4 sm:grid-cols-3">
        <Field
          label="E-mail (identifiant de connexion)"
          id="p-email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="sm:col-span-2"
          required
        />
        <Select
          label="Rôle"
          id="p-role"
          value={role}
          onChange={(e) => setRole(e.target.value as typeof role)}
        >
          <option value="CAISSIER">Caissier</option>
          <option value="PHARMACIEN_RESPONSABLE">Pharmacien</option>
        </Select>
      </div>
      <Field
        label="Mot de passe (PharmaCore)"
        id="p-password"
        type="password"
        hint="8 caractères minimum — c'est ce mot de passe que ce membre utilisera sur /connexion."
        value={motDePasse}
        onChange={(e) => setMotDePasse(e.target.value)}
        required
      />
      {error && <p className="rounded-lg border-l-2 border-danger bg-danger/5 px-3.5 py-2.5 text-sm text-danger">{error}</p>}
      <Button type="submit" disabled={submitting}>
        {submitting ? "Création…" : "Créer le compte"}
      </Button>
    </form>
  );
}
