"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { cn } from "@/lib/cn";
import { useSession } from "@/lib/useSession";
import { estAutorise, homePour, LIBELLE_ROLE } from "@/lib/roles";
import { logout } from "@/lib/api";
import type { Role } from "@/lib/types";
import Logo from "./Logo";
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
  IconSearch,
  IconChevronDown,
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
          { href: "/vente", label: "Poste de vente", icon: IconCart },
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
 * Shell plein écran de PharmaCore — sidebar dégradée, navigation propre à chaque rôle. La page de
 * connexion ({@code /connexion}) n'a pas de shell (écran de bienvenue à part) ; toutes les autres
 * routes sont gardées : session absente → redirection vers la connexion, rôle non autorisé pour la
 * route courante → redirection vers l'espace naturel du rôle (cf. lib/roles.ts). Refonte visuelle
 * uniquement — la garde d'accès et la déconnexion réelle sont strictement inchangées.
 */
export default function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [menuOuvert, setMenuOuvert] = useState(false);
  const [recherche, setRecherche] = useState("");
  const menuRef = useRef<HTMLDivElement>(null);
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

  useEffect(() => {
    function onClickDehors(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOuvert(false);
    }
    document.addEventListener("mousedown", onClickDehors);
    return () => document.removeEventListener("mousedown", onClickDehors);
  }, []);

  if (estConnexion) {
    return <>{children}</>;
  }

  if (session.state === "loading") {
    return (
      <div className="grid min-h-screen place-items-center bg-canvas text-sm text-muted">
        Chargement…
      </div>
    );
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

  /** Recherche réelle (pas décorative) : filtre le catalogue déjà chargé par /medicaments. */
  function onRecherche(e: FormEvent) {
    e.preventDefault();
    const q = recherche.trim();
    router.push(q ? `/medicaments?q=${encodeURIComponent(q)}` : "/medicaments");
  }

  return (
    <div className="flex min-h-screen bg-canvas">
      {/* Sidebar — dégradé de marque, pleine hauteur, sections groupées, items en pilules douces */}
      <aside className="fixed inset-y-0 left-0 hidden w-72 flex-col bg-ink-gradient md:flex">
        <div className="flex h-20 flex-none items-center border-b border-white/10 px-6">
          <Logo dark size="sm" subtitle="Pharmacie du Centre" />
        </div>

        <nav className="flex-1 overflow-y-auto px-4 py-6">
          {NAV_GROUPS.map((group) => (
            <div key={group.label} className="mb-6 last:mb-0">
              <div className="mb-2 px-3.5 font-mono text-[10.5px] uppercase tracking-wider text-white/40">
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
                      "mb-1 flex items-center gap-3.5 rounded-xl px-3.5 py-3 text-[14.5px] font-medium transition-all duration-150",
                      active
                        ? "bg-white/12 text-white shadow-inset"
                        : "text-white/65 hover:bg-white/[0.07] hover:text-white"
                    )}
                  >
                    <Icon className={cn("h-[18px] w-[18px] flex-none", active ? "text-brand-light" : "text-white/45")} />
                    {item.label}
                  </Link>
                );
              })}
            </div>
          ))}
        </nav>
      </aside>

      {/* Zone principale */}
      <div className="flex min-w-0 flex-1 flex-col md:pl-72">
        <header className="sticky top-0 z-30 flex h-20 flex-none items-center justify-between gap-4 border-b border-line bg-white/85 px-6 backdrop-blur-md md:px-10">
          <div className="flex min-w-0 items-center gap-4">
            <button
              onClick={() => setOpen((o) => !o)}
              className="grid h-10 w-10 flex-none place-items-center rounded-xl border border-line text-ink md:hidden"
              aria-label="Menu"
            >
              {open ? <IconClose className="h-5 w-5" /> : <IconMenu className="h-5 w-5" />}
            </button>
            <div className="min-w-0 md:hidden">
              <div className="truncate font-display text-lg font-bold text-ink">{current}</div>
            </div>
          </div>

          <form onSubmit={onRecherche} className="hidden max-w-sm flex-1 md:block">
            <div className="relative">
              <IconSearch className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
              <input
                value={recherche}
                onChange={(e) => setRecherche(e.target.value)}
                placeholder="Rechercher un médicament…"
                className="h-11 w-full rounded-xl border border-line bg-subtle pl-10 pr-4 text-sm text-body outline-none transition-all placeholder:text-muted/60 focus:border-brand focus:bg-white focus:ring-4 focus:ring-brand/10"
              />
            </div>
          </form>

          <div className="relative flex-none" ref={menuRef}>
            <button
              onClick={() => setMenuOuvert((o) => !o)}
              className="flex items-center gap-2.5 rounded-xl py-1.5 pl-1.5 pr-2.5 transition-colors hover:bg-subtle"
            >
              <span className="grid h-9 w-9 flex-none place-items-center rounded-full bg-brand-gradient font-display text-sm font-bold text-white glow-brand">
                {(session.data.nomAffichage || "?").charAt(0).toUpperCase()}
              </span>
              <span className="hidden text-left sm:block">
                <span className="block max-w-[140px] truncate text-[13.5px] font-semibold text-ink">
                  {session.data.nomAffichage}
                </span>
                <span className="block font-mono text-[10.5px] uppercase tracking-wider text-brand">
                  {role ? LIBELLE_ROLE[role] : ""}
                </span>
              </span>
              <IconChevronDown className={cn("h-4 w-4 text-muted transition-transform", menuOuvert && "rotate-180")} />
            </button>

            {menuOuvert && (
              <div className="absolute right-0 top-[calc(100%+8px)] w-56 animate-scale-in rounded-2xl border border-line bg-white p-1.5 shadow-pop">
                <div className="px-3 py-2.5">
                  <div className="truncate text-[13.5px] font-semibold text-ink">{session.data.nomAffichage}</div>
                  <div className="mt-0.5 font-mono text-[10.5px] uppercase tracking-wider text-muted">
                    {role ? LIBELLE_ROLE[role] : ""}
                  </div>
                </div>
                <div className="my-1 h-px bg-line-soft" />
                <button
                  onClick={onDeconnexion}
                  className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2.5 text-[13.5px] font-medium text-danger transition-colors hover:bg-danger/5"
                >
                  <IconLogout className="h-4 w-4" />
                  Déconnexion
                </button>
              </div>
            )}
          </div>
        </header>

        {open && (
          <div className="flex flex-col gap-1 overflow-x-auto rounded-b-2xl bg-ink-gradient px-3 py-3 md:hidden">
            {NAV.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setOpen(false)}
                className={cn(
                  "flex-none whitespace-nowrap rounded-xl px-3.5 py-2.5 text-[13.5px] font-medium transition-colors",
                  isActive(item) ? "bg-white/12 text-white" : "text-white/70 hover:text-white"
                )}
              >
                {item.label}
              </Link>
            ))}
            <button
              onClick={onDeconnexion}
              className="flex-none whitespace-nowrap rounded-xl px-3.5 py-2.5 text-left text-[13.5px] font-medium text-white/70 hover:text-white"
            >
              Déconnexion
            </button>
          </div>
        )}

        <div className="flex-1 px-6 py-9 md:px-10">{children}</div>
      </div>
    </div>
  );
}
