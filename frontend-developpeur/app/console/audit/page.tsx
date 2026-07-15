"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/Button";
import { EmptyState, LoadingBlock } from "@/components/Feedback";
import RequetesTab from "./RequetesTab";
import {
  listBusinesses,
  listTraces,
  ApiError,
  type Business,
  type OperationTrace,
  type StatutTrace,
} from "@/lib/api";
import { cn } from "@/lib/cn";

const STATUT_STYLE: Record<StatutTrace, string> = {
  COMPLETEE: "text-ok border-ok/30 bg-ok/5",
  EN_COURS: "text-brand border-brand/30 bg-brand/5",
  COMPENSEE: "text-danger border-danger/30 bg-danger/5",
};

function StatutPill({ statut }: { statut: StatutTrace }) {
  return (
    <span className={cn("inline-flex items-center rounded-full border px-2.5 py-0.5 text-[11px] font-semibold", STATUT_STYLE[statut])}>
      {statut}
    </span>
  );
}

function fmt(d: string | null): string {
  return d ? d.replace("T", " ").slice(0, 16) : "—";
}

function ActivitesTab() {
  const [businesses, setBusinesses] = useState<Business[] | null>(null);
  const [bizError, setBizError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string>("");

  const [traces, setTraces] = useState<OperationTrace[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Charge les entreprises RÉELLES du tenant (GET /v1/businesses) pour la sélection.
  useEffect(() => {
    listBusinesses()
      .then((data) => {
        setBusinesses(data);
        if (data.length > 0) setSelected(data[0].id);
      })
      .catch((err) =>
        setBizError(err instanceof ApiError ? err.detail || err.title : "Chargement des entreprises impossible.")
      );
  }, []);

  async function charger() {
    if (!selected) return;
    setError(null);
    setLoading(true);
    setTraces(null);
    try {
      setTraces(await listTraces(selected));
    } catch (err) {
      setError(err instanceof ApiError ? err.detail || err.title : "Chargement des traces impossible.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mt-8">
      {bizError && (
        <p className="border-l-2 border-danger bg-danger/5 px-3 py-2 text-sm text-danger">{bizError}</p>
      )}

      {businesses && businesses.length === 0 && (
        <EmptyState
          title="Aucune entreprise"
          description="Créez une entreprise, exécutez une opération, puis revenez consulter les traces d'audit."
          actionHref="/console/businesses"
          actionLabel="Créer une entreprise →"
        />
      )}

      {businesses && businesses.length > 0 && (
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <label className="block flex-1">
            <span className="mb-1.5 block text-[13px] font-medium text-ink">Entreprise</span>
            <select
              value={selected}
              onChange={(e) => setSelected(e.target.value)}
              className="h-11 w-full border border-line bg-white px-3 text-sm text-body outline-none focus:border-brand"
            >
              {businesses.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.nom} — v{b.versionNumber} ({b.cycleVie})
                </option>
              ))}
            </select>
          </label>
          <Button onClick={charger} disabled={loading || !selected}>
            {loading ? "Chargement…" : "Charger les traces"}
          </Button>
        </div>
      )}

      {!businesses && !bizError && <LoadingBlock lines={1} />}

      {error && (
        <p className="mt-5 border-l-2 border-danger bg-danger/5 px-3 py-2 text-sm text-danger">{error}</p>
      )}

      {traces && traces.length === 0 && (
        <div className="mt-8 border border-dashed border-line bg-white p-6 text-sm text-muted">
          <p className="font-medium text-ink">Aucune trace d&apos;opération pour cette entreprise.</p>
          <p className="mt-2">
            C&apos;est normal si vos appels n&apos;ont pas encore <em>exécuté d&apos;opération</em>. Les
            lectures (catalogue, stock, synchronisation) et les tentatives rejetées <em>avant</em> tout
            effet (paramètre manquant, règle bloquante en 422) sont comptées dans votre consommation mais
            ne créent <strong>pas</strong> de trace. Une trace apparaît quand une opération
            (<code className="font-mono text-[12px]">POST …/operations/{"{name}"}:execute</code>) aboutit
            (COMPLETEE), engage puis compense un effet (COMPENSEE), ou part en différé (EN_COURS). Pour le
            détail de <strong>tous</strong> les appels HTTP (pas seulement les opérations), voir l&apos;onglet
            « Requêtes ».
          </p>
        </div>
      )}

      {traces && traces.length > 0 && (
        <div className="mt-8 overflow-x-auto border border-line bg-white">
          <table className="w-full min-w-[720px] border-collapse text-sm">
            <thead>
              <tr className="border-b border-line bg-subtle text-left">
                {["Opération", "Statut", "Clé d'idempotence", "Créée le", "Résolue le"].map((h) => (
                  <th key={h} className="px-4 py-3 text-[11px] font-semibold uppercase tracking-wider text-muted">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {traces.map((t) => (
                <tr key={t.id} className="border-b border-line last:border-0">
                  <td className="px-4 py-3 font-medium text-ink">{t.operationNom}</td>
                  <td className="px-4 py-3">
                    <StatutPill statut={t.statut} />
                  </td>
                  <td className="px-4 py-3 font-mono text-[13px] text-muted">{t.cleIdempotence || "—"}</td>
                  <td className="px-4 py-3 font-mono text-[13px] text-muted">{fmt(t.creeLe)}</td>
                  <td className="px-4 py-3 font-mono text-[13px] text-muted">{fmt(t.resoluLe)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

const ONGLETS = [
  { id: "activites", label: "Activités" },
  { id: "requetes", label: "Requêtes" },
] as const;
type OngletId = (typeof ONGLETS)[number]["id"];

export default function ConsoleAuditPage() {
  const [onglet, setOnglet] = useState<OngletId>("activites");

  return (
    <div className="animate-fade-up">
      <div className="border-b border-line pb-6">
        <div className="text-[12px] font-semibold uppercase tracking-wider text-brand">Consultation</div>
        <h1 className="mt-2 font-display text-3xl font-bold text-ink">Audit</h1>
        <p className="mt-1 text-sm text-muted">
          <strong>Activités</strong> : vos opérations métier (statut, idempotence). <strong>Requêtes</strong> :
          le détail de chaque appel HTTP, par catégorie facturable.
        </p>
      </div>

      <div className="mt-6 flex gap-1 border-b border-line">
        {ONGLETS.map((o) => (
          <button
            key={o.id}
            onClick={() => setOnglet(o.id)}
            className={cn(
              "relative px-4 py-2.5 text-sm font-semibold transition-colors",
              onglet === o.id ? "text-brand" : "text-muted hover:text-ink"
            )}
          >
            {o.label}
            {onglet === o.id && <span className="absolute inset-x-0 -bottom-px h-[2px] bg-brand" />}
          </button>
        ))}
      </div>

      {onglet === "activites" ? <ActivitesTab /> : <RequetesTab />}
    </div>
  );
}
