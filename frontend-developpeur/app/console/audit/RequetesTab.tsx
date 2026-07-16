"use client";

import { useEffect, useState } from "react";
import { LoadingBlock } from "@/components/Feedback";
import {
  listRequetes,
  type RequeteLog,
  type CategorieRequete,
  type PeriodeRequete,
  type StatutRequete,
} from "@/lib/api";
import { IconSearch } from "@/components/icons";
import { cn } from "@/lib/cn";

const CATEGORIES: { code: CategorieRequete | null; label: string }[] = [
  { code: null, label: "Toutes" },
  { code: "KNL_CORE", label: "Kernel Core" },
  { code: "BUSINESS_CORE", label: "Business Core" },
  { code: "APP", label: "Mon application" },
];
const STATUTS: { code: StatutRequete | null; label: string }[] = [
  { code: null, label: "Tous" },
  { code: "OK", label: "Réussies" },
  { code: "ERREUR", label: "Échouées" },
];
const PERIODES: { code: PeriodeRequete | null; label: string }[] = [
  { code: null, label: "Tout" },
  { code: "JOUR", label: "Aujourd'hui" },
  { code: "SEMAINE", label: "7 jours" },
  { code: "MOIS", label: "30 jours" },
];
const METHODES = ["", "GET", "POST", "PUT", "PATCH", "DELETE"];

function CategorieBadge({ categorie }: { categorie: CategorieRequete }) {
  const style =
    categorie === "KNL_CORE"
      ? "border-brand/40 bg-brand/5 text-brand"
      : categorie === "BUSINESS_CORE"
        ? "border-ok/60 bg-ok-tint text-ok-strong"
        : "border-line bg-subtle text-muted";
  const label = categorie === "KNL_CORE" ? "Kernel Core" : categorie === "BUSINESS_CORE" ? "Business Core" : "Mon appli";
  return (
    <span className={cn("inline-flex items-center rounded-full border px-2.5 py-0.5 text-[11px] font-semibold", style)}>
      {label}
    </span>
  );
}

function StatutBadge({ statut }: { statut: number }) {
  const style =
    statut >= 200 && statut < 300
      ? "border-ok/60 bg-ok-tint text-ok-strong"
      : statut >= 400
        ? "border-danger/40 bg-danger/5 text-danger"
        : "border-line bg-subtle text-muted";
  return (
    <span className={cn("inline-flex items-center rounded-full border px-2 py-0.5 font-mono text-[11px] font-semibold", style)}>
      {statut}
    </span>
  );
}

function fmt(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString("fr-FR", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

const TAILLE = 25;

export default function RequetesTab() {
  const [categorie, setCategorie] = useState<CategorieRequete | null>(null);
  const [statut, setStatut] = useState<StatutRequete | null>(null);
  const [periode, setPeriode] = useState<PeriodeRequete | null>(null);
  const [methode, setMethode] = useState("");
  const [recherche, setRecherche] = useState("");
  const [page, setPage] = useState(0);
  const [state, setState] = useState<{ status: "loading" | "ok" | "error"; items: RequeteLog[]; total: number }>({
    status: "loading",
    items: [],
    total: 0,
  });

  useEffect(() => {
    setState((s) => ({ ...s, status: "loading" }));
    listRequetes({ categorie, statut, periode, methode: methode || null, recherche }, page, TAILLE)
      .then((p) => setState({ status: "ok", items: p.items, total: p.total }))
      .catch(() => setState({ status: "error", items: [], total: 0 }));
  }, [categorie, statut, periode, methode, recherche, page]);

  const totalPages = Math.max(1, Math.ceil(state.total / TAILLE));

  return (
    <div className="mt-8">
      {/* Filtres */}
      <div className="space-y-3 rounded-xl border border-line bg-white p-4">
        <div className="flex flex-wrap items-center gap-2">
          {CATEGORIES.map((c) => (
            <button
              key={c.label}
              onClick={() => { setCategorie(c.code); setPage(0); }}
              className={cn(
                "rounded-lg border px-3.5 py-1.5 text-[13px] font-semibold transition-colors",
                categorie === c.code ? "border-brand bg-brand text-white" : "border-line bg-white text-body hover:border-brand/40"
              )}
            >
              {c.label}
            </button>
          ))}
        </div>
        <div className="flex flex-wrap items-center gap-2 border-t border-line pt-3">
          {STATUTS.map((s) => (
            <button
              key={s.label}
              onClick={() => { setStatut(s.code); setPage(0); }}
              className={cn(
                "rounded-lg border px-3 py-1 text-[12px] font-semibold transition-colors",
                statut === s.code ? "border-ok bg-ok-tint text-ok-strong" : "border-line bg-white text-muted hover:border-ok/40"
              )}
            >
              {s.label}
            </button>
          ))}
          <span className="mx-1 h-4 w-px bg-line" />
          {PERIODES.map((p) => (
            <button
              key={p.label}
              onClick={() => { setPeriode(p.code); setPage(0); }}
              className={cn(
                "rounded-lg border px-3 py-1 text-[12px] font-semibold transition-colors",
                periode === p.code ? "border-brand bg-tint text-brand" : "border-line bg-white text-muted hover:border-brand/40"
              )}
            >
              {p.label}
            </button>
          ))}
          <span className="mx-1 h-4 w-px bg-line" />
          <select
            value={methode}
            onChange={(e) => { setMethode(e.target.value); setPage(0); }}
            className="rounded-lg border border-line bg-white px-2.5 py-1 text-[12px] font-semibold text-body outline-none focus:border-brand"
          >
            {METHODES.map((m) => (
              <option key={m} value={m}>{m || "Toutes méthodes"}</option>
            ))}
          </select>
        </div>
        <div className="relative border-t border-line pt-3">
          <IconSearch className="pointer-events-none absolute left-2.5 top-1/2 mt-1.5 h-4 w-4 -translate-y-1/2 text-muted" />
          <input
            value={recherche}
            onChange={(e) => { setRecherche(e.target.value); setPage(0); }}
            placeholder="Rechercher un endpoint (ex. /api/medicaments)…"
            className="h-9 w-full max-w-sm rounded-lg border border-line bg-white pl-8 pr-3 text-[13px] outline-none focus:border-brand"
          />
        </div>
      </div>

      {state.status === "loading" && (
        <div className="mt-5 rounded-xl border border-line bg-white p-5">
          <LoadingBlock lines={4} />
        </div>
      )}
      {state.status === "error" && (
        <p className="mt-5 rounded-lg border-l-2 border-danger bg-danger/5 px-3 py-2 text-sm text-danger">
          Chargement des requêtes impossible.
        </p>
      )}
      {state.status === "ok" && state.items.length === 0 && (
        <div className="mt-5 rounded-xl border border-dashed border-line bg-white p-6 text-sm text-muted">
          Aucune requête pour ce filtre. Exécutez un appel depuis votre application — il apparaît ici en
          quelques secondes.
        </div>
      )}

      {state.status === "ok" && state.items.length > 0 && (
        <>
          <div className="mt-5 overflow-x-auto rounded-xl border border-line bg-white shadow-card">
            <table className="w-full min-w-[860px] border-collapse text-sm">
              <thead>
                <tr className="border-b border-line bg-subtle text-left">
                  {["Catégorie", "Méthode", "Endpoint", "Résultat", "Facturable", "Durée", "Date"].map((h) => (
                    <th key={h} className="px-4 py-3 text-[11px] font-semibold uppercase tracking-wider text-muted">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {state.items.map((r) => (
                  <tr key={r.id} className="border-b border-line last:border-0 hover:bg-subtle">
                    <td className="px-4 py-3"><CategorieBadge categorie={r.categorie} /></td>
                    <td className="px-4 py-3 font-mono text-[12px] font-semibold text-ink">{r.methode}</td>
                    <td className="max-w-[280px] truncate px-4 py-3 font-mono text-[12px] text-body" title={r.endpoint}>
                      {r.endpoint}
                    </td>
                    <td className="px-4 py-3"><StatutBadge statut={r.statutHttp} /></td>
                    <td className="px-4 py-3">
                      {r.facturable ? (
                        <span className="text-[12px] font-semibold text-ok-strong">Oui</span>
                      ) : (
                        <span className="text-[12px] text-muted">Non</span>
                      )}
                    </td>
                    <td className="px-4 py-3 font-mono text-[12px] text-muted">{r.dureeMs} ms</td>
                    <td className="px-4 py-3 font-mono text-[12px] text-muted">{fmt(r.creeLe)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-4 flex items-center justify-between text-sm text-muted">
            <span>
              {state.total.toLocaleString("fr-FR")} requête{state.total > 1 ? "s" : ""} — page {page + 1} / {totalPages}
            </span>
            <div className="flex gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="rounded-lg border border-line px-3 py-1.5 font-semibold text-body transition-colors hover:border-brand/40 disabled:opacity-40"
              >
                ← Précédent
              </button>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page + 1 >= totalPages}
                className="rounded-lg border border-line px-3 py-1.5 font-semibold text-body transition-colors hover:border-brand/40 disabled:opacity-40"
              >
                Suivant →
              </button>
            </div>
          </div>
        </>
      )}

      {/* Explication honnête des 3 catégories */}
      <div className="mt-6 rounded-xl border border-line bg-subtle p-5 text-[13px] leading-relaxed text-muted">
        <p className="mb-2 font-semibold text-ink">Trois catégories de requêtes</p>
        <p>
          <strong className="text-ink">Kernel Core</strong> — appels sortants de Business Core vers Kernel (identité, stock, paiement).{" "}
          <strong className="text-ink">Business Core</strong> — appels reçus par Business Core depuis votre application.{" "}
          <strong className="text-ink">Mon application</strong> — appels internes à votre propre backend (ex. lecture de votre base locale), rapportés pour information.
        </p>
        <p className="mt-2">
          Seules <strong className="text-ink">Kernel Core</strong> et <strong className="text-ink">Business Core</strong> sont
          facturables et comptent dans votre quota mensuel — « Mon application » est affichée à titre indicatif uniquement.
        </p>
      </div>
    </div>
  );
}
