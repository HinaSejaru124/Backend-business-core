"use client";

import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import Navbar from "./Navbar";
import Footer from "./Footer";

/**
 * Sépare les DEUX mondes de l'application :
 *  - Site vitrine (public) : navbar marketing + footer.
 *  - Espaces applicatifs plein écran (/console développeur, /admin plateforme) : aucune chrome
 *    marketing — chacun fournit son propre shell (sidebar + barre supérieure).
 */
export default function SiteChrome({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const isApp =
    pathname === "/console" || pathname.startsWith("/console/") ||
    pathname === "/admin" || pathname.startsWith("/admin/");

  if (isApp) return <>{children}</>;

  return (
    <>
      <Navbar />
      <main className="flex-1">{children}</main>
      <Footer />
    </>
  );
}
