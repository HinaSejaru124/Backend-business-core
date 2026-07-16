"use client";

import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import {
  adminDevelopers,
  adminTrack,
  type AdminDeveloperRow,
  type AdminRequetePage,
  type PeriodeRequete,
  type StatutRequete,
} from "@/lib/api";
import { IconSearch } from "@/components/icons";
import { cn } from "@/lib/cn";

type CategorieFiltre = "" | "KNL_CORE" | "BUSINESS_CORE";
const METHODES = ["", "GET", "POST", "PUT", "PATCH", "DELETE"];

function fmtInstant(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString("fr-FR", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

function CategorieBadge({ categorie }: { categorie: string }) {
  const knl = categorie === "KNL_CORE";
  return (
    <span
      className={cn(
        "inline-block rounded-full border px-2.5 py-0.5 text-[10px] font-semibold uppercase tracking-wider",
        knl ? "border-brand/35 bg-brand/5 text-brand" : "border-ok/40 bg-ok-tint text-ok-strong"
      )}
    >
      {knl ? "Kernel Core" : "Business Core"}
    </span>
  );
}

function StatutHttp({ code }: { code: number }) {
  const err = code >= 400;
  return (
    <span className={cn("font-mono text-[13px] font-semibold", err ? "text-danger" : "text-ok-strong")}>
      {code || "—"}
    </span>
  );
}

export default function AdminTrackPage() {
  const searchParams = useSearchParams();
  const devParam = searchParams.get("dev") ?? "";

  const [devs, setDevs] = useState<AdminDeveloperRow[]>([]);
  const [selected, setSelected] = useState<string>(devParam);
  const [categorie, setCategorie] = useState<CategorieFiltre>("");
  const [statut, setStatut] = useState<StatutRequete | "">("");
  const [periode, setPeriode] = useState<PeriodeRequete | "">("");
  const [methode, setMethode] = useState("");
  const [recherche, setRecherche] = useState("");
  const [page, setPage] = useState<AdminRequetePage | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    adminDevelopers()
      .then((data) => {
        setDevs(data);
        if (!devParam && data.length > 0) setSelected(data[0].id);
      })
      .catch(() => setDevs([]));
  }, [devParam]);

  useEffect(() => {
    if (!selected) return;
    setLoading(true);
    adminTrack(
      selected,
      { categorie: categorie || null, statut: statut || null, periode: periode || null, methode: methode || null },
      0,
      50
    )
      .then(setPage)
      .catch(() => setPage(null))
      .finally(() => setLoading(false));
  }, [selected, categorie, statut, periode, methode]);

  const devCourant = useMemo(() => devs.find((d) => d.id === selected), [devs, selected]);
  const itemsFiltres = useMemo(() => {
    if (!page) return [];
    if (!recherche.trim()) return page.items;
    const t = recherche.trim().toLowerCase();
    return page.items.filter((r) => r.endpoint.toLowerCase().includes(t));
  }, [page, recherche]);

  return (
    <div className="animate-fade-up">
      <div className="border-b border-line pb-6">
        <div className="text-[12px] font-semibold uppercase tracking-wider text-brand">Traçabilité</div>
        <h1 className="mt-2 font-display text-3xl font-bold text-ink">Track des requêtes</h1>
        <p className="mt-1 text-sm text-muted">
          Requêtes <strong>facturables</strong> d&apos;un développeur (Kernel Core + Business Core) — les
          requêtes propres de son application (non facturables) ne sont pas visibles ici.
        </p>
      </div>

      <div className="mt-8">
        <label className="block max-w-md">
          <span className="mb-1.5 block text-[13px] font-medium text-ink">Développeur</span>
          <select
            value={selected}
            onChange={(e) => setSelected(e.target.value)}
            className="h-11 w-full rounded-lg border border-line bg-white px-3 text-sm text-ink outline-none focus:border-brand"
          >
            {devs.length === 0 && <option value="">Aucun développeur</option>}
            {devs.map((d) => (
              <option key={d.id} value={d.id}>{d.email} — {d.plan}</option>
            ))}
          </select>
        </label>

        <div className="mt-4 space-y-3 rounded-xl border border-line bg-white p-4">
          <div className="flex flex-wrap items-center gap-1.5">
            {([{ v: "", label: "Toutes" }, { v: "BUSINESS_CORE", label: "Business Core" }, { v: "KNL_CORE", label: "Kernel Core" }] as { v: CategorieFiltre; label: string }[]).map((c) => (
              <button
                key={c.v}
                onClick={() => setCategorie(c.v)}
                className={cn(
                  "rounded-lg border px-3.5 py-1.5 text-[13px] font-semibold transition-colors",
                  categorie === c.v ? "border-brand bg-brand text-white" : "border-line bg-white text-muted hover:text-ink"
                )}
              >
                {c.label}
              </button>
            ))}
          </div>
          <div className="flex flex-wrap items-center gap-1.5 border-t border-line pt-3">
            {([{ v: "", label: "Tous" }, { v: "OK", label: "Réussies" }, { v: "ERREUR", label: "Échouées" }] as { v: StatutRequete | ""; label: string }[]).map((s) => (
              <button
                key={s.label}
                onClick={() => setStatut(s.v)}
                className={cn(
                  "rounded-lg border px-3 py-1 text-[12px] font-semibold transition-colors",
                  statut === s.v ? "border-ok bg-ok-tint text-ok-strong" : "border-line bg-white text-muted hover:border-ok/40"
                )}
              >
                {s.label}
              </button>
            ))}
            <span className="mx-1 h-4 w-px bg-line" />
            {([{ v: "", label: "Tout" }, { v: "JOUR", label: "Aujourd'hui" }, { v: "SEMAINE", label: "7 jours" }, { v: "MOIS", label: "30 jours" }] as { v: PeriodeRequete | ""; label: string }[]).map((p) => (
              <button
                key={p.label}
                onClick={() => setPeriode(p.v)}
                className={cn(
                  "rounded-lg border px-3 py-1 text-[12px] font-semibold transition-colors",
                  periode === p.v ? "border-brand bg-tint text-brand" : "border-line bg-white text-muted hover:border-brand/40"
                )}
              >
                {p.label}
              </button>
            ))}
            <span className="mx-1 h-4 w-px bg-line" />
            <select
              value={methode}
              onChange={(e) => setMethode(e.target.value)}
              className="rounded-lg border border-line bg-white px-2.5 py-1 text-[12px] font-semibold text-body outline-none focus:border-brand"
            >
              {METHODES.map((m) => <option key={m} value={m}>{m || "Toutes méthodes"}</option>)}
            </select>
          </div>
          <div className="relative border-t border-line pt-3">
            <IconSearch className="pointer-events-none absolute left-2.5 top-1/2 mt-1.5 h-4 w-4 -translate-y-1/2 text-muted" />
            <input
              value={recherche}
              onChange={(e) => setRecherche(e.target.value)}
              placeholder="Rechercher un endpoint…"
              className="h-9 w-full max-w-sm rounded-lg border border-line bg-white pl-8 pr-3 text-[13px] outline-none focus:border-brand"
            />
          </div>
        </div>
      </div>

      {devCourant && (
        <p className="mt-4 text-xs text-muted">
          {page ? `${page.total.toLocaleString("fr-FR")} requête(s) au total` : ""}
          {devCourant.illimite ? "" : ` · consommation du mois : ${devCourant.consoMois.toLocaleString("fr-FR")} / ${devCourant.quota.toLocaleString("fr-FR")}`}
        </p>
      )}

      <div className="mt-6">
        {loading && <p className="text-sm text-muted">Chargement…</p>}
        {!loading && page && (
          <div className="overflow-x-auto rounded-xl border border-line bg-white shadow-card">
            <table className="w-full min-w-[820px] border-collapse text-sm">
              <thead>
                <tr className="border-b border-line bg-subtle text-left">
                  {["Catégorie", "Méthode", "Endpoint", "Statut", "Durée", "Date"].map((h) => (
                    <th key={h} className="px-5 py-3.5 text-[11px] font-semibold uppercase tracking-wider text-muted">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {itemsFiltres.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-5 py-8 text-center text-sm text-muted">Aucune requête pour ce filtre.</td>
                  </tr>
                )}
                {itemsFiltres.map((r, i) => (
                  <tr key={r.id} className={cn("transition-colors hover:bg-subtle", i !== 0 && "border-t border-line")}>
                    <td className="px-5 py-3.5"><CategorieBadge categorie={r.categorie} /></td>
                    <td className="px-5 py-3.5 font-mono text-[12px] font-semibold text-ink">{r.methode}</td>
                    <td className="px-5 py-3.5 font-mono text-[12px] text-muted">{r.endpoint}</td>
                    <td className="px-5 py-3.5"><StatutHttp code={r.statutHttp} /></td>
                    <td className="px-5 py-3.5 font-mono text-[12px] text-muted">{r.dureeMs} ms</td>
                    <td className="px-5 py-3.5 font-mono text-[12px] text-muted">{fmtInstant(r.creeLe)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
