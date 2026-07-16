import type { ReactNode } from "react";

/** En-tête clair et aéré — cohérent sur toutes les pages, dans l'esprit de la maquette de référence. */
export default function PageHeader({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow: string;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex flex-col justify-between gap-5 pb-2 sm:flex-row sm:items-end">
      <div>
        <div className="font-mono text-[12px] font-semibold uppercase tracking-wider text-brand">{eyebrow}</div>
        <h1 className="mt-2 font-display text-3xl font-bold tracking-tight text-ink">{title}</h1>
        {description && <p className="mt-2 max-w-xl text-[15px] text-muted">{description}</p>}
      </div>
      {action && <div className="flex-none">{action}</div>}
    </div>
  );
}
