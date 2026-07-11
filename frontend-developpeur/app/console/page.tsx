"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ButtonLink } from "@/components/Button";
import { LoadingBlock, OnboardingSteps } from "@/components/Feedback";
import { useAuth } from "@/lib/auth-context";
import {
  listBusinessTypes,
  listBusinesses,
  getDashboard,
  type BusinessType,
  type Business,
  type Dashboard,
} from "@/lib/api";
import { IconArrowRight, IconKey, IconActivity, IconBook, IconBolt, IconBuilding, IconCopy, IconCheck } from "@/components/icons";
import { cn } from "@/lib/cn";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T };

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

function BarChart({ items, labelKey }: { items: { [k: string]: string | number }[]; labelKey: string }) {
  const max = Math.max(1, ...items.map((i) => Number(i.total)));
  return (
    <div className="space-y-2">
      {items.map((item) => (
        <div key={String(item[labelKey])} className="flex items-center gap-3">
          <span className="w-28 truncate text-sm text-ink">{String(item[labelKey])}</span>
          <div className="h-2 flex-1 bg-subtle">
            <div
              className="h-full bg-brand"
              style={{ width: `${(Number(item.total) / max) * 100}%` }}
            />
          </div>
          <span className="w-12 text-right font-mono text-[12px] text-muted">{Number(item.total)}</span>
        </div>
      ))}
    </div>
  );
}

function StatutBadge({ value }: { value: string }) {
  const style =
    value === "PUBLIE" || value === "ACTIVE" || value === "COMPLETEE"
      ? "text-ok border-ok/30 bg-ok/5"
      : value === "ARCHIVE" || value === "FERMEE"
        ? "text-muted border-line bg-subtle"
        : "text-brand border-brand/30 bg-brand/5";
  return (
    <span className={cn("inline-block border px-2 py-0.5 font-mono text-[11px]", style)}>{value}</span>
  );
}

function CopyableId({ value }: { value: string }) {
  const [copied, setCopied] = useState(false);
  function copier() {
    navigator.clipboard.writeText(value).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1400);
    });
  }
  return (
    <div className="flex items-center gap-2">
      <span className="truncate font-mono text-sm text-ink">{value}</span>
      <button type="button" onClick={copier} className="flex-none text-muted hover:text-brand" aria-label="Copier">
        {copied ? <IconCheck className="h-3.5 w-3.5 text-ok" /> : <IconCopy className="h-3.5 w-3.5" />}
      </button>
    </div>
  );
}

function fmtInstant(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString("fr-FR", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" });
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

  const dash = usage.data;
  const onboardingSteps = [
    { label: "Compte créé et connecté", done: true },
    {
      label: "Au moins une entreprise",
      done: (dash?.nombreEntreprises ?? 0) > 0 || businesses.data.length > 0,
      href: "/console/businesses",
    },
    {
      label: "Au moins une clé API active",
      done: (dash?.nombreClesActives ?? 0) > 0,
      href: "/console/api-key",
    },
  ];

  return (
    <div className="animate-fade-up">
      <div className="flex flex-col justify-between gap-4 border-b border-line pb-6 sm:flex-row sm:items-end">
        <div>
          <div className="font-mono text-[12px] uppercase tracking-wider text-brand">Console</div>
          <h1 className="mt-2 font-display text-3xl font-bold text-ink">Tableau de bord</h1>
          <p className="mt-1 text-sm text-muted">
            Connecté en tant que <span className="font-medium text-ink">{principal}</span>
            {profil?.owner && <span className="text-brand"> · Owner</span>}
            {profil?.plan && (
              <span className="text-muted">
                {" "}
                · Plan <span className="font-mono text-ink">{profil.plan}</span>
              </span>
            )}
          </p>
        </div>
        <ButtonLink href="/console/api-key" size="sm">
          <IconKey className="h-4 w-4" /> Mes clés d&apos;API
        </ButtonLink>
      </div>

      {/* Parcours de démarrage */}
      <div className="mt-8">
        <OnboardingSteps steps={onboardingSteps} />
      </div>

      {/* Identité */}
      <div className="mt-8 grid gap-px border border-line bg-line sm:grid-cols-3">
        <div className="bg-white p-5">
          <div className="text-xs uppercase tracking-wider text-muted">Developer ID</div>
          <div className="mt-1.5">
            {profil?.developerId ? <CopyableId value={profil.developerId} /> : "—"}
          </div>
          <div className="mt-1 text-[11px] text-muted">Valeur de X-BC-Client-Id</div>
        </div>
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
          Pour créer une entreprise, vous devez être <strong>OWNER</strong>. Rapprochez-vous de
          l&apos;administrateur.
        </p>
      )}

      {/* Résumé entreprises / clés */}
      {dash && (
        <div className="mt-8 grid gap-px border border-line bg-line sm:grid-cols-2">
          <div className="bg-white p-5">
            <div className="text-xs uppercase tracking-wider text-muted">Entreprises</div>
            <div className="mt-1.5 font-display text-3xl font-bold text-ink">{dash.nombreEntreprises}</div>
            <Link href="/console/businesses" className="mt-1 text-xs text-brand hover:underline">
              Gérer →
            </Link>
          </div>
          <div className="bg-white p-5">
            <div className="text-xs uppercase tracking-wider text-muted">Clés API actives</div>
            <div className="mt-1.5 font-display text-3xl font-bold text-ink">{dash.nombreClesActives}</div>
            <Link href="/console/api-key" className="mt-1 text-xs text-brand hover:underline">
              Gérer →
            </Link>
          </div>
        </div>
      )}

      {/* Consommation d'API */}
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
                    width: `${Math.min(100, (usage.data.requetesCeMois / usage.data.quotaMensuel) * 100)}%`,
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
          {usage.state === "loading" && <LoadingBlock />}
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
                  {usage.data.erreursAujourdhui} erreur{usage.data.erreursAujourdhui > 1 ? "s" : ""} aujourd&apos;hui
                </div>
              </div>
              <div className="bg-white p-5">
                <div className="mb-2 text-xs uppercase tracking-wider text-muted">30 derniers jours</div>
                {usage.data.sparkline.length > 0 ? (
                  <Sparkline points={usage.data.sparkline} />
                ) : (
                  <div className="py-4 text-sm text-muted">Aucune donnée.</div>
                )}
              </div>
            </div>
          )}
        </div>
      </section>

      {/* Top opérations & entreprises */}
      {dash && (dash.topOperations.length > 0 || dash.topEntreprises.length > 0) && (
        <div className="mt-10 grid gap-8 xl:grid-cols-2">
          {dash.topOperations.length > 0 && (
            <section>
              <h2 className="font-display text-lg font-semibold text-ink">Top opérations (30 j)</h2>
              <div className="mt-3 border border-line bg-white p-5">
                <BarChart items={dash.topOperations.map((o) => ({ nom: o.nom, total: o.total }))} labelKey="nom" />
              </div>
            </section>
          )}
          {dash.topEntreprises.length > 0 && (
            <section>
              <h2 className="font-display text-lg font-semibold text-ink">Top entreprises (30 j)</h2>
              <div className="mt-3 border border-line bg-white p-5">
                <BarChart items={dash.topEntreprises.map((e) => ({ nom: e.nom, total: e.total }))} labelKey="nom" />
              </div>
            </section>
          )}
        </div>
      )}

      {/* Activité récente */}
      {dash && dash.activiteRecente.length > 0 && (
        <section className="mt-10">
          <h2 className="font-display text-lg font-semibold text-ink">Activité récente</h2>
          <div className="mt-3 overflow-x-auto border border-line bg-white">
            <table className="w-full min-w-[600px] border-collapse text-sm">
              <thead>
                <tr className="border-b border-line bg-subtle text-left">
                  {["Entreprise", "Opération", "Statut", "Date"].map((h) => (
                    <th key={h} className="px-4 py-3 font-mono text-[11px] uppercase tracking-wider text-muted">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {dash.activiteRecente.map((a, i) => (
                  <tr key={`${a.entrepriseId}-${a.creeLe}-${i}`} className="border-b border-line last:border-0">
                    <td className="px-4 py-3 font-medium text-ink">{a.entrepriseNom}</td>
                    <td className="px-4 py-3 text-muted">{a.operationNom}</td>
                    <td className="px-4 py-3">
                      <StatutBadge value={a.statut} />
                    </td>
                    <td className="px-4 py-3 font-mono text-[13px] text-muted">{fmtInstant(a.creeLe)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* Ressources */}
      <div className="mt-10 grid gap-8 xl:grid-cols-2">
        <section>
          <div className="flex items-baseline justify-between">
            <h2 className="font-display text-lg font-semibold text-ink">Types métier</h2>
            <span className="font-mono text-[12px] text-muted">
              {types.state === "ok" ? `${types.data.length} au total` : ""}
            </span>
          </div>
          <div className="mt-3 border border-line bg-white">
            {types.state === "loading" && <LoadingBlock lines={2} />}
            {types.state === "error" && (
              <div className="p-5 text-sm text-danger">Impossible de charger les types métier.</div>
            )}
            {types.state === "ok" && types.data.length === 0 && (
              <div className="p-5 text-sm text-muted">
                Aucun type métier.{" "}
                <Link href="/console/docs" className="text-brand underline-offset-2 hover:underline">
                  Voir la documentation
                </Link>
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

        <section>
          <div className="flex items-baseline justify-between">
            <h2 className="font-display text-lg font-semibold text-ink">Entreprises</h2>
            <Link href="/console/businesses" className="font-mono text-[12px] text-brand hover:underline">
              Gérer →
            </Link>
          </div>
          <div className="mt-3 border border-line bg-white">
            {businesses.state === "loading" && <LoadingBlock lines={2} />}
            {businesses.state === "error" && (
              <div className="p-5 text-sm text-danger">Impossible de charger les entreprises.</div>
            )}
            {businesses.state === "ok" && businesses.data.length === 0 && (
              <div className="p-5 text-sm text-muted">
                Aucune entreprise.{" "}
                <Link href="/console/businesses" className="text-brand underline-offset-2 hover:underline">
                  Créer une entreprise
                </Link>
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

      <h2 className="mt-10 font-display text-lg font-semibold text-ink">Accès rapides</h2>
      <div className="mt-3 grid gap-4 md:grid-cols-3">
        {[
          { href: "/console/businesses", icon: IconBuilding, t: "Entreprises", d: "Créer et lister vos entreprises." },
          { href: "/console/api-key", icon: IconKey, t: "Clés d'API", d: "Gérer les clés par entreprise." },
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
