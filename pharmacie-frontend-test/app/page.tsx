"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getDashboard } from "@/lib/api";
import { useSession } from "@/lib/useSession";
import { LIBELLE_ROLE } from "@/lib/roles";
import type { Dashboard } from "@/lib/types";
import { IconAlertTriangle, IconArrowRight, IconCart, IconHistory, IconPill } from "@/components/icons";
import { cn } from "@/lib/cn";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T | null };

function formatXAF(n: number) {
  return new Intl.NumberFormat("fr-FR").format(n) + " XAF";
}

export default function DashboardPage() {
  const { session } = useSession();
  const [charge, setCharge] = useState<Charge<Dashboard>>({ state: "loading", data: null });

  useEffect(() => {
    getDashboard()
      .then((data) => setCharge({ state: "ok", data }))
      .catch(() => setCharge({ state: "error", data: null }));
  }, []);

  const role = session.state === "ok" ? session.data?.role ?? null : null;
  const prenom = (session.state === "ok" ? session.data?.nomAffichage : null)?.split(" ")[0];
  const salutation = role ? LIBELLE_ROLE[role] : "";

  return (
    <div className="animate-fade-up">
      <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-end">
        <div>
          <h1 className="font-display text-3xl font-bold tracking-tight text-ink">
            Bonjour, {prenom || salutation} <span aria-hidden>👋</span>
          </h1>
          <p className="mt-1.5 text-[15px] text-muted">Voici un aperçu de l&apos;activité de votre pharmacie aujourd&apos;hui.</p>
        </div>
      </div>

      {charge.state === "loading" && (
        <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="h-32 animate-pulse rounded-2xl border border-line bg-white" />
          ))}
        </div>
      )}

      {charge.state === "error" && (
        <p className="mt-8 rounded-xl border border-danger/25 bg-danger/5 px-5 py-4 text-[15px] text-danger">
          Impossible de charger le tableau de bord — le backend Pharmacie (:9090) est-il démarré ?
        </p>
      )}

      {charge.state === "ok" && charge.data && (
        <>
          <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard
              icon={IconCart}
              label="Chiffre d'affaires du jour"
              value={formatXAF(charge.data.chiffreAffairesDuJour)}
            />
            <StatCard icon={IconHistory} label="Ventes du jour" value={String(charge.data.nombreVentesDuJour)} />
            <StatCard
              icon={IconPill}
              label="Médicaments au catalogue"
              value={String(charge.data.totalMedicaments)}
            />
            <StatCard
              icon={IconAlertTriangle}
              label="Alertes stock actives"
              value={String(charge.data.alertesStockActives)}
              warn={charge.data.alertesStockActives > 0}
              href="/alertes"
            />
          </div>

          {charge.data.nombreVentesDuJour === 0 && (
            <div className="mt-6 flex flex-col gap-3 rounded-2xl border border-brand/15 bg-brand-tint px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-[14.5px] text-ink">
                Aucune vente aujourd&apos;hui. L&apos;écran <strong>Vente</strong> explique pourquoi
                l&apos;encaissement n&apos;est pas encore disponible.
              </p>
              <Link
                href="/vente"
                className="inline-flex flex-none items-center gap-1.5 rounded-xl bg-white px-4 py-2 text-[13.5px] font-semibold text-brand shadow-card transition-all hover:-translate-y-0.5"
              >
                Aller au poste de vente <IconArrowRight className="h-3.5 w-3.5" />
              </Link>
            </div>
          )}

          <h2 className="mt-12 font-display text-xl font-bold text-ink">Accès rapides</h2>
          <div className="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            <QuickLink href="/medicaments" icon={IconPill} title="Médicaments" desc="Catalogue et stock" />
            <QuickLink href="/ordonnances" icon={IconHistory} title="Ordonnances" desc="Prescriptions clients" />
            <QuickLink href="/fournisseurs" icon={IconCart} title="Fournisseurs" desc="Commandes et réception" />
            <QuickLink href="/alertes" icon={IconAlertTriangle} title="Alertes stock" desc="Médicaments sous seuil" />
          </div>
        </>
      )}
    </div>
  );
}

function StatCard({
  icon: Icon,
  label,
  value,
  warn,
  href,
}: {
  icon: typeof IconCart;
  label: string;
  value: string;
  warn?: boolean;
  href?: string;
}) {
  const content = (
    <div
      className={cn(
        "h-full rounded-2xl border bg-white p-6 shadow-card transition-all duration-200 hover:-translate-y-1 hover:shadow-card-hover",
        warn ? "border-warning/25" : "border-line"
      )}
    >
      <span
        className={cn(
          "grid h-11 w-11 place-items-center rounded-xl",
          warn ? "bg-warning/15 text-warning" : "bg-brand-tint text-brand"
        )}
      >
        <Icon className="h-5 w-5" />
      </span>
      <div className="mt-4 font-display text-[26px] font-bold leading-none text-ink">{value}</div>
      <div className="mt-2 text-[13.5px] text-muted">{label}</div>
    </div>
  );
  return href ? <Link href={href}>{content}</Link> : content;
}

function QuickLink({
  href,
  icon: Icon,
  title,
  desc,
}: {
  href: string;
  icon: typeof IconCart;
  title: string;
  desc: string;
}) {
  return (
    <Link
      href={href}
      className="group flex flex-col justify-between rounded-2xl border border-line bg-white p-6 shadow-card transition-all duration-200 hover:-translate-y-1 hover:border-brand/30 hover:shadow-card-hover"
    >
      <div className="flex items-center justify-between">
        <span className="grid h-11 w-11 place-items-center rounded-xl bg-brand-tint text-brand transition-colors group-hover:bg-brand group-hover:text-white">
          <Icon className="h-5 w-5" />
        </span>
        <IconArrowRight className="h-4 w-4 text-muted transition-transform group-hover:translate-x-1 group-hover:text-brand" />
      </div>
      <div className="mt-4 font-display text-[17px] font-semibold text-ink">{title}</div>
      <p className="mt-1 text-[14px] text-muted">{desc}</p>
    </Link>
  );
}
