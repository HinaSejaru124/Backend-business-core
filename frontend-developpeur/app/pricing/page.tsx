import Container from "@/components/Container";
import PageHeader from "@/components/PageHeader";
import Reveal from "@/components/Reveal";
import { ButtonLink } from "@/components/Button";
import { IconCheck } from "@/components/icons";
import { cn } from "@/lib/cn";

type Plan = {
  code: string;
  prix: string;
  tagline: string;
  features: string[];
  highlight?: boolean;
  cta: string;
};

const PLANS: Plan[] = [
  {
    code: "FREE",
    prix: "0",
    tagline: "Pour démarrer et prototyper.",
    features: ["1 tenant", "Bac à sable illimité", "Documentation & Swagger", "Support communautaire"],
    cta: "Commencer",
  },
  {
    code: "PRO",
    prix: "Sur mesure",
    tagline: "Pour les applications en production.",
    features: ["Quotas étendus", "Multi-organisation", "Audit & traces", "Support prioritaire"],
    highlight: true,
    cta: "Choisir Pro",
  },
  {
    code: "ENTERPRISE",
    prix: "Sur devis",
    tagline: "Pour les besoins à grande échelle.",
    features: ["Quotas dédiés", "SLA & sécurité renforcée", "Accompagnement dédié", "Intégrations sur mesure"],
    cta: "Nous contacter",
  },
];

export default function PricingPage() {
  return (
    <>
      <PageHeader
        eyebrow="Tarifs"
        title="Un plan pour chaque étape."
        description="Commencez gratuitement, passez à l'échelle quand vous êtes prêt. Le plan choisi accompagne votre clé d'API."
      />
      <Container className="py-14">
        <div className="grid gap-6 lg:grid-cols-3">
          {PLANS.map((p, i) => (
            <Reveal key={p.code} delay={i * 100} className="h-full">
              <div
                className={cn(
                  "group flex h-full flex-col border border-line bg-white p-8 transition-all duration-200 hover:-translate-y-1.5 hover:border-brand hover:shadow-pop",
                  p.highlight && "border-t-2 border-t-brand shadow-card"
                )}
              >
                <div className="flex items-center justify-between">
                  <h3 className="font-display text-lg font-semibold text-ink transition-colors group-hover:text-brand">
                    {p.code}
                  </h3>
                  {p.highlight && (
                    <span className="border border-brand/30 bg-brand/5 px-2 py-0.5 font-mono text-[11px] uppercase tracking-wider text-brand">
                      Recommandé
                    </span>
                  )}
                </div>
                <p className="mt-2 text-sm text-muted">{p.tagline}</p>
                <div className="mt-6 font-display text-4xl font-bold text-ink">
                  {p.prix === "0" ? (
                    <>
                      0<span className="ml-1 text-base font-medium text-muted">€ / mois</span>
                    </>
                  ) : (
                    <span className="text-2xl">{p.prix}</span>
                  )}
                </div>
                <ul className="mt-6 flex-1 space-y-3">
                  {p.features.map((f) => (
                    <li key={f} className="flex gap-2.5 text-sm text-body">
                      <IconCheck className="mt-0.5 h-4 w-4 flex-none text-brand" />
                      {f}
                    </li>
                  ))}
                </ul>
                {/* Le plan choisi accompagne la création de compte + clé (cohérence Tarifs → inscription). */}
                <ButtonLink
                  href={`/login?tab=register&plan=${p.code}`}
                  variant={p.highlight ? "primary" : "secondary"}
                  className="mt-8 w-full"
                >
                  {p.cta}
                </ButtonLink>
              </div>
            </Reveal>
          ))}
        </div>
        <p className="mt-6 text-center text-xs text-muted">
          Les tarifs exacts seront précisés par l&apos;équipe. Le plan alimente le champ{" "}
          <code className="bg-subtle px-1 font-mono text-ink">planCode</code> lors de la création du compte
          et de la clé d&apos;API.
        </p>
      </Container>
    </>
  );
}
