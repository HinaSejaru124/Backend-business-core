"use client";

import { useEffect, useMemo, useState } from "react";
import { listMedicaments, listClients, listOrdonnances, creerVente, ApiError } from "@/lib/api";
import { useSession } from "@/lib/useSession";
import type { Medicament, Client, Ordonnance, Vente } from "@/lib/types";
import { Button } from "@/components/Button";
import Select from "@/components/Select";
import PageHeader from "@/components/PageHeader";
import BlocageKernel from "@/components/BlocageKernel";
import { useToast } from "@/components/Toast";
import { IconCart, IconSearch, IconTrash } from "@/components/icons";

type LignePanier = { medicament: Medicament; quantite: number };
type Encaissement =
  | { state: "idle" }
  | { state: "loading" }
  | { state: "ok"; vente: Vente }
  | { state: "error"; erreur: ApiError | null };

/**
 * Poste de vente — partagé par le Pharmacien Responsable et le Caissier (même écran, même geste de
 * vente au quotidien). La seule différence réelle : un médicament sur ordonnance déclenche la règle
 * « ordonnance requise » (effet DEROGER côté Business Core) — le Caissier est bloqué (doit escalader
 * au pharmacien), le Pharmacien Responsable peut vendre en fournissant un motif, tracé en audit côté
 * plateforme. PharmaCore n'invente rien : c'est Business Core qui vérifie le rôle réel de l'acteur
 * connecté, jamais ce frontend.
 */
export default function VentePage() {
  const toast = useToast();
  const { session } = useSession();
  const role = session.data?.role ?? null;
  const estPharmacien = role === "PHARMACIEN_RESPONSABLE";

  const [medicaments, setMedicaments] = useState<Medicament[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [ordonnances, setOrdonnances] = useState<Ordonnance[]>([]);
  const [recherche, setRecherche] = useState("");
  const [panier, setPanier] = useState<LignePanier[]>([]);
  const [clientId, setClientId] = useState("");
  const [ordonnanceId, setOrdonnanceId] = useState("");
  const [motifDerogation, setMotifDerogation] = useState("");
  const [encaissement, setEncaissement] = useState<Encaissement>({ state: "idle" });

  useEffect(() => {
    listMedicaments().then(setMedicaments).catch(() => {});
    listClients().then(setClients).catch(() => {});
    listOrdonnances().then(setOrdonnances).catch(() => {});
  }, []);

  /** Ordonnances valides du client sélectionné — seules celles-là ont un sens à lier à cette vente. */
  const ordonnancesDuClient = useMemo(
    () => ordonnances.filter((o) => o.clientId === clientId && o.statut === "VALIDE"),
    [ordonnances, clientId]
  );

  const resultats = recherche
    ? medicaments.filter((m) => m.nom.toLowerCase().includes(recherche.toLowerCase()))
    : [];

  function ajouter(m: Medicament) {
    setEncaissement({ state: "idle" });
    setPanier((prev) => {
      const existe = prev.find((l) => l.medicament.id === m.id);
      if (existe) {
        return prev.map((l) => (l.medicament.id === m.id ? { ...l, quantite: l.quantite + 1 } : l));
      }
      return [...prev, { medicament: m, quantite: 1 }];
    });
    setRecherche("");
  }

  function majQuantite(id: string, quantite: number) {
    setPanier((prev) => prev.map((l) => (l.medicament.id === id ? { ...l, quantite } : l)));
  }

  function retirer(id: string) {
    setPanier((prev) => prev.filter((l) => l.medicament.id !== id));
  }

  const necessiteOrdonnance = panier.some((l) => l.medicament.ordonnanceRequise);
  const total = panier.reduce((sum, l) => sum + l.medicament.prixUnitaire * l.quantite, 0);
  const ruptureStock = panier.some((l) => l.quantite > l.medicament.stockActuel);

  async function onEncaisser() {
    setEncaissement({ state: "loading" });
    try {
      const vente = await creerVente({
        clientId: clientId || undefined,
        ordonnanceId: ordonnanceId || undefined,
        modePaiement: "ESPECES",
        motifDerogation: estPharmacien && motifDerogation.trim() ? motifDerogation.trim() : undefined,
        lignes: panier.map((l) => ({ medicamentId: l.medicament.id, quantite: l.quantite })),
      });
      setEncaissement({ state: "ok", vente });
      setPanier([]);
      setOrdonnanceId("");
      setMotifDerogation("");
      toast("success", `Vente enregistrée — ${vente.montantTotal.toLocaleString("fr-FR")} XAF`);
    } catch (err) {
      const erreur = err instanceof ApiError ? err : null;
      setEncaissement({ state: "error", erreur });
      toast("error", erreur?.detail || erreur?.title || "Encaissement impossible.");
    }
  }

  return (
    <div className="animate-fade-up">
      <PageHeader eyebrow="Caisse" title="Poste de vente" />

      <div className="mt-6">
        <BlocageKernel contexte="L'encaissement" />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-[1fr_360px]">
        {/* Recherche médicament */}
        <div>
          <div className="relative">
            <IconSearch className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
            <input
              value={recherche}
              onChange={(e) => setRecherche(e.target.value)}
              placeholder="Rechercher un médicament (nom, code CIP)…"
              className="h-11 w-full rounded-xl border border-line bg-white pl-10 pr-3 text-sm outline-none transition-all focus:border-brand focus:ring-4 focus:ring-brand/10"
            />
          </div>
          {resultats.length > 0 && (
            <div className="mt-2 overflow-hidden rounded-xl border border-line bg-white shadow-card">
              {resultats.map((m) => (
                <button
                  key={m.id}
                  onClick={() => ajouter(m)}
                  className="flex w-full items-center justify-between border-b border-line px-4 py-3 text-left text-sm transition-colors last:border-0 hover:bg-brand-tint"
                >
                  <span>
                    {m.nom}
                    {m.ordonnanceRequise && <span className="ml-2 text-xs text-danger">Sur ordonnance</span>}
                  </span>
                  <span className="font-mono text-xs text-muted">
                    {m.prixUnitaire.toLocaleString("fr-FR")} XAF · stock {m.stockActuel}
                  </span>
                </button>
              ))}
            </div>
          )}

          <Select
            label="Client (optionnel)"
            id="client"
            value={clientId}
            onChange={(e) => {
              setClientId(e.target.value);
              setOrdonnanceId(""); // l'ordonnance choisie appartenait peut-être à l'ancien client
            }}
            className="mt-6"
          >
            <option value="">Vente comptant, sans client identifié</option>
            {clients.map((c) => (
              <option key={c.id} value={c.id}>
                {c.prenom ? `${c.prenom} ${c.nom}` : c.nom}
              </option>
            ))}
          </Select>

          {necessiteOrdonnance && clientId && (
            <Select
              label="Ordonnance liée à cette vente (optionnel)"
              id="ordonnance"
              value={ordonnanceId}
              onChange={(e) => setOrdonnanceId(e.target.value)}
              className="mt-4"
            >
              <option value="">Aucune ordonnance liée</option>
              {ordonnancesDuClient.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.medecinNom} — {new Date(o.dateEmission).toLocaleDateString("fr-FR")}
                </option>
              ))}
            </Select>
          )}
          {necessiteOrdonnance && clientId && ordonnancesDuClient.length === 0 && (
            <p className="mt-1.5 text-xs text-muted">
              Aucune ordonnance valide enregistrée pour ce client —{" "}
              <a href="/ordonnances/nouvelle" className="underline underline-offset-2 hover:text-brand">
                en créer une
              </a>
              .
            </p>
          )}

          {encaissement.state === "ok" && (
            <div className="mt-6 rounded-xl border-l-2 border-ok bg-ok/5 px-4 py-3 text-sm text-ink">
              <p className="font-medium">Vente enregistrée.</p>
              <p className="mt-1 font-mono text-xs text-muted">
                transactionId : {encaissement.vente.transactionKernelId ?? "—"}
                <br />
                traceId : {encaissement.vente.traceId ?? "—"}
              </p>
            </div>
          )}
          {encaissement.state === "error" && (
            <div className="mt-6 rounded-xl border-l-2 border-danger bg-danger/5 px-4 py-3 text-sm text-danger">
              <p className="font-medium">{encaissement.erreur?.title ?? "Encaissement impossible."}</p>
              {encaissement.erreur?.detail && <p className="mt-1 text-xs">{encaissement.erreur.detail}</p>}
              {encaissement.erreur?.violatedRule && (
                <p className="mt-1 font-mono text-xs text-muted">
                  règle violée : {encaissement.erreur.violatedRule}
                  {encaissement.erreur.requiredAction && ` → ${encaissement.erreur.requiredAction}`}
                </p>
              )}
            </div>
          )}
        </div>

        {/* Panier */}
        <div className="overflow-hidden rounded-2xl border border-line bg-white shadow-card">
          <div className="flex items-center gap-2 border-b border-line bg-subtle px-4 py-3">
            <IconCart className="h-4 w-4 text-brand" />
            <h2 className="font-display text-[15px] font-semibold text-ink">Panier</h2>
          </div>

          {panier.length === 0 ? (
            <p className="p-4 text-sm text-muted">Aucun article — recherchez un médicament à gauche.</p>
          ) : (
            <div className="divide-y divide-line">
              {panier.map((l) => (
                <div key={l.medicament.id} className="p-3">
                  <div className="flex items-center justify-between gap-2">
                    <span className="truncate text-sm font-medium text-ink">{l.medicament.nom}</span>
                    <button onClick={() => retirer(l.medicament.id)} className="text-muted hover:text-danger">
                      <IconTrash className="h-4 w-4" />
                    </button>
                  </div>
                  <div className="mt-1.5 flex items-center justify-between">
                    <input
                      type="number"
                      min={1}
                      value={l.quantite}
                      onChange={(e) => majQuantite(l.medicament.id, Number(e.target.value))}
                      className="h-8 w-16 rounded-lg border border-line px-2 text-sm outline-none transition-all focus:border-brand focus:ring-4 focus:ring-brand/10"
                    />
                    <span className="font-mono text-sm text-ink">
                      {(l.medicament.prixUnitaire * l.quantite).toLocaleString("fr-FR")} XAF
                    </span>
                  </div>
                  {l.quantite > l.medicament.stockActuel && (
                    <p className="mt-1 text-xs text-danger">
                      Quantité au-delà du stock local ({l.medicament.stockActuel} disponible)
                    </p>
                  )}
                </div>
              ))}
            </div>
          )}

          <div className="border-t border-line p-4">
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted">Total</span>
              <span className="font-display text-lg font-bold text-ink">{total.toLocaleString("fr-FR")} XAF</span>
            </div>

            {necessiteOrdonnance && estPharmacien && (
              <div className="mt-3">
                <label htmlFor="motif" className="mb-1.5 block text-[13px] font-medium text-ink">
                  Motif de dérogation (ordonnance)
                </label>
                <textarea
                  id="motif"
                  value={motifDerogation}
                  onChange={(e) => setMotifDerogation(e.target.value)}
                  placeholder="Ex. ordonnance vérifiée verbalement, renouvellement autorisé…"
                  rows={2}
                  className="w-full rounded-xl border border-line bg-white px-3.5 py-2 text-sm text-body outline-none transition-all placeholder:text-muted/60 focus:border-brand focus:ring-4 focus:ring-brand/10"
                />
                <p className="mt-1 text-xs text-muted">
                  Requis par Business Core pour vendre un médicament sur ordonnance en tant que
                  Pharmacien Responsable — tracé en audit.
                </p>
              </div>
            )}
            {necessiteOrdonnance && !estPharmacien && (
              <p className="mt-2 rounded-lg border-l-2 border-danger bg-danger/5 px-3.5 py-2.5 text-xs text-danger">
                Médicament sur ordonnance : seul un Pharmacien Responsable peut le vendre (avec motif).
                Escaladez cette vente.
              </p>
            )}
            {ruptureStock && (
              <p className="mt-2 rounded-lg border-l-2 border-warning bg-warning/5 px-3 py-2 text-xs text-warning">
                Quantité demandée supérieure au stock local pour au moins un article.
              </p>
            )}

            <Button
              className="mt-4 w-full"
              disabled={panier.length === 0 || encaissement.state === "loading"}
              onClick={onEncaisser}
            >
              {encaissement.state === "loading" ? "Encaissement…" : "Encaisser"}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
