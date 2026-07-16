"use client";

import { useEffect, useState, type ChangeEvent, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { listClients, listMedicaments, creerOrdonnance, ApiError } from "@/lib/api";
import type { Client, Medicament } from "@/lib/types";
import { Button } from "@/components/Button";
import Field from "@/components/Field";
import Select from "@/components/Select";
import { IconPlus, IconTrash } from "@/components/icons";
import { useToast } from "@/components/Toast";

type Ligne = { medicamentId: string; quantitePrescrite: string; posologie: string };

export default function NouvelleOrdonnancePage() {
  const router = useRouter();
  const toast = useToast();
  const [clients, setClients] = useState<Client[]>([]);
  const [medicaments, setMedicaments] = useState<Medicament[]>([]);

  const [clientId, setClientId] = useState("");
  const [medecinNom, setMedecinNom] = useState("");
  const [medecinNumeroOrdre, setMedecinNumeroOrdre] = useState("");
  const [dateEmission, setDateEmission] = useState(() => new Date().toISOString().slice(0, 10));
  const [documentNom, setDocumentNom] = useState<string | null>(null);
  const [documentContentType, setDocumentContentType] = useState<string | null>(null);
  const [documentBase64, setDocumentBase64] = useState<string | null>(null);
  const [lignes, setLignes] = useState<Ligne[]>([{ medicamentId: "", quantitePrescrite: "1", posologie: "" }]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listClients().then(setClients).catch(() => {});
    listMedicaments().then(setMedicaments).catch(() => {});
  }, []);

  function majLigne(i: number, patch: Partial<Ligne>) {
    setLignes((prev) => prev.map((l, idx) => (idx === i ? { ...l, ...patch } : l)));
  }

  function onFichierChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) {
      setDocumentNom(null);
      setDocumentContentType(null);
      setDocumentBase64(null);
      return;
    }
    setDocumentNom(file.name);
    setDocumentContentType(file.type || "application/octet-stream");
    const reader = new FileReader();
    reader.onload = () => {
      // dataURL = "data:<type>;base64,<contenu>" — on ne garde que la partie base64.
      const result = reader.result as string;
      setDocumentBase64(result.split(",")[1] ?? null);
    };
    reader.readAsDataURL(file);
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const ordonnance = await creerOrdonnance({
        clientId,
        medecinNom,
        medecinNumeroOrdre: medecinNumeroOrdre || undefined,
        dateEmission,
        documentNom: documentNom || undefined,
        documentContentType: documentContentType || undefined,
        documentContenuBase64: documentBase64 || undefined,
        lignes: lignes
          .filter((l) => l.medicamentId)
          .map((l) => ({
            medicamentId: l.medicamentId,
            quantitePrescrite: Number(l.quantitePrescrite),
            posologie: l.posologie || undefined,
          })),
      });
      toast("success", "Ordonnance créée.");
      router.push(`/ordonnances`);
      void ordonnance;
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
      <Link href="/ordonnances" className="text-sm text-muted hover:text-ink">
        ← Retour aux ordonnances
      </Link>
      <h1 className="mt-4 pb-6 font-display text-2xl font-bold tracking-tight text-ink">
        Nouvelle ordonnance
      </h1>

      <form onSubmit={onSubmit} className="mt-6 space-y-5">
        <Select label="Client" id="client" value={clientId} onChange={(e) => setClientId(e.target.value)} required>
          <option value="">Sélectionner un client…</option>
          {clients.map((c) => (
            <option key={c.id} value={c.id}>
              {c.prenom ? `${c.prenom} ${c.nom}` : c.nom}
            </option>
          ))}
        </Select>

        <div className="grid grid-cols-2 gap-4">
          <Field label="Médecin" id="medecin" value={medecinNom} onChange={(e) => setMedecinNom(e.target.value)} required />
          <Field
            label="N° d'ordre (optionnel)"
            id="numeroOrdre"
            value={medecinNumeroOrdre}
            onChange={(e) => setMedecinNumeroOrdre(e.target.value)}
          />
        </div>

        <Field
          label="Date d'émission"
          id="date"
          type="date"
          value={dateEmission}
          onChange={(e) => setDateEmission(e.target.value)}
          required
        />

        <label className="block">
          <span className="mb-1.5 block text-[13px] font-medium text-ink">
            Document (PDF/image) — optionnel
          </span>
          <input
            type="file"
            accept="application/pdf,image/*"
            onChange={onFichierChange}
            className="block w-full text-sm text-muted file:mr-3 file:border file:border-line file:bg-white file:px-3 file:py-2 file:text-sm"
          />
          <span className="mt-1.5 block text-xs text-muted">
            {documentNom
              ? `« ${documentNom} » sera enregistré avec l'ordonnance et consultable depuis la liste.`
              : "Scan ou photo de l'ordonnance papier (ex. pour une prescription sur ordonnance)."}
          </span>
        </label>

        <div>
          <div className="mb-2 flex items-center justify-between">
            <span className="text-[13px] font-medium text-ink">Médicaments prescrits</span>
            <button
              type="button"
              onClick={() => setLignes((p) => [...p, { medicamentId: "", quantitePrescrite: "1", posologie: "" }])}
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
                  value={l.quantitePrescrite}
                  onChange={(e) => majLigne(i, { quantitePrescrite: e.target.value })}
                  className="h-10 w-20 rounded-lg border border-line px-2 text-sm outline-none transition-all focus:border-brand focus:ring-4 focus:ring-brand/10"
                  placeholder="Qté"
                />
                <input
                  value={l.posologie}
                  onChange={(e) => majLigne(i, { posologie: e.target.value })}
                  className="h-10 flex-1 rounded-lg border border-line px-2 text-sm outline-none transition-all focus:border-brand focus:ring-4 focus:ring-brand/10"
                  placeholder="Posologie (ex. 2x/jour)"
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
          {loading ? "Création…" : "Créer l'ordonnance"}
        </Button>
      </form>
    </div>
  );
}
