"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";
import Logo from "@/components/Logo";
import { ButtonLink } from "@/components/Button";
import {
  IconLayers,
  IconUsers,
  IconActivity,
  IconWallet,
  IconExternal,
  IconLogout,
  IconShield,
} from "@/components/icons";
import { useAuth } from "@/lib/auth-context";
import { adminMe } from "@/lib/api";
import { cn } from "@/lib/cn";

const NAV = [
  { href: "/admin", label: "Tableau de bord", icon: IconLayers, exact: true },
  { href: "/admin/developers", label: "Développeurs", icon: IconUsers },
  { href: "/admin/track", label: "Track des requêtes", icon: IconActivity },
  { href: "/admin/billing", label: "Facturation", icon: IconWallet },
];

/**
 * Shell PLEIN ÉCRAN de la console d'administration de la plateforme Business Core. Même style que
 * l'espace développeur (SiteChrome n'affiche pas la chrome marketing ici). Double garde : session
 * valide (comme la console) PUIS droits admin réels (GET /v1/admin/me → 403 si non-admin).
 */
export default function AdminLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { status, principal, logout } = useAuth();
  const [adminState, setAdminState] = useState<"loading" | "ok" | "denied">("loading");

  useEffect(() => {
    if (status !== "authed") return;
    setAdminState("loading");
    adminMe()
      .then(() => setAdminState("ok"))
      .catch(() => setAdminState("denied"));
  }, [status]);

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

  if (status === "anon") {
    return (
      <div className="grid min-h-screen place-items-center bg-subtle px-6">
        <div className="w-full max-w-md rounded-2xl border border-line bg-white p-8 text-center shadow-glow-sm">
          <IconShield className="mx-auto h-6 w-6 text-brand" />
          <h1 className="mt-4 font-display text-xl font-bold text-ink">Connexion requise</h1>
          <p className="mt-2 text-sm text-muted">
            Connectez-vous avec un compte administrateur pour accéder à la console de plateforme.
          </p>
          <ButtonLink href="/login?tab=login" className="mt-6 w-full">
            Se connecter
          </ButtonLink>
        </div>
      </div>
    );
  }

  if (adminState === "loading") {
    return (
      <div className="grid min-h-screen place-items-center bg-subtle">
        <p className="text-sm text-muted">Vérification des droits d&apos;administration…</p>
      </div>
    );
  }

  if (adminState === "denied") {
    return (
      <div className="grid min-h-screen place-items-center bg-subtle px-6">
        <div className="w-full max-w-md rounded-2xl border border-line bg-white p-8 text-center shadow-glow-sm">
          <IconShield className="mx-auto h-6 w-6 text-danger" />
          <h1 className="mt-4 font-display text-xl font-bold text-ink">Accès réservé</h1>
          <p className="mt-2 text-sm text-muted">
            Votre compte <span className="font-medium text-ink">{principal}</span> n&apos;a pas les
            droits d&apos;administration de la plateforme.
          </p>
          <ButtonLink href="/console" variant="secondary" className="mt-6 w-full">
            ← Retour à ma console
          </ButtonLink>
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
  const current = NAV.find(isActive)?.label ?? "Administration";

  return (
    <div className="flex min-h-screen bg-subtle">
      <aside className="fixed inset-y-0 left-0 hidden w-64 flex-col bg-white md:flex">
        <div className="flex h-16 flex-none items-center gap-2.5 border-b border-line px-6">
          <Link href="/admin" aria-label="Business Core — Administration">
            <Logo plain />
          </Link>
          <span className="rounded-full border border-brand/30 bg-brand/5 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-brand">
            Admin
          </span>
        </div>

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

          <div className="mt-4 border-t border-line pt-4">
            <div className="px-3 pb-1 font-mono text-[10px] uppercase tracking-wider text-muted">
              Navigation
            </div>
            <Link
              href="/console"
              className="group flex items-center gap-3 px-3.5 py-3 text-sm text-muted transition-all duration-200 hover:translate-x-0.5 hover:bg-subtle hover:text-ink"
            >
              <IconExternal className="h-4 w-4 transition-colors group-hover:text-brand" /> Ma console développeur
            </Link>
          </div>
        </nav>

        <div className="flex-none border-r border-t border-line p-3">
          <button
            onClick={onLogout}
            className="flex w-full items-center gap-3 px-3.5 py-3 text-sm text-danger transition-colors hover:bg-danger/5"
          >
            <IconLogout className="h-4 w-4" /> Se déconnecter
          </button>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col md:pl-64">
        <header className="sticky top-0 z-30 flex h-16 flex-none items-center justify-between gap-4 border-b border-line bg-tint/80 px-6 backdrop-blur-md md:px-10">
          <div className="min-w-0">
            <div className="text-[11px] font-semibold uppercase tracking-wider text-muted">
              Administration plateforme
            </div>
            <div className="truncate font-display text-[15px] font-semibold text-ink">{current}</div>
          </div>
          <div className="flex items-center gap-3">
            <div className="hidden text-right sm:block">
              <div className="truncate text-[13.5px] font-semibold leading-tight text-ink">
                {principal || "Administrateur"}
              </div>
              <div className="text-[11px] font-semibold uppercase tracking-wider text-brand">
                Administrateur
              </div>
            </div>
            <span className="grid h-9 w-9 flex-none place-items-center rounded-full bg-gradient-to-br from-ink to-[#132a5c] font-mono text-sm font-semibold text-white shadow-glow-sm">
              {(principal || "?").charAt(0).toUpperCase()}
            </span>
          </div>
        </header>

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

        <div className="flex-1 px-6 py-8 md:px-10">{children}</div>
      </div>
    </div>
  );
}
