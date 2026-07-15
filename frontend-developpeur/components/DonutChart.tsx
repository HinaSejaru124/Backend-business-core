"use client";

import { useId, useState } from "react";

export type DonutSlice = { label: string; value: number };

/**
 * Anneau (donut) + légende — bâti sur des données RÉELLES (aucune valeur simulée).
 * Chaque part est un arc proportionnel ; la légende donne le libellé, le compte réel et le %.
 * Utilisé pour « requêtes par entreprise / clé API » (une clé active par entreprise).
 */
const PALETTE = ["#1B4DF5", "#0E9F6E", "#7C3AED", "#F59E0B", "#EC4899", "#0EA5E9", "#64748B"];

export default function DonutChart({ slices, unit = "requêtes" }: { slices: DonutSlice[]; unit?: string }) {
  const gid = useId();
  const [actif, setActif] = useState<number | null>(null);

  const total = slices.reduce((s, x) => s + x.value, 0);

  if (total === 0) {
    return (
      <div className="grid h-[180px] place-items-center text-center text-sm text-muted">
        Aucune requête sur la période.
      </div>
    );
  }

  const R = 42;
  const C = 2 * Math.PI * R;
  let offset = 0;

  const arcs = slices.map((s, i) => {
    const frac = s.value / total;
    const arc = {
      couleur: PALETTE[i % PALETTE.length],
      dash: frac * C,
      gap: C - frac * C,
      decalage: offset,
      pct: Math.round(frac * 100),
      ...s,
    };
    offset += frac * C;
    return arc;
  });

  const misEnAvant = actif !== null ? arcs[actif] : null;

  return (
    <div className="flex flex-col items-center gap-6 sm:flex-row">
      <div className="relative flex-none">
        <svg viewBox="0 0 100 100" width="150" height="150" className="-rotate-90">
          <circle cx="50" cy="50" r={R} fill="none" stroke="#EDF1F8" strokeWidth="12" />
          {arcs.map((a, i) => (
            <circle
              key={a.label}
              cx="50"
              cy="50"
              r={R}
              fill="none"
              stroke={a.couleur}
              strokeWidth={actif === i ? 15 : 12}
              strokeDasharray={`${a.dash} ${a.gap}`}
              strokeDashoffset={-a.decalage}
              className="cursor-pointer transition-[stroke-width] duration-200"
              onMouseEnter={() => setActif(i)}
              onMouseLeave={() => setActif(null)}
            />
          ))}
        </svg>
        <div className="pointer-events-none absolute inset-0 grid place-items-center text-center">
          <div>
            <div className="font-display text-2xl font-bold text-ink">
              {(misEnAvant ? misEnAvant.value : total).toLocaleString("fr-FR")}
            </div>
            <div className="text-[11px] text-muted">
              {misEnAvant ? `${misEnAvant.pct}%` : unit}
            </div>
          </div>
        </div>
      </div>

      <ul className="min-w-0 flex-1 space-y-2" aria-label={`Légende — ${gid}`}>
        {arcs.map((a, i) => (
          <li
            key={a.label}
            className="flex items-center gap-2.5 text-sm"
            onMouseEnter={() => setActif(i)}
            onMouseLeave={() => setActif(null)}
          >
            <span className="h-2.5 w-2.5 flex-none rounded-full" style={{ background: a.couleur }} />
            <span className="min-w-0 flex-1 truncate text-body">{a.label}</span>
            <span className="flex-none font-semibold text-ink">{a.value.toLocaleString("fr-FR")}</span>
            <span className="w-10 flex-none text-right text-xs text-muted">{a.pct}%</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
