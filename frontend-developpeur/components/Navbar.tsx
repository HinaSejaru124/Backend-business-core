"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import Container from "./Container";
import Logo from "./Logo";
import { ButtonLink } from "./Button";
import { IconMenu, IconClose } from "./icons";
import { cn } from "@/lib/cn";

/**
 * Navbar du SITE VITRINE (public uniquement).
 * L'espace connecté (/console) a son propre shell — cette navbar n'y apparaît jamais
 * (cf. components/SiteChrome). Elle présente toujours la même face publique :
 * « Se connecter » / « Créer un compte ». Un visiteur déjà authentifié qui clique
 * « Se connecter » est redirigé vers sa console par la page /login.
 */
const LINKS = [
  { href: "/", label: "Accueil" },
  { href: "/docs", label: "Documentation" },
  { href: "/pricing", label: "Tarifs" },
];

export default function Navbar() {
  const [open, setOpen] = useState(false);
  const pathname = usePathname();
  const active = (href: string) => (href === "/" ? pathname === "/" : pathname.startsWith(href));

  return (
    <header className="sticky top-0 z-50 border-b border-line bg-white/80 backdrop-blur-md">
      <Container>
        <div className="flex h-16 items-center justify-between gap-4">
          <Logo />

          <nav className="hidden items-center gap-9 md:flex">
            {LINKS.map((l) => (
              <Link
                key={l.href}
                href={l.href}
                data-active={active(l.href)}
                className={cn("navlink text-[15px]", active(l.href) ? "text-ink" : "text-muted")}
              >
                {l.label}
              </Link>
            ))}
          </nav>

          <div className="hidden items-center gap-5 md:flex">
            <Link
              href="/login?tab=login"
              className={cn(
                "navlink text-[15px] font-medium",
                active("/login") ? "text-ink" : "text-ink/80"
              )}
            >
              Se connecter
            </Link>
            <ButtonLink href="/login?tab=register" size="sm">
              Créer un compte
            </ButtonLink>
          </div>

          <button
            onClick={() => setOpen((o) => !o)}
            className="grid h-10 w-10 place-items-center border border-line text-ink transition-colors hover:bg-tint active:translate-y-px md:hidden"
            aria-label="Menu"
            aria-expanded={open}
          >
            {open ? <IconClose className="h-5 w-5" /> : <IconMenu className="h-5 w-5" />}
          </button>
        </div>
      </Container>

      {open && (
        <div className="animate-fade-up border-t border-line bg-white md:hidden">
          <Container>
            <nav className="flex flex-col py-2">
              {LINKS.map((l) => (
                <Link
                  key={l.href}
                  href={l.href}
                  onClick={() => setOpen(false)}
                  className={cn(
                    "border-b border-line py-3 text-[15px] transition-colors",
                    active(l.href)
                      ? "border-l-2 border-l-brand pl-3 text-ink"
                      : "text-muted hover:text-ink"
                  )}
                >
                  {l.label}
                </Link>
              ))}
              <div className="flex items-center gap-3 py-4">
                <ButtonLink
                  href="/login?tab=login"
                  variant="secondary"
                  size="sm"
                  className="flex-1"
                  onClick={() => setOpen(false)}
                >
                  Se connecter
                </ButtonLink>
                <ButtonLink
                  href="/login?tab=register"
                  size="sm"
                  className="flex-1"
                  onClick={() => setOpen(false)}
                >
                  Créer un compte
                </ButtonLink>
              </div>
            </nav>
          </Container>
        </div>
      )}
    </header>
  );
}
