"use client";

import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import Navbar from "./Navbar";
import Footer from "./Footer";

/**
 * Sépare les DEUX mondes de l'application :
 *  - Site vitrine (public) : navbar marketing + footer.
 *  - Espace développeur (/console) : aucune chrome marketing — la console fournit
 *    son propre shell plein écran (sidebar + barre supérieure). C'est ce qui évite
 *    d'avoir le dashboard « coincé » au centre sous la navbar de connexion.
 */
export default function SiteChrome({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const isApp = pathname === "/console" || pathname.startsWith("/console/");

  if (isApp) return <>{children}</>;

  return (
    <>
      <Navbar />
      <main className="flex-1">{children}</main>
      <Footer />
    </>
  );
}
