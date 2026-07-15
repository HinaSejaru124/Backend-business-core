"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ButtonLink, Button } from "@/components/Button";
import { LoadingBlock, OnboardingSteps } from "@/components/Feedback";
import LineChart from "@/components/LineChart";
import DonutChart from "@/components/DonutChart";
import UpgradeModal from "@/components/UpgradeModal";
import { useAuth } from "@/lib/auth-context";
import {
  listBusinessTypes,
  listBusinesses,
  getDashboard,
  getSparkline,
  type BusinessType,
  type Business,
  type Dashboard,
  type UsagePoint,
} from "@/lib/api";
import { IconArrowRight, IconKey, IconActivity, IconBook, IconBuilding, IconCopy, IconCheck, IconCrown } from "@/components/icons";
import { cn } from "@/lib/cn";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T };

function BarChart({ items, labelKey }: { items: { [k: string]: string | number }[]; labelKey: string }) {
  const max = Math.max(1, ...items.map((i) => Number(i.total)));
  return (
    <div className="space-y-2">
      {items.map((item) => (
        <div key={String(item[labelKey])} className="flex items-center gap-3">
          <span className="w-28 truncate text-sm text-ink">{String(item[labelKey])}</span>
          <div className="h-2 flex-1 bg-subtle">
            <div className="h-full bg-brand" style={{ width: `${(Number(item.total) / max) * 100}%` }} />
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
      ? "text-ok-strong border-ok/60 bg-ok-tint"
      : value === "ARCHIVE" || value === "FERMEE"
        ? "text-muted border-line bg-subtle"
        : "text-brand border-brand/40 bg-brand/5";
  return (
    <span className={cn("inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-[11px] font-semibold", style)}>
      {(value === "PUBLIE" || value === "ACTIVE" || value === "COMPLETEE") && (
        <span className="h-1.5 w-1.5 rounded-full bg-ok" />
      )}
      {value}
    </span>
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

const PERIODES = [
  { id: "JOUR", label: "1 jour", jours: 1 },
  { id: "SEMAINE", label: "1 semaine", jours: 7 },
  { id: "MOIS", label: "1 mois", jours: 30 },
  { id: "ANNEE", label: "1 année", jours: 365 },
] as const;
type PeriodeId = (typeof PERIODES)[number]["id"];

export default function ConsoleDashboard() {
  const { profil, principal, refresh } = useAuth();
  const [types, setTypes] = useState<Charge<BusinessType[]>>({ state: "loading", data: [] });
  const [businesses, setBusinesses] = useState<Charge<Business[]>>({ state: "loading", data: [] });
  const [usage, setUsage] = useState<Charge<Dashboard | null>>({ state: "loading", data: null });
  const [upgradeOuvert, setUpgradeOuvert] = useState(false);
  const [periode, setPeriode] = useState<PeriodeId>("MOIS");
  const [sparkline, setSparkline] = useState<Charge<UsagePoint[]>>({ state: "loading", data: [] });

  function chargerDashboard() {
    getDashboard()
      .then((data) => setUsage({ state: "ok", data }))
      .catch(() => setUsage({ state: "error", data: null }));
  }

  useEffect(() => {
    listBusinessTypes()
      .then((data) => setTypes({ state: "ok", data }))
      .catch(() => setTypes({ state: "error", data: [] }));
    listBusinesses()
      .then((data) => setBusinesses({ state: "ok", data }))
      .catch(() => setBusinesses({ state: "error", data: [] }));
    chargerDashboard();
  }, []);

  useEffect(() => {
    const jours = PERIODES.find((p) => p.id === periode)?.jours ?? 30;
    setSparkline((s) => ({ ...s, state: "loading" }));
    getSparkline(jours)
      .then((data) => setSparkline({ state: "ok", data }))
      .catch(() => setSparkline({ state: "error", data: [] }));
  }, [periode]);

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
    {
      label: "Première requête effectuée",
      done: (dash?.requetesCeMois ?? 0) > 0,
    },
  ];
  const onboardingPct = Math.round(
    (onboardingSteps.filter((s) => s.done).length / onboardingSteps.length) * 100
  );

  return (
    <div className="animate-fade-up">
      <div className="flex flex-col justify-between gap-4 border-b border-line pb-6 sm:flex-row sm:items-end">
        <div>
          <div className="text-[12px] font-semibold uppercase tracking-wider text-brand">Console</div>
          <h1 className="mt-2 font-display text-3xl font-bold text-ink">Vue d&apos;ensemble</h1>
          <p className="mt-1 text-sm text-muted">
            Connecté en tant que <span className="font-medium text-ink">{principal}</span>
            {profil?.owner && <span className="text-brand"> · Owner</span>}
          </p>
        </div>
        <ButtonLink href="/console/api-key" size="sm">
          <IconKey className="h-4 w-4" /> Mes clés d&apos;API
        </ButtonLink>
      </div>

      {/* ── Consommation d'API — la barre phare, toujours présente ── */}
      <section className="mt-8 grid gap-5 lg:grid-cols-[1.6fr_1fr]">
        <div className="flex flex-col border border-line bg-white p-6 card-hover">
          {/* En-tête compact : titre + lien à gauche/droite, sélecteur de période centré en dessous */}
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-baseline gap-2.5">
              <span className="flex items-center gap-1.5 text-[13px] text-muted">
                <IconActivity className="h-3.5 w-3.5 text-brand" /> Consommation d&apos;API
              </span>
              {usage.state === "ok" && usage.data && (
                <span className="font-display text-lg font-bold text-ink">
                  {usage.data.requetesCeMois.toLocaleString("fr-FR")}
                  <span className="ml-1 text-[11px] font-normal text-muted">ce mois</span>
                </span>
              )}
            </div>
            <Link href="/console/pricing" className="text-[11px] font-semibold text-brand hover:underline">
              Détails complets →
            </Link>
          </div>

          {usage.state === "loading" && <LoadingBlock />}
          {usage.state === "error" && (
            <div className="mt-4 text-sm text-danger">Impossible de charger la consommation.</div>
          )}
          {usage.state === "ok" && usage.data && (
            <>
              <div className="mt-3 flex justify-center">
                <div className="inline-flex gap-1 rounded-lg border border-line bg-subtle p-1">
                  {PERIODES.map((p) => (
                    <button
                      key={p.id}
                      onClick={() => setPeriode(p.id)}
                      className={cn(
                        "rounded-md px-3.5 py-1.5 text-[12px] font-semibold transition-all duration-200",
                        periode === p.id
                          ? "bg-ok text-white shadow-card"
                          : "text-body hover:text-ink"
                      )}
                    >
                      {p.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* La courbe reçoit la majorité de l'espace vertical de la carte. */}
              <div className="mt-5 flex-1">
                {sparkline.state === "loading" && <LoadingBlock />}
                {sparkline.state === "error" && (
                  <div className="text-sm text-danger">Impossible de charger la courbe.</div>
                )}
                {sparkline.state === "ok" && <LineChart points={sparkline.data} height={280} />}
              </div>
              <div className="mt-1 text-center text-[11px] font-semibold uppercase tracking-wider text-muted">
                {periode === "JOUR"
                  ? "Aujourd'hui (grain journalier)"
                  : periode === "SEMAINE"
                    ? "7 derniers jours"
                    : periode === "MOIS"
                      ? "30 derniers jours"
                      : "365 derniers jours"}
              </div>

              <div className="mt-6 grid grid-cols-3 gap-4 border-t border-line pt-5">
                <div>
                  <div className="text-xs uppercase tracking-wider text-muted">Aujourd&apos;hui</div>
                  <div className="mt-1 font-display text-xl font-bold text-ink">
                    {usage.data.requetesAujourdhui.toLocaleString("fr-FR")}
                  </div>
                </div>
                <div>
                  <div className="text-xs uppercase tracking-wider text-muted">Taux d&apos;erreur</div>
                  <div className="mt-1 font-display text-xl font-bold text-ink">
                    {(usage.data.tauxErreur * 100).toFixed(1)}
                    <span className="text-sm text-muted">%</span>
                  </div>
                </div>
                <div>
                  <div className="text-xs uppercase tracking-wider text-muted">Erreurs (jour)</div>
                  <div className="mt-1 font-display text-xl font-bold text-ink">
                    {usage.data.erreursAujourdhui}
                  </div>
                </div>
              </div>

              {usage.data.bloque && (
                <p className="mt-5 border-l-2 border-danger bg-danger/5 px-4 py-3 text-sm text-danger">
                  Quota mensuel atteint : vos clés d&apos;API sont bloquées (HTTP 402).{" "}
                  <button onClick={() => setUpgradeOuvert(true)} className="font-semibold underline underline-offset-2">
                    Passez à un plan supérieur
                  </button>
                  .
                </p>
              )}
            </>
          )}
        </div>

        {/* Plan actuel + checklist de démarrage */}
        <div className="flex flex-col gap-5">
          <div className="border border-line bg-white p-6 card-hover">
            <div className="flex items-center gap-2 text-sm text-muted">
              <IconCrown className="h-4 w-4 text-brand" /> Plan actuel
            </div>
            {usage.state === "ok" && usage.data && (
              <>
                <div className="mt-2 font-display text-2xl font-bold text-ink">{usage.data.plan}</div>
                <div className="mt-1 text-sm text-muted">
                  {usage.data.quotaMensuel < 0
                    ? "Requêtes illimitées"
                    : `${usage.data.requetesRestantes.toLocaleString("fr-FR")} / ${usage.data.quotaMensuel.toLocaleString("fr-FR")} restantes ce mois`}
                </div>
                {usage.data.quotaMensuel > 0 && (
                  <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-subtle">
                    <div
                      className={cn("h-full rounded-full", usage.data.bloque ? "bg-danger" : "bg-ok")}
                      style={{ width: `${Math.min(100, (usage.data.requetesCeMois / usage.data.quotaMensuel) * 100)}%` }}
                    />
                  </div>
                )}
                <Button size="sm" className="mt-5 w-full" onClick={() => setUpgradeOuvert(true)}>
                  Gérer / Upgrade mon plan
                </Button>
              </>
            )}
          </div>

          <div className="flex-1 border border-line bg-white p-6 card-hover">
            <div className="flex items-center justify-between">
              <span className="text-sm text-muted">Démarrage</span>
              <span className="font-mono text-sm font-semibold text-brand">{onboardingPct}%</span>
            </div>
            <div className="mt-4">
              <OnboardingSteps steps={onboardingSteps} />
            </div>
          </div>
        </div>
      </section>

      {upgradeOuvert && (
        <UpgradeModal
          planActuel={dash?.plan ?? null}
          onClose={() => setUpgradeOuvert(false)}
          onChanged={() => {
            // Ne ferme PAS ici : la modal gère elle-même sa fermeture animée après confirmation
            // (onClose est appelé par la modal une fois l'animation de sortie terminée).
            chargerDashboard();
            void refresh(); // resynchronise profil.plan (auth-context) — c'est lui qui alimente le badge de la navbar
          }}
        />
      )}

      {/* Identité */}
      <div className="mt-8 grid gap-px border border-line bg-line sm:grid-cols-3">
        <div className="bg-white p-5">
          <div className="text-xs uppercase tracking-wider text-muted">Developer ID</div>
          <div className="mt-1.5">{profil?.developerId ? <CopyableId value={profil.developerId} /> : "—"}</div>
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

      {/* Entreprises / Clés API / Top opérations / Top entreprises */}
      {dash && (
        <div className="mt-10 grid gap-8 xl:grid-cols-2">
          <section>
            <div className="flex items-baseline justify-between">
              <h2 className="font-display text-lg font-semibold text-ink">Entreprises</h2>
              <ButtonLink href="/console/businesses" variant="secondary" size="sm">
                {dash.nombreEntreprises} au total →
              </ButtonLink>
            </div>
            <div className="mt-3 border border-line bg-white">
              {businesses.state === "loading" && <LoadingBlock lines={2} />}
              {businesses.state === "ok" && businesses.data.length === 0 && (
                <div className="p-5 text-sm text-muted">
                  Aucune entreprise.{" "}
                  <Link href="/console/businesses" className="text-brand underline-offset-2 hover:underline">
                    Créer une entreprise
                  </Link>
                </div>
              )}
              {businesses.state === "ok" &&
                businesses.data.slice(0, 5).map((b, i) => (
                  <div key={b.id} className={cn("flex items-center gap-4 px-5 py-3.5", i !== 0 && "border-t border-line")}>
                    <span className="truncate text-sm font-medium text-ink">{b.nom}</span>
                    <span className="font-mono text-[12px] text-muted">v{b.versionNumber}</span>
                    <span className="ml-auto">
                      <StatutBadge value={b.cycleVie} />
                    </span>
                  </div>
                ))}
            </div>
          </section>

          <section>
            <div className="flex items-baseline justify-between">
              <h2 className="font-display text-lg font-semibold text-ink">Clés API actives</h2>
              <ButtonLink href="/console/api-key" variant="secondary" size="sm">
                {dash.nombreClesActives} au total →
              </ButtonLink>
            </div>
            <div className="mt-3 grid h-[calc(100%-2.5rem)] place-items-center border border-line bg-white p-5 card-hover">
              <div className="text-center">
                <div className="font-display text-4xl font-bold text-ink">{dash.nombreClesActives}</div>
                <p className="mt-1 text-sm text-muted">clé{dash.nombreClesActives > 1 ? "s" : ""} active{dash.nombreClesActives > 1 ? "s" : ""}</p>
                <ButtonLink href="/console/api-key" variant="secondary" size="sm" className="mt-4">
                  Gérer les clés
                </ButtonLink>
              </div>
            </div>
          </section>

          {dash.topOperations.length > 0 && (
            <section>
              <h2 className="font-display text-lg font-semibold text-ink">Top opérations (30 j)</h2>
              <div className="mt-3 border border-line bg-white p-5 card-hover">
                <BarChart items={dash.topOperations.map((o) => ({ nom: o.nom, total: o.total }))} labelKey="nom" />
              </div>
            </section>
          )}
          {dash.topEntreprises.length > 0 && (
            <section>
              <h2 className="font-display text-lg font-semibold text-ink">Requêtes par entreprise / clé (30 j)</h2>
              <div className="mt-3 border border-line bg-white p-5 card-hover">
                <DonutChart
                  slices={dash.topEntreprises.map((e) => ({ label: e.nom, value: e.total }))}
                />
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

      {/* Types métier */}
      <section className="mt-10">
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
              <div key={t.id} className={cn("flex items-center gap-4 px-5 py-3.5", i !== 0 && "border-t border-line")}>
                <code className="font-mono text-[13px] font-medium text-ink">{t.code}</code>
                <span className="truncate text-sm text-muted">{t.nom}</span>
                <span className="ml-auto">
                  <StatutBadge value={t.statut} />
                </span>
              </div>
            ))}
        </div>
      </section>

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
