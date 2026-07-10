"use client";

import { useEffect, useState } from "react";
import { ButtonLink } from "./Button";
import { API_BASE } from "@/lib/api";
import { cn } from "@/lib/cn";

const NAV = [
  { id: "demarrage", label: "Démarrage rapide" },
  { id: "authentification", label: "Authentification" },
  { id: "reference", label: "Référence API" },
  { id: "erreurs", label: "Format d'erreur" },
];

export default function DocsNav() {
  const [active, setActive] = useState<string>(NAV[0].id);

  useEffect(() => {
    const els = NAV.map((n) => document.getElementById(n.id)).filter(Boolean) as HTMLElement[];
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) setActive(e.target.id);
        });
      },
      { rootMargin: "-15% 0px -75% 0px", threshold: 0 }
    );
    els.forEach((el) => io.observe(el));
    return () => io.disconnect();
  }, []);

  return (
    <div className="sticky top-24">
      <div className="font-mono text-[12px] uppercase tracking-wider text-muted">Documentation</div>
      <nav className="mt-4 flex flex-col border-l border-line">
        {NAV.map((n) => (
          <a
            key={n.id}
            href={`#${n.id}`}
            className={cn(
              "-ml-px border-l-2 px-4 py-2 text-sm transition-all",
              active === n.id
                ? "border-brand font-medium text-ink"
                : "border-transparent text-muted hover:border-line hover:text-ink"
            )}
          >
            {n.label}
          </a>
        ))}
      </nav>
      <div className="mt-6 border border-line p-4">
        <div className="text-sm font-medium text-ink">Swagger UI</div>
        <p className="mt-1 text-xs text-muted">Explorez et testez l&apos;API en direct.</p>
        <ButtonLink href={`${API_BASE}/swagger-ui.html`} external variant="secondary" size="sm" className="mt-3 w-full">
          Ouvrir Swagger
        </ButtonLink>
      </div>
    </div>
  );
}
