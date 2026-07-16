import { cn } from "@/lib/cn";

/** Badges et jauges partagés par les pages de la console d'administration. */

export function PlanBadge({ plan }: { plan: string }) {
  const style =
    plan === "ENTERPRISE"
      ? "border-brand/35 bg-brand/5 text-brand"
      : plan === "PRO"
        ? "border-ok/40 bg-ok-tint text-ok-strong"
        : "border-line bg-subtle text-muted";
  return (
    <span className={cn("inline-block rounded-full border px-2.5 py-0.5 text-[11px] font-semibold", style)}>
      {plan}
    </span>
  );
}

export function StatutBadge({ status }: { status: string }) {
  const actif = status === "ACTIVE";
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-[11px] font-semibold",
        actif ? "border-ok/40 bg-ok-tint text-ok-strong" : "border-danger/30 bg-danger/5 text-danger"
      )}
    >
      <span className={cn("h-1.5 w-1.5 rounded-full", actif ? "bg-ok" : "bg-danger")} />
      {actif ? "Actif" : "Bloqué"}
    </span>
  );
}

export function ConsoBar({ pct, illimite }: { pct: number; illimite: boolean }) {
  if (illimite) return <span className="text-xs text-muted">Illimité</span>;
  const danger = pct >= 90;
  return (
    <div className="flex items-center gap-2">
      <div className="h-2 w-24 overflow-hidden rounded-full bg-subtle">
        <div
          className={cn("h-full rounded-full", danger ? "bg-danger" : "bg-brand")}
          style={{ width: `${Math.min(100, pct)}%` }}
        />
      </div>
      <span className="w-10 text-right font-mono text-xs text-muted">{pct.toFixed(0)}%</span>
    </div>
  );
}
