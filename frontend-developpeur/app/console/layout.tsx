"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";
import Logo from "@/components/Logo";
import { ButtonLink } from "@/components/Button";
import {
  IconLayers,
  IconKey,
  IconActivity,
  IconBook,
  IconBolt,
  IconBuilding,
  IconExternal,
  IconLogout,
  IconShield,
} from "@/components/icons";
import { useAuth } from "@/lib/auth-context";
import { API_BASE, adminMe } from "@/lib/api";
import { cn } from "@/lib/cn";

const NAV = [
  { href: "/console", label: "Tableau de bord", icon: IconLayers, exact: true },
  { href: "/console/businesses", label: "Entreprises", icon: IconBuilding },
  { href: "/console/api-key", label: "Clés d'API", icon: IconKey },
  { href: "/console/audit", label: "Audit", icon: IconActivity },
  { href: "/console/docs", label: "Documentation", icon: IconBook },
  { href: "/console/pricing", label: "Tarifs & Consommation", icon: IconBolt },
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
  const [sessionExpired, setSessionExpired] = useState(false);
  const [estAdmin, setEstAdmin] = useState(false);

  useEffect(() => {
    function onExpired() {
      setSessionExpired(true);
    }
    window.addEventListener("bc:session-expired", onExpired);
    return () => window.removeEventListener("bc:session-expired", onExpired);
  }, []);

  // L'entrée « Administration » n'apparaît que pour un compte réellement admin (GET /v1/admin/me).
  useEffect(() => {
    if (status !== "authed") {
      setEstAdmin(false);
      return;
    }
    adminMe()
      .then(() => setEstAdmin(true))
      .catch(() => setEstAdmin(false));
  }, [status]);

  useEffect(() => {
    if (status === "authed") setSessionExpired(false);
  }, [status]);

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
          <h1 className="mt-4 font-display text-xl font-bold text-ink">
            {sessionExpired ? "Session expirée" : "Connexion requise"}
          </h1>
          <p className="mt-2 text-sm text-muted">
            {sessionExpired
              ? "Votre session a expiré. Reconnectez-vous pour continuer."
              : "Votre session n'est pas active. Connectez-vous pour accéder à votre espace."}
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
      {/* ── Sidebar — fond clair, l'item actif se teinte légèrement en bleu.
           La bordure droite ne s'applique PAS au bandeau du haut (logo) : ainsi la barre
           supérieure est lisse et continue, avec un seul trait horizontal. ── */}
      <aside className="fixed inset-y-0 left-0 hidden w-64 flex-col bg-white md:flex">
        <div className="flex h-16 flex-none items-center border-b border-line px-6">
          <Link href="/console" aria-label="Business Core">
            <Logo plain />
          </Link>
        </div>

        {/* Navigation — hover progressif, actif = fond bleu clair + barre bleu foncé */}
        <nav className="flex-1 overflow-y-auto border-r border-line px-3 py-4">
          {NAV.map((item) => {
            const Icon = item.icon;
            const activeItem = isActive(item);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "group relative flex items-center gap-3 overflow-hidden px-3.5 py-3 text-sm transition-all duration-200 ease-out",
                  activeItem
                    ? "bg-tint font-medium text-ink"
                    : "text-muted hover:translate-x-0.5 hover:bg-subtle hover:text-ink"
                )}
              >
                <span
                  className={cn(
                    "absolute inset-y-1 left-0 w-[3px] bg-brand transition-transform duration-200 ease-out",
                    activeItem ? "scale-y-100" : "scale-y-0 group-hover:scale-y-100"
                  )}
                  aria-hidden
                />
                <Icon
                  className={cn(
                    "h-4 w-4 transition-colors",
                    activeItem ? "text-brand" : "text-muted group-hover:text-brand"
                  )}
                />
                {item.label}
              </Link>
            );
          })}

          {estAdmin && (
            <div className="mt-4 border-t border-line pt-4">
              <div className="px-3 pb-1 font-mono text-[10px] uppercase tracking-wider text-muted">
                Plateforme
              </div>
              <Link
                href="/admin"
                className="group flex items-center gap-3 px-3.5 py-3 text-sm font-medium text-brand transition-all duration-200 hover:translate-x-0.5 hover:bg-tint"
              >
                <IconShield className="h-4 w-4" /> Administration
              </Link>
            </div>
          )}

          <div className="mt-4 border-t border-line pt-4">
            <div className="px-3 pb-1 font-mono text-[10px] uppercase tracking-wider text-muted">
              Ressources
            </div>
            <a
              href={`${API_BASE}/swagger-ui.html`}
              target="_blank"
              rel="noreferrer"
              className="group flex items-center gap-3 px-3.5 py-3 text-sm text-muted transition-all duration-200 hover:translate-x-0.5 hover:bg-subtle hover:text-ink"
            >
              <IconExternal className="h-4 w-4 transition-colors group-hover:text-brand" /> Swagger UI
            </a>
          </div>
        </nav>

        {/* Déconnexion */}
        <div className="flex-none border-r border-t border-line p-3">
          <button
            onClick={onLogout}
            className="flex w-full items-center gap-3 px-3.5 py-3 text-sm text-danger transition-colors hover:bg-danger/5"
          >
            <IconLogout className="h-4 w-4" /> Se déconnecter
          </button>
        </div>
      </aside>

      {/* ── Zone principale (décalée de la sidebar) ── */}
      <div className="flex min-w-0 flex-1 flex-col md:pl-64">
        {/* Barre supérieure — identité connectée en haut à droite (seul endroit où elle apparaît) */}
        <header className="sticky top-0 z-30 flex h-16 flex-none items-center justify-between gap-4 border-b border-line bg-tint/80 px-6 backdrop-blur-md md:px-10">
          <div className="min-w-0">
            <div className="text-[11px] font-semibold uppercase tracking-wider text-muted">Console</div>
            <div className="truncate font-display text-[15px] font-semibold text-ink">{current}</div>
          </div>
          <div className="flex items-center gap-3">
            <div className="hidden text-right sm:block">
              <div className="truncate text-[13.5px] font-semibold leading-tight text-ink">
                {principal || "Mon compte"}
              </div>
              <div className="text-[11px] font-semibold uppercase tracking-wider text-muted">
                {profil?.owner ? <span className="text-brand">Owner</span> : "Développeur"}
                {profil?.plan && <span> · {profil.plan}</span>}
              </div>
            </div>
            <span className="grid h-9 w-9 flex-none place-items-center rounded-full bg-gradient-to-br from-ink to-[#132a5c] font-mono text-sm font-semibold text-white shadow-glow-sm">
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
