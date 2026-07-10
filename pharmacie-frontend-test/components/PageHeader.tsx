import type { ReactNode } from "react";

/** Bandeau d'en-tête vert foncé — cohérent sur toutes les pages, casse le blanc dominant. */
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
    <div className="flex flex-col justify-between gap-5 border-l-4 border-brand bg-ink px-8 py-8 sm:flex-row sm:items-center">
      <div>
        <div className="font-mono text-[12px] uppercase tracking-wider text-brand">{eyebrow}</div>
        <h1 className="mt-2 font-display text-4xl font-bold text-white">{title}</h1>
        {description && <p className="mt-2 max-w-xl text-[15px] text-white/70">{description}</p>}
      </div>
      {action && <div className="flex-none">{action}</div>}
    </div>
  );
}
