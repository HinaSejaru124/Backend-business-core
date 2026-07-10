"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getDashboard } from "@/lib/api";
import type { Dashboard } from "@/lib/types";
import { IconAlertTriangle, IconArrowRight, IconCart, IconHistory, IconPill } from "@/components/icons";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T | null };

function formatXAF(n: number) {
  return new Intl.NumberFormat("fr-FR").format(n) + " XAF";
}

export default function DashboardPage() {
  const [charge, setCharge] = useState<Charge<Dashboard>>({ state: "loading", data: null });

  useEffect(() => {
    getDashboard()
      .then((data) => setCharge({ state: "ok", data }))
      .catch(() => setCharge({ state: "error", data: null }));
  }, []);

  return (
    <div className="animate-fade-up">
      {/* Bandeau vert plein — casse le blanc dès l'arrivée sur la page */}
      <div className="border-l-4 border-brand bg-ink px-8 py-8">
        <div className="font-mono text-[12px] uppercase tracking-wider text-brand">Tableau de bord</div>
        <h1 className="mt-2 font-display text-4xl font-bold text-white">Pharmacie du Centre</h1>
        <p className="mt-2 text-[15px] text-white/70">Vue d&apos;ensemble de l&apos;activité du jour.</p>
      </div>

      {charge.state === "loading" && (
        <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="h-36 animate-pulse border border-line bg-white" />
          ))}
        </div>
      )}

      {charge.state === "error" && (
        <p className="mt-8 border-l-4 border-danger bg-danger/5 px-5 py-4 text-[15px] text-danger">
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
              tinted
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
            <p className="mt-6 border-l-4 border-brand bg-brand-tint px-5 py-4 text-[15px] text-ink">
              Aucune vente aujourd&apos;hui. L&apos;écran{" "}
              <Link href="/vente" className="font-semibold underline underline-offset-2">
                Vente
              </Link>{" "}
              explique pourquoi l&apos;encaissement n&apos;est pas encore disponible.
            </p>
          )}

          <h2 className="mt-12 font-display text-2xl font-bold text-ink">Accès rapides</h2>
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
  tinted,
  href,
}: {
  icon: typeof IconCart;
  label: string;
  value: string;
  warn?: boolean;
  tinted?: boolean;
  href?: string;
}) {
  const content = (
    <div
      className={`h-full border-2 p-6 transition-all hover:-translate-y-0.5 hover:shadow-card ${
        warn ? "border-warning bg-warning/5" : tinted ? "border-brand bg-brand-tint" : "border-brand bg-white"
      }`}
    >
      <span
        className={`grid h-12 w-12 place-items-center ${warn ? "bg-warning" : "bg-brand"}`}
      >
        <Icon className="h-6 w-6 text-white" />
      </span>
      <div className="mt-4 font-display text-3xl font-bold text-ink">{value}</div>
      <div className="mt-1.5 text-[14px] text-muted">{label}</div>
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
      className="group flex flex-col justify-between border border-line bg-white p-6 transition-all hover:-translate-y-0.5 hover:border-brand hover:bg-brand-tint hover:shadow-card"
    >
      <div className="flex items-center justify-between">
        <span className="grid h-11 w-11 place-items-center bg-brand-tint text-brand transition-colors group-hover:bg-brand group-hover:text-white">
          <Icon className="h-5 w-5" />
        </span>
        <IconArrowRight className="h-4 w-4 text-muted transition-transform group-hover:translate-x-1 group-hover:text-brand" />
      </div>
      <div className="mt-4 font-display text-[17px] font-semibold text-ink">{title}</div>
      <p className="mt-1 text-[14px] text-muted">{desc}</p>
    </Link>
  );
}
