"use client";

import { useEffect, useState } from "react";
import { LoadingBlock } from "@/components/Feedback";
import { listRequetes, ApiError, type RequeteLog, type CategorieRequete } from "@/lib/api";
import { cn } from "@/lib/cn";

const CATEGORIES: { code: CategorieRequete | null; label: string; hint: string }[] = [
  { code: null, label: "Toutes", hint: "" },
  { code: "KNL_CORE", label: "KNL Core", hint: "Business Core → Kernel" },
  { code: "BUSINESS_CORE", label: "Business Core", hint: "Votre appli → Business Core" },
];

function CategorieBadge({ categorie }: { categorie: CategorieRequete }) {
  const style =
    categorie === "KNL_CORE"
      ? "border-brand/40 bg-brand/5 text-brand"
      : "border-ok/60 bg-ok-tint text-ok-strong";
  return (
    <span className={cn("inline-flex items-center rounded-full border px-2.5 py-0.5 text-[11px] font-semibold", style)}>
      {categorie === "KNL_CORE" ? "KNL Core" : "Business Core"}
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

const TAILLE = 20;

export default function RequetesTab() {
  const [categorie, setCategorie] = useState<CategorieRequete | null>(null);
  const [page, setPage] = useState(0);
  const [state, setState] = useState<{ status: "loading" | "ok" | "error"; items: RequeteLog[]; total: number }>({
    status: "loading",
    items: [],
    total: 0,
  });

  useEffect(() => {
    setState((s) => ({ ...s, status: "loading" }));
    listRequetes(categorie, page, TAILLE)
      .then((p) => setState({ status: "ok", items: p.items, total: p.total }))
      .catch((e) => {
        setState({ status: "error", items: [], total: 0 });
        void e;
      });
  }, [categorie, page]);

  const totalPages = Math.max(1, Math.ceil(state.total / TAILLE));

  return (
    <div className="mt-8">
      <p className="text-sm text-muted">
        Chaque requête <strong>KNL Core</strong> et <strong>Business Core</strong> consomme votre quota —
        ce sont les deux seules catégories facturables. Les appels que votre propre backend exécute en
        local (sans jamais toucher Business Core) ne sont pas visibles ici : Business Core n&apos;en a
        aucune connaissance.
      </p>

      <div className="mt-5 flex flex-wrap items-center gap-2">
        {CATEGORIES.map((c) => (
          <button
            key={c.label}
            onClick={() => {
              setCategorie(c.code);
              setPage(0);
            }}
            className={cn(
              "rounded-lg border px-3.5 py-1.5 text-[13px] font-semibold transition-colors",
              categorie === c.code
                ? "border-brand bg-brand text-white"
                : "border-line bg-white text-body hover:border-brand/40"
            )}
          >
            {c.label}
          </button>
        ))}
      </div>

      {state.status === "loading" && (
        <div className="mt-5 border border-line bg-white p-5">
          <LoadingBlock lines={4} />
        </div>
      )}

      {state.status === "error" && (
        <p className="mt-5 border-l-2 border-danger bg-danger/5 px-3 py-2 text-sm text-danger">
          Chargement des requêtes impossible.
        </p>
      )}

      {state.status === "ok" && state.items.length === 0 && (
        <div className="mt-5 border border-dashed border-line bg-white p-6 text-sm text-muted">
          Aucune requête {categorie ? `de catégorie « ${categorie} » ` : ""}pour l&apos;instant. Exécutez
          un appel depuis votre application — il apparaît ici en quelques secondes.
        </div>
      )}

      {state.status === "ok" && state.items.length > 0 && (
        <>
          <div className="mt-5 overflow-x-auto border border-line bg-white">
            <table className="w-full min-w-[820px] border-collapse text-sm">
              <thead>
                <tr className="border-b border-line bg-subtle text-left">
                  {["Catégorie", "Méthode", "Endpoint", "Résultat", "Durée", "Date"].map((h) => (
                    <th key={h} className="px-4 py-3 text-[11px] font-semibold uppercase tracking-wider text-muted">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {state.items.map((r) => (
                  <tr key={r.id} className="border-b border-line last:border-0">
                    <td className="px-4 py-3">
                      <CategorieBadge categorie={r.categorie} />
                    </td>
                    <td className="px-4 py-3 font-mono text-[12px] font-semibold text-ink">{r.methode}</td>
                    <td className="max-w-[320px] truncate px-4 py-3 font-mono text-[12px] text-body" title={r.endpoint}>
                      {r.endpoint}
                    </td>
                    <td className="px-4 py-3">
                      <StatutBadge statut={r.statutHttp} />
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
    </div>
  );
}
