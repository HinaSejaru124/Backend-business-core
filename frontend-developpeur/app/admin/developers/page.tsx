"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { adminDevelopers, type AdminDeveloperRow } from "@/lib/api";
import { IconArrowRight } from "@/components/icons";
import { PlanBadge, StatutBadge, ConsoBar } from "@/components/admin-ui";
import { cn } from "@/lib/cn";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T };

function fmt(n: number): string {
  return n.toLocaleString("fr-FR");
}

export default function AdminDevelopersPage() {
  const [charge, setCharge] = useState<Charge<AdminDeveloperRow[]>>({ state: "loading", data: [] });

  useEffect(() => {
    adminDevelopers()
      .then((data) => setCharge({ state: "ok", data }))
      .catch(() => setCharge({ state: "error", data: [] }));
  }, []);

  return (
    <div className="animate-fade-up">
      <div className="border-b border-line pb-6">
        <div className="text-[12px] font-semibold uppercase tracking-wider text-brand">Clients</div>
        <h1 className="mt-2 font-display text-3xl font-bold text-ink">Développeurs</h1>
        <p className="mt-1 text-sm text-muted">
          Les développeurs qui utilisent Business Core — plan, statut et consommation en temps réel.
        </p>
      </div>

      <div className="mt-8">
        {charge.state === "loading" && <p className="text-sm text-muted">Chargement…</p>}
        {charge.state === "error" && (
          <p className="rounded-lg border border-danger/25 bg-danger/5 px-4 py-3 text-sm text-danger">
            Impossible de charger la liste des développeurs.
          </p>
        )}
        {charge.state === "ok" && (
          <div className="overflow-x-auto rounded-xl border border-line bg-white shadow-card">
            <table className="w-full min-w-[820px] border-collapse text-sm">
              <thead>
                <tr className="border-b border-line bg-subtle text-left">
                  {["Développeur", "Plan", "Statut", "Entreprises", "Clés actives", "Consommation (mois)", ""].map((h) => (
                    <th key={h} className="px-5 py-3.5 text-[11px] font-semibold uppercase tracking-wider text-muted">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {charge.data.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-5 py-8 text-center text-sm text-muted">
                      Aucun développeur enregistré.
                    </td>
                  </tr>
                )}
                {charge.data.map((d, i) => (
                  <tr
                    key={d.id}
                    className={cn("transition-colors hover:bg-subtle", i !== 0 && "border-t border-line")}
                  >
                    <td className="px-5 py-4">
                      <Link href={`/admin/developers/${d.id}`} className="font-medium text-ink hover:text-brand">
                        {d.email}
                      </Link>
                      <div className="mt-0.5 font-mono text-[11px] text-muted">
                        depuis {new Date(d.createdAt).toLocaleDateString("fr-FR")}
                      </div>
                    </td>
                    <td className="px-5 py-4"><PlanBadge plan={d.plan} /></td>
                    <td className="px-5 py-4"><StatutBadge status={d.status} /></td>
                    <td className="px-5 py-4 font-mono text-[13px] text-ink">{fmt(d.nbEntreprises)}</td>
                    <td className="px-5 py-4 font-mono text-[13px] text-ink">{fmt(d.nbClesActives)}</td>
                    <td className="px-5 py-4"><ConsoBar pct={d.pctConso} illimite={d.illimite} /></td>
                    <td className="px-5 py-4 text-right">
                      <Link
                        href={`/admin/developers/${d.id}`}
                        className="inline-flex items-center gap-1.5 rounded-lg bg-ok px-3.5 py-2 text-[12.5px] font-semibold text-white shadow-sm transition-all hover:-translate-y-0.5 hover:bg-ok-strong"
                      >
                        Gérer dev <IconArrowRight className="h-3.5 w-3.5" />
                      </Link>
                    </td>
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
