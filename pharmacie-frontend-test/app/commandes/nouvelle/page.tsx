"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { listFournisseurs, listMedicaments, creerCommande, ApiError } from "@/lib/api";
import type { Fournisseur, Medicament } from "@/lib/types";
import { Button } from "@/components/Button";
import Field from "@/components/Field";
import Select from "@/components/Select";
import { IconPlus, IconTrash } from "@/components/icons";
import { useToast } from "@/components/Toast";

type Ligne = { medicamentId: string; quantiteCommandee: string; prixUnitaireAchat: string };

export default function NouvelleCommandePage() {
  const router = useRouter();
  const toast = useToast();
  const [fournisseurs, setFournisseurs] = useState<Fournisseur[]>([]);
  const [medicaments, setMedicaments] = useState<Medicament[]>([]);

  const [fournisseurId, setFournisseurId] = useState("");
  const [dateCommande, setDateCommande] = useState(() => new Date().toISOString().slice(0, 10));
  const [dateReceptionPrevue, setDateReceptionPrevue] = useState("");
  const [lignes, setLignes] = useState<Ligne[]>([
    { medicamentId: "", quantiteCommandee: "1", prixUnitaireAchat: "" },
  ]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listFournisseurs().then(setFournisseurs).catch(() => {});
    listMedicaments().then(setMedicaments).catch(() => {});
  }, []);

  function majLigne(i: number, patch: Partial<Ligne>) {
    setLignes((prev) => prev.map((l, idx) => (idx === i ? { ...l, ...patch } : l)));
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const commande = await creerCommande({
        fournisseurId,
        dateCommande,
        dateReceptionPrevue: dateReceptionPrevue || undefined,
        lignes: lignes
          .filter((l) => l.medicamentId)
          .map((l) => ({
            medicamentId: l.medicamentId,
            quantiteCommandee: Number(l.quantiteCommandee),
            prixUnitaireAchat: Number(l.prixUnitaireAchat),
          })),
      });
      toast("success", "Commande créée.");
      router.push(`/commandes/${commande.id}`);
    } catch (err) {
      const msg = err instanceof ApiError ? err.detail || err.title : "Création impossible.";
      setError(msg);
      toast("error", msg);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="animate-fade-up max-w-2xl">
      <Link href="/commandes" className="text-sm text-muted hover:text-ink">
        ← Retour aux commandes
      </Link>
      <h1 className="mt-4 pb-6 font-display text-2xl font-bold tracking-tight text-ink">
        Nouvelle commande fournisseur
      </h1>

      <form onSubmit={onSubmit} className="mt-6 space-y-5">
        <Select
          label="Fournisseur"
          id="fournisseur"
          value={fournisseurId}
          onChange={(e) => setFournisseurId(e.target.value)}
          required
        >
          <option value="">Sélectionner un fournisseur…</option>
          {fournisseurs.map((f) => (
            <option key={f.id} value={f.id}>
              {f.nom}
            </option>
          ))}
        </Select>

        <div className="grid grid-cols-2 gap-4">
          <Field
            label="Date de commande"
            id="dateCommande"
            type="date"
            value={dateCommande}
            onChange={(e) => setDateCommande(e.target.value)}
            required
          />
          <Field
            label="Réception prévue (optionnel)"
            id="dateReception"
            type="date"
            value={dateReceptionPrevue}
            onChange={(e) => setDateReceptionPrevue(e.target.value)}
          />
        </div>

        <div>
          <div className="mb-2 flex items-center justify-between">
            <span className="text-[13px] font-medium text-ink">Médicaments commandés</span>
            <button
              type="button"
              onClick={() =>
                setLignes((p) => [...p, { medicamentId: "", quantiteCommandee: "1", prixUnitaireAchat: "" }])
              }
              className="flex items-center gap-1 text-xs font-medium text-brand hover:underline"
            >
              <IconPlus className="h-3.5 w-3.5" /> Ajouter une ligne
            </button>
          </div>
          <div className="space-y-3">
            {lignes.map((l, i) => (
              <div key={i} className="flex items-end gap-2 rounded-xl border border-line bg-white p-3">
                <div className="flex-1">
                  <select
                    value={l.medicamentId}
                    onChange={(e) => majLigne(i, { medicamentId: e.target.value })}
                    className="h-10 w-full rounded-lg border border-line bg-white px-2 text-sm outline-none transition-all focus:border-brand focus:ring-4 focus:ring-brand/10"
                  >
                    <option value="">Médicament…</option>
                    {medicaments.map((m) => (
                      <option key={m.id} value={m.id}>
                        {m.nom}
                      </option>
                    ))}
                  </select>
                </div>
                <input
                  type="number"
                  min={1}
                  value={l.quantiteCommandee}
                  onChange={(e) => majLigne(i, { quantiteCommandee: e.target.value })}
                  className="h-10 w-20 rounded-lg border border-line px-2 text-sm outline-none transition-all focus:border-brand focus:ring-4 focus:ring-brand/10"
                  placeholder="Qté"
                />
                <input
                  type="number"
                  min={0}
                  value={l.prixUnitaireAchat}
                  onChange={(e) => majLigne(i, { prixUnitaireAchat: e.target.value })}
                  className="h-10 w-28 rounded-lg border border-line px-2 text-sm outline-none transition-all focus:border-brand focus:ring-4 focus:ring-brand/10"
                  placeholder="Prix achat"
                />
                {lignes.length > 1 && (
                  <button
                    type="button"
                    onClick={() => setLignes((p) => p.filter((_, idx) => idx !== i))}
                    className="grid h-10 w-10 flex-none place-items-center rounded-lg text-muted transition-colors hover:bg-danger/5 hover:text-danger"
                    aria-label="Retirer"
                  >
                    <IconTrash className="h-4 w-4" />
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>

        {error && <p className="rounded-lg border-l-2 border-danger bg-danger/5 px-3.5 py-2.5 text-sm text-danger">{error}</p>}
        <Button type="submit" disabled={loading}>
          {loading ? "Création…" : "Créer la commande"}
        </Button>
      </form>
    </div>
  );
}
