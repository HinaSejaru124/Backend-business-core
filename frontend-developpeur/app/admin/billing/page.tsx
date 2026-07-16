"use client";

import { useEffect, useState } from "react";
import { adminBilling, type AdminBillingSummary } from "@/lib/api";
import { IconWallet } from "@/components/icons";
import { cn } from "@/lib/cn";
import PricingEditor from "./pricing-editor";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T | null };

function fmt(n: number): string {
  return n.toLocaleString("fr-FR");
}

export default function AdminBillingPage() {
  const [charge, setCharge] = useState<Charge<AdminBillingSummary>>({ state: "loading", data: null });

  useEffect(() => {
    adminBilling()
      .then((data) => setCharge({ state: "ok", data }))
      .catch(() => setCharge({ state: "error", data: null }));
  }, []);

  const d = charge.state === "ok" ? charge.data : null;

  return (
    <div className="animate-fade-up">
      <div className="border-b border-line pb-6">
        <div className="text-[12px] font-semibold uppercase tracking-wider text-brand">Comptabilité</div>
        <h1 className="mt-2 font-display text-3xl font-bold text-ink">Facturation</h1>
        <p className="mt-1 text-sm text-muted">
          Revenus de la plateforme par plan tarifaire.
        </p>
      </div>

      {charge.state === "loading" && <p className="mt-8 text-sm text-muted">Chargement…</p>}
      {charge.state === "error" && (
        <p className="mt-8 rounded-lg border border-danger/25 bg-danger/5 px-4 py-3 text-sm text-danger">
          Impossible de charger la facturation.
        </p>
      )}

      {d && (
        <>
          {/* Deux totaux : facturé théorique vs encaissé réel */}
          <div className="mt-8 grid gap-5 sm:grid-cols-2">
            <div className="rounded-xl border border-line bg-white p-6 shadow-card">
              <div className="flex items-center gap-2 text-sm text-muted">
                <IconWallet className="h-4 w-4 text-brand" /> Chiffre d&apos;affaires théorique / mois
              </div>
              <div className="mt-3 font-display text-3xl font-bold text-ink">
                {fmt(d.caTheoriqueMensuelTotal)} <span className="text-lg font-normal text-muted">{d.devise}</span>
              </div>
              <p className="mt-2 text-xs text-muted">
                Si chaque développeur payait le prix de son plan actuel.
              </p>
            </div>
            <div className="rounded-xl border border-line bg-white p-6 shadow-card">
              <div className="text-sm text-muted">Encaissé réel</div>
              <div className="mt-3 font-display text-3xl font-bold text-ink">
                {fmt(d.encaisseReel)} <span className="text-lg font-normal text-muted">{d.devise}</span>
              </div>
              <p className="mt-2 text-xs text-muted">
                Le paiement réel passe par Kernel Core (indisponible) — aucun montant n&apos;est encore
                encaissé. Ce chiffre deviendra exact quand les paiements seront actifs.
              </p>
            </div>
          </div>

          {/* Détail par plan */}
          <div className="mt-6 overflow-x-auto rounded-xl border border-line bg-white shadow-card">
            <table className="w-full min-w-[720px] border-collapse text-sm">
              <thead>
                <tr className="border-b border-line bg-subtle text-left">
                  {["Plan", "Prix / mois", "Quota mensuel", "Abonnés", "CA théorique / mois"].map((h) => (
                    <th key={h} className="px-5 py-3.5 text-[11px] font-semibold uppercase tracking-wider text-muted">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {d.plans.map((p, i) => (
                  <tr key={p.code} className={cn(i !== 0 && "border-t border-line")}>
                    <td className="px-5 py-4">
                      <span className="font-display text-[15px] font-bold text-ink">{p.code}</span>
                    </td>
                    <td className="px-5 py-4 font-mono text-[13px] text-ink">
                      {p.prixMensuel > 0 ? `${fmt(p.prixMensuel)} ${p.devise}` : "Gratuit"}
                    </td>
                    <td className="px-5 py-4 font-mono text-[13px] text-muted">
                      {p.illimite ? "Illimité" : `${fmt(p.quotaMensuel)} req`}
                    </td>
                    <td className="px-5 py-4 font-mono text-[13px] text-ink">{fmt(p.nbAbonnes)}</td>
                    <td className="px-5 py-4 font-mono text-[13px] font-semibold text-ink">
                      {fmt(p.caTheoriqueMensuel)} {p.devise}
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="border-t-2 border-line bg-subtle">
                  <td className="px-5 py-4 font-semibold text-ink" colSpan={4}>Total facturé théorique / mois</td>
                  <td className="px-5 py-4 font-mono text-[14px] font-bold text-brand">
                    {fmt(d.caTheoriqueMensuelTotal)} {d.devise}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </>
      )}

      <PricingEditor />
    </div>
  );
}
