"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { cn } from "@/lib/cn";
import { useSession } from "@/lib/useSession";
import { estAutorise, homePour, LIBELLE_ROLE } from "@/lib/roles";
import { logout } from "@/lib/api";
import type { Role } from "@/lib/types";
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
  IconShield,
  IconLogout,
} from "./icons";

type NavItem = { href: string; label: string; icon: typeof IconLayers; exact?: boolean };
type NavGroup = { label: string; items: NavItem[] };

/** Chaque rôle ne voit que sa propre navigation — c'est ici que « chacun a son espace » se concrétise. */
function navGroupsPour(role: Role | null): NavGroup[] {
  if (role === "TITULAIRE") {
    return [
      {
        label: "Principal",
        items: [{ href: "/", label: "Tableau de bord", icon: IconLayers, exact: true }],
      },
      {
        label: "Catalogue & stock",
        items: [
          { href: "/medicaments", label: "Médicaments", icon: IconPill },
          { href: "/alertes", label: "Alertes stock", icon: IconAlertTriangle },
          { href: "/fournisseurs", label: "Fournisseurs", icon: IconTruck },
          { href: "/commandes", label: "Commandes fournisseurs", icon: IconCart },
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
      {
        label: "Équipe",
        items: [{ href: "/admin", label: "Personnel", icon: IconShield }],
      },
    ];
  }
  if (role === "PHARMACIEN_RESPONSABLE" || role === "CAISSIER") {
    const items: NavItem[] = [
      { href: "/vente", label: "Poste de vente", icon: IconCart, exact: true },
      { href: "/clients", label: "Clients", icon: IconUsers },
    ];
    if (role === "PHARMACIEN_RESPONSABLE") {
      items.push({ href: "/ordonnances", label: "Ordonnances", icon: IconFileText });
    }
    return [{ label: "Principal", items }];
  }
  return [];
}

/**
 * Shell plein écran de PharmaCore — sidebar plein-vert foncé, navigation propre à chaque rôle. La page
 * de connexion ({@code /connexion}) n'a pas de shell (écran de bienvenue à part) ; toutes les autres
 * routes sont gardées : session absente → redirection vers la connexion, rôle non autorisé pour la
 * route courante → redirection vers l'espace naturel du rôle (cf. lib/roles.ts).
 */
export default function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const { session, rafraichir } = useSession();

  const estConnexion = pathname === "/connexion";

  useEffect(() => {
    if (estConnexion || session.state !== "ok") return;
    if (!session.data?.connecte) {
      router.replace("/connexion");
      return;
    }
    if (!estAutorise(pathname, session.data.role)) {
      router.replace(homePour(session.data.role));
    }
  }, [estConnexion, session, pathname, router]);

  if (estConnexion) {
    return <>{children}</>;
  }

  if (session.state === "loading") {
    return <div className="grid min-h-screen place-items-center text-sm text-muted">Chargement…</div>;
  }

  if (!session.data?.connecte || !estAutorise(pathname, session.data.role)) {
    return null; // redirection en cours (useEffect ci-dessus)
  }

  const role = session.data.role;
  const NAV_GROUPS = navGroupsPour(role);
  const NAV = NAV_GROUPS.flatMap((g) => g.items);

  const isActive = (item: NavItem) =>
    item.exact ? pathname === item.href : pathname.startsWith(item.href);
  const current = NAV.find(isActive)?.label ?? "PharmaCore";

  async function onDeconnexion() {
    await logout().catch(() => {});
    rafraichir();
    router.replace("/connexion");
  }

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

        <div className="flex-none border-t border-white/10 px-5 py-5">
          <div className="mb-3 px-1">
            <div className="truncate text-[13px] font-medium text-white">{session.data.nomAffichage}</div>
            <div className="font-mono text-[10.5px] uppercase tracking-wider text-brand">
              {role ? LIBELLE_ROLE[role] : ""}
            </div>
          </div>
          <button
            onClick={onDeconnexion}
            className="flex w-full items-center gap-2.5 px-3 py-2.5 text-[13.5px] font-medium text-white/70 transition-colors hover:bg-white/10 hover:text-white"
          >
            <IconLogout className="h-4 w-4 text-brand" />
            Déconnexion
          </button>
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
          <div className="flex flex-col gap-1 overflow-x-auto border-b border-line bg-ink px-3 py-2 md:hidden">
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
            <button
              onClick={onDeconnexion}
              className="flex-none whitespace-nowrap px-3 py-2 text-left text-[13px] font-medium text-white/70 hover:text-white"
            >
              Déconnexion
            </button>
          </div>
        )}

        <div className="flex-1 px-8 py-10 md:px-12">{children}</div>
      </div>
    </div>
  );
}
