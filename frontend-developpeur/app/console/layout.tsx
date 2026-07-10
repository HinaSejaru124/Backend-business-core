"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import type { ReactNode } from "react";
import Logo from "@/components/Logo";
import { ButtonLink } from "@/components/Button";
import {
  IconLayers,
  IconKey,
  IconActivity,
  IconBook,
  IconBolt,
  IconExternal,
  IconLogout,
  IconShield,
} from "@/components/icons";
import { useAuth } from "@/lib/auth-context";
import { API_BASE } from "@/lib/api";
import { cn } from "@/lib/cn";

const NAV = [
  { href: "/console", label: "Tableau de bord", icon: IconLayers, exact: true },
  { href: "/console/audit", label: "Audit", icon: IconActivity },
  { href: "/console/docs", label: "Documentation", icon: IconBook },
  { href: "/console/pricing", label: "Tarifs & Consommation", icon: IconBolt },
  { href: "/console/api-key", label: "Clé d'API", icon: IconKey },
];

/**
 * Shell PLEIN ÉCRAN de l'espace développeur.
 * C'est une application distincte du site vitrine : aucune navbar marketing ici
 * (cf. SiteChrome). Sidebar fixe à gauche + barre supérieure ; le contenu occupe
 * toute la largeur restante. La garde d'authentification vit ICI, en un seul endroit.
 */
export default function ConsoleLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { status, principal, profil, logout } = useAuth();

  // ── Garde : session en cours de vérification ──
  if (status === "loading") {
    return (
      <div className="grid min-h-screen place-items-center bg-subtle">
        <div className="text-center">
          <div className="mx-auto h-8 w-8 animate-pulse bg-line" aria-hidden />
          <p className="mt-4 text-sm text-muted">Vérification de votre session…</p>
        </div>
      </div>
    );
  }

  // ── Garde : pas de session valide ──
  if (status === "anon") {
    return (
      <div className="grid min-h-screen place-items-center bg-subtle px-6">
        <div className="w-full max-w-md border border-line bg-white p-8 text-center">
          <IconShield className="mx-auto h-6 w-6 text-brand" />
          <h1 className="mt-4 font-display text-xl font-bold text-ink">Connexion requise</h1>
          <p className="mt-2 text-sm text-muted">
            Votre session n&apos;est pas active. Connectez-vous pour accéder à votre espace.
          </p>
          <ButtonLink href="/login?tab=login" className="mt-6 w-full">
            Se connecter
          </ButtonLink>
          <Link href="/" className="mt-4 inline-block text-xs text-muted hover:text-ink">
            ← Retour au site
          </Link>
        </div>
      </div>
    );
  }

  function onLogout() {
    logout();
    router.push("/");
  }

  const isActive = (item: (typeof NAV)[number]) =>
    item.exact ? pathname === item.href : pathname.startsWith(item.href);
  const current = NAV.find(isActive)?.label ?? "Espace développeur";

  return (
    <div className="flex min-h-screen bg-subtle">
      {/* ── Sidebar ── */}
      <aside className="fixed inset-y-0 left-0 hidden w-64 flex-col border-r border-line bg-white md:flex">
        <div className="flex h-16 flex-none items-center border-b border-line px-6">
          <Link href="/console" aria-label="Business Core">
            <Logo plain />
          </Link>
        </div>

        {/* Compte réel */}
        <div className="border-b border-line px-4 py-4">
          <div className="flex items-center gap-3">
            <span className="grid h-9 w-9 flex-none place-items-center bg-ink font-mono text-sm font-semibold text-white">
              {(principal || "?").charAt(0).toUpperCase()}
            </span>
            <div className="min-w-0">
              <div className="truncate text-sm font-medium text-ink">{principal || "Mon compte"}</div>
              <div className="mt-0.5 font-mono text-[11px] uppercase tracking-wider text-muted">
                {profil?.owner ? <span className="text-brand">Owner</span> : "Développeur"}
              </div>
            </div>
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto px-3 py-4">
          {NAV.map((item) => {
            const Icon = item.icon;
            const activeItem = isActive(item);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex items-center gap-3 border-l-2 px-3 py-2.5 text-sm transition-colors",
                  activeItem
                    ? "border-brand bg-tint font-medium text-ink"
                    : "border-transparent text-muted hover:bg-subtle hover:text-ink"
                )}
              >
                <Icon className={cn("h-4 w-4", activeItem ? "text-brand" : "text-muted")} />
                {item.label}
              </Link>
            );
          })}

          <div className="mt-4 border-t border-line pt-4">
            <div className="px-3 pb-1 font-mono text-[10px] uppercase tracking-wider text-muted">
              Ressources
            </div>
            <a
              href={`${API_BASE}/swagger-ui.html`}
              target="_blank"
              rel="noreferrer"
              className="flex items-center gap-3 px-3 py-2.5 text-sm text-muted transition-colors hover:bg-subtle hover:text-ink"
            >
              <IconExternal className="h-4 w-4" /> Swagger UI
            </a>
          </div>
        </nav>

        {/* Déconnexion */}
        <div className="flex-none border-t border-line p-3">
          <button
            onClick={onLogout}
            className="flex w-full items-center gap-3 px-3 py-2.5 text-sm text-danger transition-colors hover:bg-danger/5"
          >
            <IconLogout className="h-4 w-4" /> Se déconnecter
          </button>
        </div>
      </aside>

      {/* ── Zone principale (décalée de la sidebar) ── */}
      <div className="flex min-w-0 flex-1 flex-col md:pl-64">
        {/* Barre supérieure */}
        <header className="sticky top-0 z-30 flex h-16 flex-none items-center justify-between gap-4 border-b border-line bg-white/85 px-6 backdrop-blur-md md:px-10">
          <div className="min-w-0">
            <div className="font-mono text-[11px] uppercase tracking-wider text-muted">Console</div>
            <div className="truncate font-display text-[15px] font-semibold text-ink">{current}</div>
          </div>
          <div className="flex items-center gap-4">
            <Link
              href="/"
              className="hidden items-center gap-1.5 text-sm text-muted transition-colors hover:text-ink sm:flex"
            >
              <IconExternal className="h-4 w-4" /> Voir le site
            </Link>
            <span className="grid h-9 w-9 place-items-center bg-ink font-mono text-sm font-semibold text-white">
              {(principal || "?").charAt(0).toUpperCase()}
            </span>
          </div>
        </header>

        {/* Onglets mobile (sidebar cachée) */}
        <div className="flex gap-1 overflow-x-auto border-b border-line bg-white px-4 py-2 md:hidden">
          {NAV.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex-none whitespace-nowrap px-3 py-1.5 text-[13px] font-medium transition-colors",
                isActive(item) ? "bg-ink text-white" : "text-muted hover:text-ink"
              )}
            >
              {item.label}
            </Link>
          ))}
          <button
            onClick={onLogout}
            className="ml-auto flex-none whitespace-nowrap px-3 py-1.5 text-[13px] font-medium text-danger"
          >
            Déconnexion
          </button>
        </div>

        {/* Contenu — pleine largeur */}
        <div className="flex-1 px-6 py-8 md:px-10">{children}</div>
      </div>
    </div>
  );
}
