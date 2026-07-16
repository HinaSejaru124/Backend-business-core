"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { cn } from "@/lib/cn";
import {
  IconLayers,
  IconCart,
  IconPill,
  IconFileText,
  IconUsers,
  IconHistory,
  IconTruck,
  IconAlertTriangle,
  IconMenu,
  IconClose,
} from "./icons";

type NavItem = { href: string; label: string; icon: typeof IconLayers; exact?: boolean };

/** Sections groupées (adapté du modèle fourni — pas copié, palette et radius restent les nôtres). */
const NAV_GROUPS: { label: string; items: NavItem[] }[] = [
  {
    label: "Principal",
    items: [
      { href: "/", label: "Tableau de bord", icon: IconLayers, exact: true },
      // "exact" sur /vente : sinon "/ventes".startsWith("/vente") === true et les deux
      // items s'activaient ensemble (bug signalé — collision de préfixe entre les deux routes).
      { href: "/vente", label: "Vente", icon: IconCart, exact: true },
    ],
  },
  {
    label: "Catalogue & stock",
    items: [
      { href: "/medicaments", label: "Médicaments", icon: IconPill },
      { href: "/alertes", label: "Alertes stock", icon: IconAlertTriangle },
      { href: "/fournisseurs", label: "Fournisseurs", icon: IconTruck },
    ],
  },
  {
    label: "Patients & ventes",
    items: [
      { href: "/clients", label: "Clients", icon: IconUsers },
      { href: "/ordonnances", label: "Ordonnances", icon: IconFileText },
      { href: "/ventes", label: "Historique des ventes", icon: IconHistory },
    ],
  },
];
const NAV = NAV_GROUPS.flatMap((g) => g.items);

/**
 * Shell plein écran unique de PharmaCore — sidebar plein-vert foncé (pas de distinction
 * site vitrine / espace connecté, contrairement à frontend-developpeur) : cette application
 * EST la console, du début à la fin. Pas d'authentification en v1 (cf. frontend-test.md §1).
 */
export default function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  const isActive = (item: NavItem) =>
    item.exact ? pathname === item.href : pathname.startsWith(item.href);
  const current = NAV.find(isActive)?.label ?? "PharmaCore";

  return (
    <div className="flex min-h-screen bg-subtle">
      {/* Sidebar — fond vert foncé, pleine hauteur, sections groupées */}
      <aside className="fixed inset-y-0 left-0 hidden w-72 flex-col bg-ink md:flex">
        <div className="flex h-20 flex-none items-center gap-3 border-b border-white/10 px-6">
          <span className="grid h-11 w-11 flex-none place-items-center bg-brand font-display text-xl font-bold text-white">
            +
          </span>
          <div className="min-w-0">
            <div className="font-display text-[19px] font-bold leading-tight text-white">PharmaCore</div>
            <div className="mt-0.5 truncate font-mono text-[11px] uppercase tracking-wider text-brand">
              Pharmacie du Centre
            </div>
          </div>
        </div>

        <nav className="flex-1 overflow-y-auto px-4 py-6">
          {NAV_GROUPS.map((group) => (
            <div key={group.label} className="mb-6 last:mb-0">
              <div className="mb-2 px-4 font-mono text-[10.5px] uppercase tracking-wider text-white/40">
                {group.label}
              </div>
              {group.items.map((item) => {
                const Icon = item.icon;
                const active = isActive(item);
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={cn(
                      "mb-1.5 flex items-center gap-3.5 px-4 py-3 text-[15px] font-medium transition-all",
                      active
                        ? "bg-brand text-white shadow-pop"
                        : "text-white/70 hover:bg-white/10 hover:text-white"
                    )}
                  >
                    <Icon className={cn("h-5 w-5 flex-none", active ? "text-white" : "text-brand")} />
                    {item.label}
                  </Link>
                );
              })}
            </div>
          ))}
        </nav>

        <div className="flex-none border-t border-white/10 px-6 py-5">
          <p className="font-mono text-[10.5px] leading-relaxed text-white/50">
            Bâtie avec la clé API Business Core.
            <br />
            Application de test — GUIDE-PROJET-PHARMACORE.md
          </p>
        </div>
      </aside>

      {/* Zone principale */}
      <div className="flex min-w-0 flex-1 flex-col md:pl-72">
        <header className="sticky top-0 z-30 flex h-20 flex-none items-center justify-between gap-4 border-b-2 border-brand bg-white px-8">
          <div className="flex items-center gap-4">
            <button
              onClick={() => setOpen((o) => !o)}
              className="grid h-10 w-10 place-items-center border border-line text-ink md:hidden"
              aria-label="Menu"
            >
              {open ? <IconClose className="h-5 w-5" /> : <IconMenu className="h-5 w-5" />}
            </button>
            <div>
              <div className="font-mono text-[12px] uppercase tracking-wider text-brand">PharmaCore</div>
              <div className="font-display text-xl font-bold text-ink">{current}</div>
            </div>
          </div>
        </header>

        {open && (
          <div className="flex gap-1 overflow-x-auto border-b border-line bg-ink px-3 py-2 md:hidden">
            {NAV.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setOpen(false)}
                className={cn(
                  "flex-none whitespace-nowrap px-3 py-2 text-[13px] font-medium transition-colors",
                  isActive(item) ? "bg-brand text-white" : "text-white/70 hover:text-white"
                )}
              >
                {item.label}
              </Link>
            ))}
          </div>
        )}

        <div className="flex-1 px-8 py-10 md:px-12">{children}</div>
      </div>
    </div>
  );
}
