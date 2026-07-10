"use client";

import { useEffect, useState } from "react";
import { listMedicaments, listClients } from "@/lib/api";
import type { Medicament, Client } from "@/lib/types";
import { Button } from "@/components/Button";
import Select from "@/components/Select";
import PageHeader from "@/components/PageHeader";
import BlocageKernel from "@/components/BlocageKernel";
import { IconCart, IconSearch, IconTrash } from "@/components/icons";

type LignePanier = { medicament: Medicament; quantite: number };

/**
 * Écran de caisse — panier fonctionnel (recherche, quantités, client, détection ordonnance requise),
 * mais l'encaissement final est désactivé : POST /api/ventes n'existe pas encore côté backend
 * (bloqué par le Kernel, cf. BlocageKernel). Construire un panier faux qui "réussirait" silencieusement
 * serait justement le genre de donnée inventée que ce projet s'interdit.
 */
export default function VentePage() {
  const [medicaments, setMedicaments] = useState<Medicament[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [recherche, setRecherche] = useState("");
  const [panier, setPanier] = useState<LignePanier[]>([]);
  const [clientId, setClientId] = useState("");

  useEffect(() => {
    listMedicaments().then(setMedicaments).catch(() => {});
    listClients().then(setClients).catch(() => {});
  }, []);

  const resultats = recherche
    ? medicaments.filter((m) => m.nom.toLowerCase().includes(recherche.toLowerCase()))
    : [];

  function ajouter(m: Medicament) {
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

  return (
    <div className="animate-fade-up">
      <PageHeader eyebrow="Caisse" title="Vente" />

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
              className="h-11 w-full border border-line bg-white pl-10 pr-3 text-sm outline-none focus:border-brand"
            />
          </div>
          {resultats.length > 0 && (
            <div className="mt-2 border border-line bg-white">
              {resultats.map((m) => (
                <button
                  key={m.id}
                  onClick={() => ajouter(m)}
                  className="flex w-full items-center justify-between border-b border-line px-4 py-3 text-left text-sm last:border-0 hover:bg-brand-tint"
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
            onChange={(e) => setClientId(e.target.value)}
            className="mt-6"
          >
            <option value="">Vente comptant, sans client identifié</option>
            {clients.map((c) => (
              <option key={c.id} value={c.id}>
                {c.prenom ? `${c.prenom} ${c.nom}` : c.nom}
              </option>
            ))}
          </Select>
        </div>

        {/* Panier */}
        <div className="border border-line bg-white">
          <div className="flex items-center gap-2 border-b border-line px-4 py-3">
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
                      className="h-8 w-16 border border-line px-2 text-sm outline-none focus:border-brand"
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

            {necessiteOrdonnance && (
              <p className="mt-2 border-l-2 border-danger bg-danger/5 px-3 py-2 text-xs text-danger">
                Ordonnance requise pour au moins un article — la vraie vérification a lieu côté Business
                Core à l&apos;exécution de l&apos;opération (règle EXIGER).
              </p>
            )}
            {ruptureStock && (
              <p className="mt-2 border-l-2 border-warning bg-warning/5 px-3 py-2 text-xs text-warning">
                Quantité demandée supérieure au stock local pour au moins un article.
              </p>
            )}

            <Button className="mt-4 w-full" disabled title="Indisponible tant que le blocage Kernel n'est pas levé">
              Encaisser — indisponible
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
