"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ButtonLink } from "@/components/Button";
import { useAuth } from "@/lib/auth-context";
import {
  listBusinessTypes,
  listBusinesses,
  getDashboard,
  type BusinessType,
  type Business,
  type Dashboard,
} from "@/lib/api";
import { IconArrowRight, IconKey, IconActivity, IconBook, IconBolt } from "@/components/icons";
import { cn } from "@/lib/cn";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T };

/** Mini-courbe d'usage (30 jours) en barres — sans dépendance externe. */
function Sparkline({ points }: { points: { jour: string; total: number }[] }) {
  const max = Math.max(1, ...points.map((p) => p.total));
  return (
    <div className="flex h-16 items-end gap-[3px]">
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

function StatutBadge({ value }: { value: string }) {
  const style =
    value === "PUBLIE" || value === "ACTIVE"
      ? "text-ok border-ok/30 bg-ok/5"
      : value === "ARCHIVE" || value === "FERMEE"
        ? "text-muted border-line bg-subtle"
        : "text-brand border-brand/30 bg-brand/5";
  return (
    <span className={cn("inline-block border px-2 py-0.5 font-mono text-[11px]", style)}>{value}</span>
  );
}

export default function ConsoleDashboard() {
  const { profil, principal } = useAuth();
  const [types, setTypes] = useState<Charge<BusinessType[]>>({ state: "loading", data: [] });
  const [businesses, setBusinesses] = useState<Charge<Business[]>>({ state: "loading", data: [] });
  const [usage, setUsage] = useState<Charge<Dashboard | null>>({ state: "loading", data: null });

  useEffect(() => {
    listBusinessTypes()
      .then((data) => setTypes({ state: "ok", data }))
      .catch(() => setTypes({ state: "error", data: [] }));
    listBusinesses()
      .then((data) => setBusinesses({ state: "ok", data }))
      .catch(() => setBusinesses({ state: "error", data: [] }));
    getDashboard()
      .then((data) => setUsage({ state: "ok", data }))
      .catch(() => setUsage({ state: "error", data: null }));
  }, []);

  return (
    <div className="animate-fade-up">
      {/* En-tête */}
      <div className="flex flex-col justify-between gap-4 border-b border-line pb-6 sm:flex-row sm:items-end">
        <div>
          <div className="font-mono text-[12px] uppercase tracking-wider text-brand">Console</div>
          <h1 className="mt-2 font-display text-3xl font-bold text-ink">Tableau de bord</h1>
          <p className="mt-1 text-sm text-muted">
            Connecté en tant que <span className="font-medium text-ink">{principal}</span>
            {profil?.owner && <span className="text-brand"> · Owner</span>}
          </p>
        </div>
        <ButtonLink href="/console/api-key" size="sm">
          <IconKey className="h-4 w-4" /> Ma clé d&apos;API
        </ButtonLink>
      </div>

      {/* Identité (données réelles du JWT vérifié par le backend) */}
      <div className="mt-8 grid gap-px border border-line bg-line sm:grid-cols-2">
        <div className="bg-white p-5">
          <div className="text-xs uppercase tracking-wider text-muted">Tenant</div>
          <div className="mt-1.5 truncate font-mono text-sm text-ink">{profil?.tenantId}</div>
        </div>
        <div className="bg-white p-5">
          <div className="text-xs uppercase tracking-wider text-muted">Acteur</div>
          <div className="mt-1.5 truncate font-mono text-sm text-ink">{profil?.actorId || "—"}</div>
        </div>
      </div>

      {!profil?.owner && (
        <p className="mt-4 border-l-2 border-brand bg-tint px-4 py-3 text-sm text-ink">
          Pour créer une organisation, vous devez être <strong>OWNER</strong>. Rapprochez-vous de
          l&apos;administrateur.
        </p>
      )}

      {/* Consommation d'API — comptage RÉEL des requêtes authentifiées par clé (GET /v1/dashboard) */}
      <section className="mt-10">
        <div className="flex items-baseline justify-between">
          <h2 className="flex items-center gap-2 font-display text-lg font-semibold text-ink">
            <IconBolt className="h-5 w-5 text-brand" /> Consommation d&apos;API
          </h2>
          <Link
            href="/console/pricing"
            className="font-mono text-[12px] text-brand underline-offset-2 hover:underline"
          >
            Détails &amp; plan →
          </Link>
        </div>

        {/* Plan & quota (données réelles : plan du compte + quota du catalogue backend) */}
        {usage.state === "ok" && usage.data && (
          <div
            className={cn(
              "mt-3 border p-5",
              usage.data.bloque ? "border-danger bg-danger/5" : "border-line bg-white"
            )}
          >
            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
              <div className="flex items-center gap-3">
                <span className="text-xs uppercase tracking-wider text-muted">Plan</span>
                <span className="border border-brand/35 bg-brand/5 px-2.5 py-0.5 font-mono text-[13px] font-semibold tracking-wide text-brand">
                  {usage.data.plan}
                </span>
              </div>
              <div className="text-sm">
                {usage.data.quotaMensuel < 0 ? (
                  <span className="text-ink">
                    <strong>Illimité</strong> · {usage.data.requetesCeMois.toLocaleString("fr-FR")} requêtes ce mois
                  </span>
                ) : (
                  <span className="text-ink">
                    <strong>{Math.max(0, usage.data.requetesRestantes).toLocaleString("fr-FR")}</strong>{" "}
                    / {usage.data.quotaMensuel.toLocaleString("fr-FR")} requêtes restantes ce mois
                  </span>
                )}
              </div>
            </div>

            {usage.data.quotaMensuel > 0 && (
              <div className="mt-3 h-2 w-full overflow-hidden bg-subtle">
                <div
                  className={cn("h-full", usage.data.bloque ? "bg-danger" : "bg-brand")}
                  style={{
                    width: `${Math.min(
                      100,
                      (usage.data.requetesCeMois / usage.data.quotaMensuel) * 100
                    )}%`,
                  }}
                />
              </div>
            )}

            {usage.data.bloque && (
              <p className="mt-3 text-sm text-danger">
                Quota mensuel atteint : vos clés d&apos;API sont bloquées (HTTP 402).{" "}
                <Link href="/console/pricing" className="font-semibold underline underline-offset-2">
                  Passez à un plan supérieur
                </Link>{" "}
                pour continuer.
              </p>
            )}
          </div>
        )}

        <div className="mt-3 border border-line bg-white">
          {usage.state === "loading" && <div className="p-5 text-sm text-muted">Chargement…</div>}
          {usage.state === "error" && (
            <div className="p-5 text-sm text-danger">Impossible de charger la consommation.</div>
          )}
          {usage.state === "ok" && usage.data && (
            <div className="grid gap-px bg-line lg:grid-cols-[repeat(3,minmax(0,1fr))_2fr]">
              <div className="bg-white p-5">
                <div className="text-xs uppercase tracking-wider text-muted">Requêtes ce mois</div>
                <div className="mt-1.5 font-display text-3xl font-bold text-ink">
                  {usage.data.requetesCeMois.toLocaleString("fr-FR")}
                </div>
              </div>
              <div className="bg-white p-5">
                <div className="text-xs uppercase tracking-wider text-muted">Aujourd&apos;hui</div>
                <div className="mt-1.5 font-display text-3xl font-bold text-ink">
                  {usage.data.requetesAujourdhui.toLocaleString("fr-FR")}
                </div>
              </div>
              <div className="bg-white p-5">
                <div className="text-xs uppercase tracking-wider text-muted">Taux d&apos;erreur</div>
                <div className="mt-1.5 font-display text-3xl font-bold text-ink">
                  {(usage.data.tauxErreur * 100).toFixed(1)}
                  <span className="text-lg text-muted">%</span>
                </div>
                <div className="mt-0.5 text-[11px] text-muted">
                  {usage.data.erreursAujourdhui} erreur{usage.data.erreursAujourdhui > 1 ? "s" : ""}{" "}
                  aujourd&apos;hui
                </div>
              </div>
              <div className="bg-white p-5">
                <div className="mb-2 text-xs uppercase tracking-wider text-muted">
                  30 derniers jours
                </div>
                {usage.data.sparkline.length > 0 ? (
                  <Sparkline points={usage.data.sparkline} />
                ) : (
                  <div className="py-4 text-sm text-muted">Aucune donnée.</div>
                )}
              </div>
            </div>
          )}
        </div>
        {usage.state === "ok" && usage.data && usage.data.requetesCeMois === 0 && (
          <p className="mt-3 text-xs text-muted">
            Aucune requête comptée pour l&apos;instant. Dès que vos applications appellent l&apos;API
            avec une clé <code className="font-mono">X-BC-Client-Id</code> /{" "}
            <code className="font-mono">X-BC-Api-Key</code>, la consommation s&apos;affiche ici en
            temps réel.
          </p>
        )}
      </section>

      {/* Ressources réelles du tenant */}
      <div className="mt-10 grid gap-8 xl:grid-cols-2">
        {/* Types métier */}
        <section>
          <div className="flex items-baseline justify-between">
            <h2 className="font-display text-lg font-semibold text-ink">Types métier</h2>
            <span className="font-mono text-[12px] text-muted">
              {types.state === "ok" ? `${types.data.length} au total` : ""}
            </span>
          </div>
          <div className="mt-3 border border-line bg-white">
            {types.state === "loading" && (
              <div className="p-5 text-sm text-muted">Chargement…</div>
            )}
            {types.state === "error" && (
              <div className="p-5 text-sm text-danger">Impossible de charger les types métier.</div>
            )}
            {types.state === "ok" && types.data.length === 0 && (
              <div className="p-5 text-sm text-muted">
                Aucun type métier pour l&apos;instant. Déclarez-en un via l&apos;API —{" "}
                <Link href="/console/docs" className="text-brand underline-offset-2 hover:underline">
                  voir la documentation
                </Link>
                .
              </div>
            )}
            {types.state === "ok" &&
              types.data.slice(0, 6).map((t, i) => (
                <div
                  key={t.id}
                  className={cn("flex items-center gap-4 px-5 py-3.5", i !== 0 && "border-t border-line")}
                >
                  <code className="font-mono text-[13px] font-medium text-ink">{t.code}</code>
                  <span className="truncate text-sm text-muted">{t.nom}</span>
                  <span className="ml-auto">
                    <StatutBadge value={t.statut} />
                  </span>
                </div>
              ))}
          </div>
        </section>

        {/* Entreprises */}
        <section>
          <div className="flex items-baseline justify-between">
            <h2 className="font-display text-lg font-semibold text-ink">Entreprises</h2>
            <span className="font-mono text-[12px] text-muted">
              {businesses.state === "ok" ? `${businesses.data.length} au total` : ""}
            </span>
          </div>
          <div className="mt-3 border border-line bg-white">
            {businesses.state === "loading" && (
              <div className="p-5 text-sm text-muted">Chargement…</div>
            )}
            {businesses.state === "error" && (
              <div className="p-5 text-sm text-danger">Impossible de charger les entreprises.</div>
            )}
            {businesses.state === "ok" && businesses.data.length === 0 && (
              <div className="p-5 text-sm text-muted">
                Aucune entreprise pour l&apos;instant. Créez-en une via l&apos;API (<code className="font-mono text-[12px]">POST /v1/businesses</code>).
              </div>
            )}
            {businesses.state === "ok" &&
              businesses.data.slice(0, 6).map((b, i) => (
                <div
                  key={b.id}
                  className={cn("flex items-center gap-4 px-5 py-3.5", i !== 0 && "border-t border-line")}
                >
                  <span className="truncate text-sm font-medium text-ink">{b.nom}</span>
                  <span className="font-mono text-[12px] text-muted">v{b.versionNumber}</span>
                  <span className="ml-auto">
                    <StatutBadge value={b.cycleVie} />
                  </span>
                </div>
              ))}
          </div>
        </section>
      </div>

      {/* Accès rapides */}
      <h2 className="mt-10 font-display text-lg font-semibold text-ink">Accès rapides</h2>
      <div className="mt-3 grid gap-4 md:grid-cols-3">
        {[
          { href: "/console/api-key", icon: IconKey, t: "Clé d'API", d: "Générer la clé de vos applications." },
          { href: "/console/audit", icon: IconActivity, t: "Audit", d: "Traces d'exécution de vos opérations." },
          { href: "/console/docs", icon: IconBook, t: "Documentation", d: "Référence complète de l'API." },
        ].map((c) => {
          const Icon = c.icon;
          return (
            <Link
              key={c.href}
              href={c.href}
              className="group border border-line bg-white p-5 transition-all hover:-translate-y-0.5 hover:border-brand hover:shadow-card"
            >
              <div className="flex items-center justify-between">
                <Icon className="h-5 w-5 text-brand" />
                <IconArrowRight className="h-4 w-4 text-muted transition-transform group-hover:translate-x-1" />
              </div>
              <div className="mt-3 font-display text-[15px] font-semibold text-ink">{c.t}</div>
              <p className="mt-1 text-[13px] text-muted">{c.d}</p>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
