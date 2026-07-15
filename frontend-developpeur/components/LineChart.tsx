"use client";

import { useId, useMemo, useState } from "react";

export type ChartPoint = { jour: string; total: number };

/** Calcule un pas d'échelle "rond" (1/2/5 × 10^n) pour ~4-5 graduations, à la manière d'un vrai graphe. */
function pasGradue(max: number): number {
  if (max <= 0) return 1;
  const cible = max / 4;
  const magnitude = Math.pow(10, Math.floor(Math.log10(cible)));
  const normalise = cible / magnitude;
  const pas = normalise < 1.5 ? 1 : normalise < 3.5 ? 2 : normalise < 7.5 ? 5 : 10;
  return pas * magnitude;
}

function fmtGraduation(n: number): string {
  if (n >= 1000) return `${(n / 1000).toLocaleString("fr-FR", { maximumFractionDigits: 1 })}k`;
  return n.toLocaleString("fr-FR");
}

/**
 * Courbe lissée (aire + ligne) sur des points réels — aucune donnée simulée : `points` vient de
 * GET /v1/dashboard/sparkline (compteurs réels par clé API, agrégés jour par jour). Graduation Y
 * "ronde" façon graphe pro. Survol = point rond fixe (HTML, pas SVG) qui suit la courbe sans jamais
 * se déformer, avec tooltip.
 */
export default function LineChart({ points, height = 220 }: { points: ChartPoint[]; height?: number }) {
  const gradientId = useId();
  const [hover, setHover] = useState<number | null>(null);

  const maxReel = Math.max(1, ...points.map((p) => p.total));
  const pas = pasGradue(maxReel);
  const plafond = Math.max(pas, Math.ceil(maxReel / pas) * pas);
  const graduations = useMemo(() => {
    const g: number[] = [];
    for (let v = 0; v <= plafond; v += pas) g.push(v);
    return g.reverse();
  }, [plafond, pas]);

  if (points.length === 0) {
    return <div className="grid h-[220px] place-items-center text-sm text-muted">Aucune donnée.</div>;
  }

  const width = 100;
  const stepX = points.length > 1 ? width / (points.length - 1) : 0;

  const coords = points.map((p, i) => ({
    x: points.length > 1 ? i * stepX : width / 2,
    y: 100 - (p.total / plafond) * 96,
    p,
  }));

  function smoothPath(): string {
    if (coords.length === 1) return `M ${coords[0].x} ${coords[0].y}`;
    let d = `M ${coords[0].x} ${coords[0].y}`;
    for (let i = 0; i < coords.length - 1; i++) {
      const c = coords[i];
      const n = coords[i + 1];
      const mx = (c.x + n.x) / 2;
      d += ` Q ${c.x} ${c.y} ${mx} ${(c.y + n.y) / 2}`;
    }
    const last = coords[coords.length - 1];
    d += ` T ${last.x} ${last.y}`;
    return d;
  }

  const linePath = smoothPath();
  const areaPath = `${linePath} L ${coords[coords.length - 1].x} 100 L ${coords[0].x} 100 Z`;
  const active = hover !== null ? coords[hover] : null;

  return (
    <div className="flex gap-3" style={{ height }}>
      {/* Axe Y gradué */}
      <div className="flex flex-none flex-col justify-between py-1 text-right font-mono text-[10px] text-muted">
        {graduations.map((g) => (
          <div key={g}>{fmtGraduation(g)}</div>
        ))}
      </div>

      <div className="relative min-w-0 flex-1">
        <svg
          viewBox="0 0 100 100"
          preserveAspectRatio="none"
          className="h-full w-full overflow-visible"
          onMouseLeave={() => setHover(null)}
        >
          <defs>
            <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#0E9F6E" stopOpacity="0.22" />
              <stop offset="100%" stopColor="#0E9F6E" stopOpacity="0" />
            </linearGradient>
            <filter id={`${gradientId}-glow`} x="-20%" y="-50%" width="140%" height="220%">
              <feDropShadow dx="0" dy="0.6" stdDeviation="0.9" floodColor="#0E9F6E" floodOpacity="0.35" />
            </filter>
          </defs>

          {/* Lignes de graduation horizontales — discrètes, pointillées */}
          {graduations.map((g) => {
            const y = 100 - (g / plafond) * 96;
            return (
              <line
                key={g}
                x1={0}
                y1={y}
                x2={100}
                y2={y}
                stroke="#E2E7F0"
                strokeWidth={0.3}
                strokeDasharray="1.2 1.6"
                vectorEffect="non-scaling-stroke"
              />
            );
          })}

          <path d={areaPath} fill={`url(#${gradientId})`} stroke="none" />
          <path
            d={linePath}
            fill="none"
            stroke="#0E9F6E"
            strokeWidth={1.15}
            vectorEffect="non-scaling-stroke"
            strokeLinecap="round"
            strokeLinejoin="round"
            filter={`url(#${gradientId}-glow)`}
          />
          {coords.map((c, i) => (
            <rect
              key={c.p.jour}
              x={c.x - stepX / 2}
              y={0}
              width={Math.max(stepX, 100 / points.length)}
              height={100}
              fill="transparent"
              onMouseEnter={() => setHover(i)}
            />
          ))}
        </svg>

        {/* Curseur : un vrai rond HTML fixe (positionné en %), jamais déformé par le viewBox SVG */}
        {active && (
          <div
            className="pointer-events-none absolute h-2.5 w-2.5 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-white bg-ok shadow-[0_0_0_1px_rgba(14,159,110,0.4)]"
            style={{ left: `${active.x}%`, top: `${active.y}%` }}
          />
        )}
        {active && (
          <div
            className="pointer-events-none absolute -translate-x-1/2 -translate-y-full rounded border border-line bg-white px-2.5 py-1.5 text-xs shadow-pop"
            style={{ left: `${active.x}%`, top: `${Math.max(0, active.y - 6)}%` }}
          >
            <div className="font-semibold text-ink">{active.p.total.toLocaleString("fr-FR")} requêtes</div>
            <div className="text-muted">{active.p.jour}</div>
          </div>
        )}
      </div>
    </div>
  );
}
