"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { adminOverview, type AdminOverview } from "@/lib/api";
import { IconUsers, IconKey, IconBuilding, IconActivity, IconBan, IconArrowRight } from "@/components/icons";
import { cn } from "@/lib/cn";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T | null };

function fmt(n: number): string {
  return n.toLocaleString("fr-FR");
}

export default function AdminDashboardPage() {
  const [charge, setCharge] = useState<Charge<AdminOverview>>({ state: "loading", data: null });

  useEffect(() => {
    adminOverview()
      .then((data) => setCharge({ state: "ok", data }))
      .catch(() => setCharge({ state: "error", data: null }));
  }, []);

  return (
    <div className="animate-fade-up">
      <div className="border-b border-line pb-6">
        <div className="text-[12px] font-semibold uppercase tracking-wider text-brand">Plateforme</div>
        <h1 className="mt-2 font-display text-3xl font-bold text-ink">Vue d&apos;ensemble</h1>
        <p className="mt-1 text-sm text-muted">
          Activité réelle de tous les développeurs utilisant Business Core.
        </p>
      </div>

      {charge.state === "loading" && (
        <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="h-28 animate-pulse rounded-xl border border-line bg-white" />
          ))}
        </div>
      )}

      {charge.state === "error" && (
        <p className="mt-8 rounded-lg border border-danger/25 bg-danger/5 px-5 py-4 text-sm text-danger">
          Impossible de charger la vue d&apos;ensemble.
        </p>
      )}

      {charge.state === "ok" && charge.data && (
        <>
          <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            <StatTile icon={IconUsers} label="Développeurs" value={fmt(charge.data.nbDeveloppeurs)} href="/admin/developers" />
            <StatTile icon={IconBuilding} label="Entreprises créées" value={fmt(charge.data.nbEntreprises)} />
            <StatTile icon={IconKey} label="Clés API actives" value={fmt(charge.data.nbClesActives)} />
            <StatTile
              icon={IconBan}
              label="Développeurs bloqués"
              value={fmt(charge.data.nbDeveloppeursBloques)}
              warn={charge.data.nbDeveloppeursBloques > 0}
            />
          </div>

          <div className="mt-6 grid gap-5 lg:grid-cols-[1.4fr_1fr]">
            {/* Requêtes par catégorie facturable */}
            <div className="rounded-xl border border-line bg-white p-6 shadow-card">
              <div className="flex items-center gap-2 text-sm text-muted">
                <IconActivity className="h-4 w-4 text-brand" /> Requêtes facturables consommées
              </div>
              <p className="mt-1 text-xs text-muted">
                Seules ces deux catégories consomment le quota des développeurs.
              </p>
              <div className="mt-5 grid gap-4 sm:grid-cols-2">
                <div className="rounded-lg border border-line bg-subtle p-5">
                  <div className="text-xs uppercase tracking-wider text-muted">Business Core</div>
                  <div className="mt-1.5 font-display text-3xl font-bold text-ink">
                    {fmt(charge.data.requetesBusinessCore)}
                  </div>
                  <div className="mt-1 text-xs text-muted">appels reçus par Business Core</div>
                </div>
                <div className="rounded-lg border border-line bg-subtle p-5">
                  <div className="text-xs uppercase tracking-wider text-muted">Kernel Core</div>
                  <div className="mt-1.5 font-display text-3xl font-bold text-ink">
                    {fmt(charge.data.requetesKernelCore)}
                  </div>
                  <div className="mt-1 text-xs text-muted">appels sortants vers Kernel Core</div>
                </div>
              </div>
            </div>

            {/* Répartition des plans */}
            <div className="rounded-xl border border-line bg-white p-6 shadow-card">
              <div className="text-sm text-muted">Répartition des plans</div>
              <div className="mt-4 space-y-3">
                {charge.data.repartitionPlans.length === 0 && (
                  <p className="text-sm text-muted">Aucun développeur.</p>
                )}
                {charge.data.repartitionPlans.map((p) => {
                  const pct = charge.data!.nbDeveloppeurs > 0
                    ? (p.nombre / charge.data!.nbDeveloppeurs) * 100
                    : 0;
                  return (
                    <div key={p.plan}>
                      <div className="flex items-center justify-between text-sm">
                        <span className="font-medium text-ink">{p.plan}</span>
                        <span className="font-mono text-xs text-muted">{p.nombre}</span>
                      </div>
                      <div className="mt-1.5 h-2 w-full overflow-hidden rounded-full bg-subtle">
                        <div className="h-full rounded-full bg-brand" style={{ width: `${pct}%` }} />
                      </div>
                    </div>
                  );
                })}
              </div>
              <Link
                href="/admin/billing"
                className="mt-5 inline-flex items-center gap-1.5 text-[13px] font-semibold text-brand hover:underline"
              >
                Voir la facturation <IconArrowRight className="h-3.5 w-3.5" />
              </Link>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function StatTile({
  icon: Icon,
  label,
  value,
  warn,
  href,
}: {
  icon: typeof IconUsers;
  label: string;
  value: string;
  warn?: boolean;
  href?: string;
}) {
  const content = (
    <div
      className={cn(
        "h-full rounded-xl border bg-white p-6 shadow-card transition-all duration-200 hover:-translate-y-0.5 hover:shadow-card-hover",
        warn ? "border-danger/25" : "border-line"
      )}
    >
      <span
        className={cn(
          "grid h-10 w-10 place-items-center rounded-lg",
          warn ? "bg-danger/10 text-danger" : "bg-tint text-brand"
        )}
      >
        <Icon className="h-5 w-5" />
      </span>
      <div className="mt-4 font-display text-[26px] font-bold leading-none text-ink">{value}</div>
      <div className="mt-2 text-[13px] text-muted">{label}</div>
    </div>
  );
  return href ? <Link href={href}>{content}</Link> : content;
}
