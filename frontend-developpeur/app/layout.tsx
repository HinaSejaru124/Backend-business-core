import type { Metadata } from "next";
import { Plus_Jakarta_Sans, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import SiteChrome from "@/components/SiteChrome";
import { AuthProvider } from "@/lib/auth-context";

// Une SEULE famille pour toute l'UI (titres + texte) : Plus Jakarta Sans, ronde et lisible,
// comme la maquette. La hiérarchie se fait au poids/à la couleur, pas en changeant de police.
const jakarta = Plus_Jakarta_Sans({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700", "800"],
  variable: "--font-sans",
  display: "swap",
});
// Monospace RÉSERVÉE au code et aux identifiants techniques — jamais aux titres/labels d'UI.
const mono = JetBrains_Mono({
  subsets: ["latin"],
  weight: ["400", "500"],
  variable: "--font-mono",
  display: "swap",
});

export const metadata: Metadata = {
  title: "Business Core — Le cœur métier prêt à l'emploi",
  description:
    "L'API qui donne aux développeurs un cœur métier générique, au-dessus du Kernel. Déclarez votre métier en données, pas en code.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="fr" className={`${jakarta.variable} ${mono.variable}`}>
      <body className="flex min-h-screen flex-col bg-white font-sans text-body antialiased">
        <AuthProvider>
          <SiteChrome>{children}</SiteChrome>
        </AuthProvider>
      </body>
    </html>
  );
}
