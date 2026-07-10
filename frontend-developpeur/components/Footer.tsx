import Link from "next/link";
import Container from "./Container";
import Logo from "./Logo";
import { API_BASE } from "@/lib/api";

type FooterLink = { label: string; href: string; external?: boolean };

const COLUMNS: { title: string; links: FooterLink[] }[] = [
  {
    title: "Produit",
    links: [
      { label: "Documentation", href: "/docs" },
      { label: "Tarifs", href: "/pricing" },
      { label: "Console", href: "/console" },
    ],
  },
  {
    title: "Développeurs",
    links: [
      { label: "Référence API", href: "/docs" },
      { label: "Swagger UI", href: `${API_BASE}/swagger-ui.html`, external: true },
      { label: "Audit", href: "/console/audit" },
    ],
  },
  {
    title: "Compte",
    links: [
      { label: "Se connecter", href: "/login" },
      { label: "Clé d'API", href: "/console/api-key" },
    ],
  },
];

export default function Footer() {
  return (
    <footer className="border-t border-line bg-white">
      <Container className="py-14">
        <div className="grid gap-10 md:grid-cols-[1.4fr_1fr_1fr_1fr]">
          <div className="max-w-xs">
            <Logo />
            <p className="mt-4 text-sm leading-relaxed text-muted">
              Le cœur métier générique, prêt à l&apos;emploi, au-dessus du Kernel. Déclarez votre métier en
              données — pas en code.
            </p>
          </div>
          {COLUMNS.map((col) => (
            <div key={col.title}>
              <h4 className="font-display text-[13px] font-semibold uppercase tracking-wider text-ink">
                {col.title}
              </h4>
              <ul className="mt-4 space-y-3">
                {col.links.map((l) => (
                  <li key={l.label}>
                    {l.external ? (
                      <a
                        href={l.href}
                        target="_blank"
                        rel="noreferrer"
                        className="text-sm text-muted transition-colors hover:text-brand"
                      >
                        {l.label}
                      </a>
                    ) : (
                      <Link href={l.href} className="text-sm text-muted transition-colors hover:text-brand">
                        {l.label}
                      </Link>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-12 flex flex-col items-start justify-between gap-3 border-t border-line pt-6 text-xs text-muted sm:flex-row sm:items-center">
          <span>© {new Date().getFullYear()} Business Core. Tous droits réservés.</span>
          <span className="font-mono">Adossé au Kernel RT-Comops · API REST · Multi-tenant</span>
        </div>
      </Container>
    </footer>
  );
}
