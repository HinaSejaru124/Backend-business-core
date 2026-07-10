"use client";

import { useCallback, useEffect, useState } from "react";
import {
  getDashboard,
  getPlans,
  upgradePlan,
  ApiError,
  type Dashboard,
  type Plan,
} from "@/lib/api";
import { Button } from "@/components/Button";
import { cn } from "@/lib/cn";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T };

/**
 * Tarifs & Consommation — vue propre au développeur connecté (résolu côté backend via le JWT :
 * un autre développeur voit son propre plan, sa propre conso, ses propres clés).
 *
 * Tout est réel : plan et quota viennent de GET /v1/dashboard, le catalogue de GET /v1/plans, et le
 * changement de plan de POST /v1/plan/upgrade (paiement via Kernel Core — simulé pour l'instant, donc
 * confirmé immédiatement). Le comptage des requêtes est réel (chaque appel par clé API est compté).
 */
function Sparkline({ points }: { points: { jour: string; total: number }[] }) {
  const max = Math.max(1, ...points.map((p) => p.total));
  return (
    <div className="flex h-20 items-end gap-[3px]">
      {points.map((p) => (
        <div
          key={p.jour}
          className={cn("flex-1", p.total > 0 ? "bg-brand" : "bg-line")}
          style={{ height: p.total > 0 ? `${Math.max(6, (p.total / max) * 100)}%` : "4px" }}
          title={`${p.jour} · ${p.total} requête${p.total > 1 ? "s" : ""}`}
        />
      ))}
    </div>
  );
}

function fmt(n: number): string {
  return n.toLocaleString("fr-FR");
}

export default function ConsolePricingPage() {
  const [usage, setUsage] = useState<Charge<Dashboard | null>>({ state: "loading", data: null });
  const [plans, setPlans] = useState<Plan[]>([]);
  const [enCours, setEnCours] = useState<string | null>(null); // code plan en cours d'upgrade
  const [message, setMessage] = useState<{ type: "ok" | "err"; texte: string } | null>(null);

  const charger = useCallback(() => {
    getDashboard()
      .then((data) => setUsage({ state: "ok", data }))
      .catch(() => setUsage({ state: "error", data: null }));
  }, []);

  useEffect(() => {
    charger();
    getPlans()
      .then(setPlans)
      .catch(() => setPlans([]));
  }, [charger]);

  const d = usage.state === "ok" ? usage.data : null;
  const planCourant = d?.plan ?? null;

  async function changerPlan(code: string) {
    if (enCours) return;
    setMessage(null);
    setEnCours(code);
    try {
      const res = await upgradePlan(code);
      if (res.statut === "CONFIRME") {
        setMessage({ type: "ok", texte: `Plan changé : vous êtes maintenant en ${res.plan}.` });
        charger(); // recharge plan + quota réels
      } else {
        setMessage({
          type: "ok",
          texte: `Paiement en attente. Finalisez le paiement pour activer le plan ${code}.`,
        });
      }
    } catch (e) {
      setMessage({
        type: "err",
        texte: e instanceof ApiError ? e.detail || e.title : "Changement de plan impossible.",
      });
    } finally {
      setEnCours(null);
    }
  }

  return (
    <div className="animate-fade-up space-y-10 py-4">
      <div className="border-b border-line pb-6">
        <div className="font-mono text-[12px] uppercase tracking-wider text-brand">Facturation</div>
        <h1 className="mt-2 font-display text-3xl font-bold text-ink">Tarifs &amp; Consommation</h1>
        <p className="mt-1 text-sm text-muted">
          Modèle à l&apos;usage : chaque requête de vos applications est comptée. Choisissez le plan
          adapté à votre volume.
        </p>
      </div>

      {message && (
        <div
          className={cn(
            "border-l-2 px-4 py-3 text-sm",
            message.type === "ok"
              ? "border-ok bg-ok/5 text-ink"
              : "border-danger bg-danger/5 text-danger"
          )}
        >
          {message.texte}
        </div>
      )}

      {/* Plan courant + jauge */}
      <div
        className={cn(
          "border p-6",
          d?.bloque ? "border-danger bg-danger/5" : "border-line bg-white"
        )}
      >
        {usage.state === "loading" && <div className="text-sm text-muted">Chargement…</div>}
        {usage.state === "error" && (
          <div className="text-sm text-danger">Impossible de charger votre plan.</div>
        )}
        {d && (
          <>
            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
              <div>
                <div className="font-mono text-xs uppercase tracking-wider text-muted">Plan actuel</div>
                <div className="mt-1.5 font-display text-2xl font-bold text-ink">{d.plan}</div>
              </div>
              <div className="text-right">
                {d.quotaMensuel < 0 ? (
                  <div className="font-display text-xl font-bold text-ink">Illimité</div>
                ) : (
                  <>
                    <div className="font-display text-xl font-bold text-ink">
                      {fmt(Math.max(0, d.requetesRestantes))} restantes
                    </div>
                    <div className="text-xs text-muted">
                      sur {fmt(d.quotaMensuel)} requêtes / mois
                    </div>
                  </>
                )}
              </div>
            </div>

            {d.quotaMensuel > 0 && (
              <div className="mt-4 h-2.5 w-full overflow-hidden bg-subtle">
                <div
                  className={cn("h-full", d.bloque ? "bg-danger" : "bg-brand")}
                  style={{ width: `${Math.min(100, (d.requetesCeMois / d.quotaMensuel) * 100)}%` }}
                />
              </div>
            )}
            <div className="mt-2 text-xs text-muted">
              {fmt(d.requetesCeMois)} requêtes consommées ce mois
              {d.bloque && (
                <span className="text-danger">
                  {" "}
                  · quota atteint, clés API bloquées (HTTP 402) — passez à un plan supérieur.
                </span>
              )}
            </div>
          </>
        )}
      </div>

      {/* Consommation réelle (30 j) */}
      {d && (
        <div className="border border-line bg-white p-6">
          <h2 className="font-display text-lg font-semibold text-ink">Votre consommation</h2>
          <div className="mt-5 grid gap-6 sm:grid-cols-3">
            <div className="border border-line bg-subtle p-5">
              <div className="font-mono text-xs font-semibold text-muted">Ce mois</div>
              <div className="mt-2 font-display text-3xl font-bold text-ink">{fmt(d.requetesCeMois)}</div>
            </div>
            <div className="border border-line bg-subtle p-5">
              <div className="font-mono text-xs font-semibold text-muted">Aujourd&apos;hui</div>
              <div className="mt-2 font-display text-3xl font-bold text-ink">
                {fmt(d.requetesAujourdhui)}
              </div>
            </div>
            <div className="border border-line bg-subtle p-5">
              <div className="font-mono text-xs font-semibold text-muted">Taux d&apos;erreur</div>
              <div className="mt-2 font-display text-3xl font-bold text-ink">
                {(d.tauxErreur * 100).toFixed(1)}
                <span className="text-lg text-muted">%</span>
              </div>
            </div>
          </div>
          <div className="mt-6">
            <div className="mb-2 font-mono text-[11px] uppercase tracking-wider text-muted">
              30 derniers jours
            </div>
            {d.sparkline.length > 0 ? (
              <Sparkline points={d.sparkline} />
            ) : (
              <div className="py-4 text-sm text-muted">Aucune donnée.</div>
            )}
          </div>
        </div>
      )}

      {/* Catalogue des plans */}
      <div>
        <h2 className="font-display text-lg font-semibold text-ink">Changer de plan</h2>
        <p className="mt-1 text-sm text-muted">
          Le paiement est géré par Kernel Core (bientôt disponible). Pour l&apos;instant le changement
          est appliqué immédiatement.
        </p>
        <div className="mt-5 grid gap-5 md:grid-cols-3">
          {plans.map((p) => {
            const actuel = planCourant === p.code;
            return (
              <div
                key={p.code}
                className={cn(
                  "flex flex-col border bg-white p-6",
                  actuel ? "border-brand shadow-card" : "border-line"
                )}
              >
                <div className="flex items-center justify-between">
                  <span className="font-display text-lg font-bold text-ink">{p.code}</span>
                  {actuel && (
                    <span className="border border-brand/35 bg-brand/5 px-2 py-0.5 font-mono text-[10px] uppercase tracking-wider text-brand">
                      Actuel
                    </span>
                  )}
                </div>
                <div className="mt-3 font-display text-2xl font-bold text-ink">
                  {p.illimite ? "Illimité" : `${fmt(p.quotaMensuel)}`}
                  {!p.illimite && (
                    <span className="text-sm font-normal text-muted"> req/mois</span>
                  )}
                </div>
                <div className="mt-1 text-xs text-muted">
                  {p.prixMensuel > 0 ? `${fmt(p.prixMensuel)} ${p.devise}/mois` : "Prix géré par Kernel Core"}
                </div>
                <div className="mt-5 flex-1" />
                {actuel ? (
                  <Button variant="secondary" size="sm" disabled className="w-full">
                    Votre plan
                  </Button>
                ) : (
                  <Button
                    size="sm"
                    className="w-full"
                    disabled={enCours !== null}
                    onClick={() => changerPlan(p.code)}
                  >
                    {enCours === p.code ? "Changement…" : `Passer à ${p.code}`}
                  </Button>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
