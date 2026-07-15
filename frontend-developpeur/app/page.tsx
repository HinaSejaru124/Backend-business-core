import Container from "@/components/Container";
import Section from "@/components/Section";
import CodeWindow from "@/components/CodeWindow";
import Reveal from "@/components/Reveal";
import { ButtonLink } from "@/components/Button";
import {
  IconArrowRight,
  IconLayers,
  IconBraces,
  IconShield,
  IconBolt,
  IconActivity,
  IconKey,
  IconTerminal,
  IconCheck,
  IconBook,
} from "@/components/icons";

const HERO_JSON = `POST /v1/business-types

{
  "code": "PHARMACIE",
  "nom": "Pharmacie",
  "businessDomainId": null
}`;

const HERO_TERMINAL = `$ curl -X POST $API/v1/auth/login \\
   -d '{"principal":"dev@ex.com","password":"••••"}'

→ 200 OK
{ "accessToken": "eyJhbGciOiJSUzI1Ni…",
  "owner": true }`;

const STEP_1 = `POST …/versions/1/offers

{
  "nom": "Doliprane 1000mg",
  "formePrix": "FIXE",
  "prix": 1500,
  "capacites": ["STOCKABLE"]
}`;

const STEP_2 = `POST /v1/businesses

{
  "typeId": "…",
  "versionNumber": 1,
  "nom": "Pharmacie du Centre"
}`;

const STEP_3 = `POST …/operations/vente:execute
Idempotency-Key: 6f2a-…

{ "parametres": { "offreId": "…", "quantite": 2 } }

→ 200 { "statut": "COMPLETEE" }`;

const AUDIT_EXAMPLE = `GET /v1/businesses/{id}/traces?limit=1

→ 200
{
  "operation": "Vendre",
  "statut": "COMPLETEE",
  "declencheur": "acteur:caissier-42",
  "duree_ms": 118
}`;

const BRICKS: { icon: React.ComponentType<{ className?: string }>; nom: string; desc: string }[] = [
  { icon: IconLayers, nom: "Types métier", desc: "Le modèle, déclaré une fois, versionné par épinglage." },
  { icon: IconBraces, nom: "Offres", desc: "Unités de valeur : socle + capacités activables." },
  { icon: IconShield, nom: "Acteurs", desc: "Opérateurs & bénéficiaires, rôles métier étanches." },
  { icon: IconShield, nom: "Règles", desc: "Déclencheur → condition → effet, jamais codé." },
  { icon: IconBolt, nom: "Opérations", desc: "Workflows déclarés, exécutés avec compensation." },
  { icon: IconActivity, nom: "Transactions", desc: "Façade unifiée sur les échanges de valeur." },
  { icon: IconKey, nom: "Configuration", desc: "Les valeurs de réglage, à deux niveaux." },
];

const CAPACITES_REELLES = [
  "Documentation interactive (Swagger / OpenAPI)",
  "Traces & audit de chaque opération exécutée",
  "Clé d'API en libre-service, révocable à tout moment",
  "Essai gratuit inclus, sans carte bancaire",
  "Isolation multi-tenant par Row-Level Security",
];

function Eyebrow({ children }: { children: React.ReactNode }) {
  return (
    <span className="inline-flex items-center gap-2 rounded-full border border-brand/25 bg-tint px-3.5 py-1.5 font-mono text-[11px] uppercase tracking-wider text-brand shadow-glow-sm">
      {children}
    </span>
  );
}

export default function HomePage() {
  return (
    <>
      {/* ── Hero ─────────────────────────────────────────────── */}
      <section className="relative overflow-hidden border-b border-line">
        <div className="code-photo absolute inset-0" aria-hidden />
        <div className="absolute inset-0 bg-[rgba(243,246,253,0.94)]" aria-hidden />
        <div className="dotgrid absolute inset-0 opacity-40" aria-hidden />
        <Container className="relative py-20 md:py-28">
          <div className="grid items-center gap-16 lg:grid-cols-[1.05fr_0.95fr]">
            <Reveal className="max-w-2xl">
              <Eyebrow>
                <IconBolt className="h-3 w-3" /> Business Core · Plateforme API
              </Eyebrow>
              <h1 className="mt-6 font-display text-[clamp(42px,6.4vw,72px)] font-bold leading-[1.03] text-ink">
                Le cœur métier que vous <span className="text-brand">n&apos;aurez plus</span> à réécrire.
              </h1>
              <p className="mt-6 max-w-xl text-[19px] leading-relaxed text-muted">
                Déclarez votre métier en données via une API REST — types, offres, règles, opérations.
                Business Core l&apos;interprète et l&apos;exécute au-dessus du Kernel. Vous ne recodez plus
                la même plomberie.
              </p>
              <div className="mt-9 flex flex-wrap items-center gap-3">
                <ButtonLink href="/login?tab=register">
                  Obtenir ma clé d&apos;API
                  <IconArrowRight className="h-4 w-4" />
                </ButtonLink>
                <ButtonLink href="/docs" variant="secondary">
                  <IconBook className="h-4 w-4" /> Lire la documentation
                </ButtonLink>
              </div>
              <div className="mt-10 flex flex-wrap gap-2.5">
                {["Adossé au Kernel", "API REST réactive", "Multi-tenant (RLS)", "Erreurs RFC 7807"].map((t) => (
                  <span
                    key={t}
                    className="inline-flex items-center gap-1.5 rounded-full border border-line bg-white px-3 py-1.5 text-[12.5px] text-body shadow-card"
                  >
                    <IconCheck className="h-3.5 w-3.5 text-brand" /> {t}
                  </span>
                ))}
              </div>
            </Reveal>

            <div className="relative">
              <div className="glow-orb absolute -inset-10 -z-10" aria-hidden />
              <Reveal delay={140}>
                <div className="tilt-code animate-float">
                  <CodeWindow filename="declarer-type.json" lang="json" typeText={HERO_JSON} typeSpeed={12} />
                </div>
              </Reveal>
              <Reveal delay={320}>
                <div className="tilt-code -mt-4 ml-8">
                  <CodeWindow filename="terminal" lang="bash" typeText={HERO_TERMINAL} typeSpeed={9} />
                </div>
              </Reveal>
            </div>
          </div>
        </Container>
      </section>

      {/* ── Bandeau de confiance ─────────────────────────────── */}
      <div className="border-b border-line bg-white py-10">
        <Container>
          <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
            {[
              { k: "7", v: "briques métier" },
              { k: "REST", v: "réactif (WebFlux)" },
              { k: "RLS", v: "isolation multi-tenant" },
              { k: "JWT", v: "auth déléguée au Kernel" },
            ].map((s, i) => (
              <Reveal key={s.v} delay={i * 90}>
                <div className="rounded-2xl border border-line bg-subtle px-4 py-7 text-center transition-all hover:-translate-y-1 hover:border-brand/30 hover:shadow-card">
                  <div className="font-display text-3xl font-semibold text-ink">{s.k}</div>
                  <div className="mt-1 text-sm text-muted">{s.v}</div>
                </div>
              </Reveal>
            ))}
          </div>
        </Container>
      </div>

      {/* ── Problème → Solution ──────────────────────────────── */}
      <Section alt>
        <Container>
          <Reveal className="max-w-2xl">
            <Eyebrow>Le problème</Eyebrow>
            <h2 className="mt-5 font-display text-[clamp(30px,4.2vw,44px)] font-bold text-ink">
              Arrêtez de recoder la même plomberie métier.
            </h2>
            <p className="mt-4 text-lg text-muted">
              Chaque nouvelle application refait les mêmes fondations : catalogue, rôles, contraintes,
              transactions. Business Core les fournit — vous déclarez, il exécute.
            </p>
          </Reveal>

          <div className="mt-12 grid gap-6 lg:grid-cols-2">
            <Reveal>
              <div className="h-full rounded-2xl border border-line bg-white p-6 shadow-card transition-all hover:-translate-y-1">
                <div className="font-mono text-[12px] uppercase tracking-wider text-muted">Sans Business Core</div>
                <ul className="mt-4 space-y-3 text-[15px] text-body">
                  {[
                    "Modèle de données réécrit à chaque projet",
                    "Règles métier codées en dur, dispersées",
                    "Intégration paiement / stock refaite à la main",
                    "Multi-tenant et audit bricolés",
                  ].map((t) => (
                    <li key={t} className="flex gap-3">
                      <span className="mt-2.5 h-px w-4 flex-none bg-danger" />
                      {t}
                    </li>
                  ))}
                </ul>
              </div>
            </Reveal>
            <Reveal delay={140}>
              <div className="h-full rounded-2xl border border-brand/20 bg-white p-6 shadow-glow-sm transition-all hover:-translate-y-1 hover:shadow-glow">
                <div className="font-mono text-[12px] uppercase tracking-wider text-brand">Avec Business Core</div>
                <ul className="mt-4 space-y-3 text-[15px] text-body">
                  {[
                    "Un modèle générique en 7 briques, déclaré en données",
                    "Règles déclarées : déclencheur → condition → effet",
                    "Façade unifiée vers le Kernel (ventes, stock, caisse)",
                    "Isolation RLS et traçabilité intégrées",
                  ].map((t) => (
                    <li key={t} className="flex gap-3">
                      <IconCheck className="mt-0.5 h-4 w-4 flex-none text-brand" />
                      {t}
                    </li>
                  ))}
                </ul>
              </div>
            </Reveal>
          </div>
        </Container>
      </Section>

      {/* ── Comment ça marche ────────────────────────────────── */}
      <Section>
        <Container>
          <Reveal className="max-w-2xl">
            <Eyebrow>Comment ça marche</Eyebrow>
            <h2 className="mt-5 font-display text-[clamp(30px,4.2vw,44px)] font-bold text-ink">
              Déclarez, instanciez, exécutez.
            </h2>
          </Reveal>
          <div className="mt-12 grid gap-6 lg:grid-cols-3">
            {[
              { n: "01", t: "Déclarez", d: "Le type métier, ses offres et ses règles — une fois.", code: STEP_1, f: "offre.json" },
              { n: "02", t: "Instanciez", d: "Créez vos entreprises sur une version épinglée.", code: STEP_2, f: "entreprise.json" },
              { n: "03", t: "Exécutez", d: "Lancez une opération : 200 immédiat ou 202 différé.", code: STEP_3, f: "vente.http" },
            ].map((s, i) => (
              <Reveal key={s.n} delay={i * 120} className="flex flex-col">
                <div className="flex items-baseline gap-3">
                  <span className="rounded-full bg-tint px-2.5 py-0.5 font-mono text-sm text-brand">{s.n}</span>
                  <h3 className="font-display text-xl font-semibold text-ink">{s.t}</h3>
                </div>
                <p className="mt-2 text-[15px] text-muted">{s.d}</p>
                <CodeWindow className="mt-5 flex-1" filename={s.f} lang="http" copyText={s.code}>
                  {s.code}
                </CodeWindow>
              </Reveal>
            ))}
          </div>
        </Container>
      </Section>

      {/* ── Expérience développeur ────────────────────────────── */}
      <Section alt className="!py-0">
        <div className="relative overflow-hidden bg-ink py-20 md:py-24">
          <div className="linegrid pointer-events-none absolute inset-0 opacity-20" aria-hidden />
          <Container className="relative">
            <div className="grid items-center gap-14 lg:grid-cols-[0.95fr_1.05fr]">
              <Reveal>
                <Eyebrow>Expérience développeur</Eyebrow>
                <h2 className="mt-5 font-display text-[clamp(28px,4vw,40px)] font-bold text-white">
                  Pensée pour aller vite, sans rien cacher.
                </h2>
                <p className="mt-4 text-[17px] leading-relaxed text-slate-300">
                  Tout ce qu&apos;il faut pour intégrer, tester et surveiller une intégration réelle —
                  rien de plus, rien d&apos;inventé.
                </p>
                <ul className="mt-7 space-y-3.5">
                  {CAPACITES_REELLES.map((c) => (
                    <li key={c} className="flex items-start gap-3 text-[15px] text-slate-200">
                      <span className="mt-0.5 grid h-5 w-5 flex-none place-items-center rounded-full bg-brand/20">
                        <IconCheck className="h-3 w-3 text-brand" />
                      </span>
                      {c}
                    </li>
                  ))}
                </ul>
                <ButtonLink
                  href="/docs"
                  variant="secondary"
                  className="mt-8 border-white/25 bg-transparent text-white hover:border-brand hover:bg-brand"
                >
                  Explorer la documentation
                </ButtonLink>
              </Reveal>

              <Reveal delay={160}>
                <div className="tilt-code">
                  <CodeWindow filename="audit.http" lang="http" copyText={AUDIT_EXAMPLE}>
                    {AUDIT_EXAMPLE}
                  </CodeWindow>
                </div>
              </Reveal>
            </div>
          </Container>
        </div>
      </Section>

      {/* ── Le modèle en briques ─────────────────────────────── */}
      <Section>
        <Container>
          <Reveal className="max-w-2xl">
            <Eyebrow>Le modèle</Eyebrow>
            <h2 className="mt-5 font-display text-[clamp(30px,4.2vw,44px)] font-bold text-ink">
              Sept briques, un cœur métier complet.
            </h2>
            <p className="mt-4 text-lg text-muted">
              Un modèle générique et cohérent, interprété par un moteur unique — le tout orchestré
              au-dessus du Kernel.
            </p>
          </Reveal>

          <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {BRICKS.map((b, i) => {
              const Icon = b.icon;
              return (
                <Reveal key={b.nom} delay={i * 70}>
                  <div className="group h-full rounded-2xl border border-line bg-white p-6 shadow-card transition-all hover:-translate-y-1.5 hover:border-brand/30 hover:shadow-glow-sm">
                    <span className="grid h-10 w-10 place-items-center rounded-xl bg-tint transition-colors group-hover:bg-brand">
                      <Icon className="h-5 w-5 text-brand transition-colors group-hover:text-white" />
                    </span>
                    <h3 className="mt-4 font-display text-base font-semibold text-ink">{b.nom}</h3>
                    <p className="mt-1.5 text-sm leading-relaxed text-muted">{b.desc}</p>
                  </div>
                </Reveal>
              );
            })}
            <Reveal delay={490}>
              <div className="flex h-full flex-col justify-center rounded-2xl bg-ink p-6 text-white shadow-glow">
                <span className="grid h-10 w-10 place-items-center rounded-xl bg-brand/20">
                  <IconTerminal className="h-5 w-5 text-brand" />
                </span>
                <h3 className="mt-4 font-display text-base font-semibold">Le Kernel</h3>
                <p className="mt-1.5 text-sm leading-relaxed text-slate-400">
                  Le noyau générique. Business Core l&apos;orchestre en façade.
                </p>
              </div>
            </Reveal>
          </div>
        </Container>
      </Section>

      {/* ── CTA final ────────────────────────────────────────── */}
      <Section className="!py-20">
        <Container>
          <Reveal>
            <div className="relative overflow-hidden rounded-3xl border border-ink bg-ink px-8 py-16 text-center shadow-glow">
              <div className="code-photo absolute inset-0 opacity-20" aria-hidden />
              <div className="linegrid pointer-events-none absolute inset-0 opacity-30" aria-hidden />
              <div className="relative mx-auto max-w-2xl">
                <h2 className="font-display text-[clamp(28px,4.4vw,44px)] font-bold text-white">
                  Construisez votre métier, pas votre plomberie.
                </h2>
                <p className="mt-4 text-lg text-slate-300">
                  Récupérez votre clé d&apos;API et lancez votre première opération en quelques minutes.
                </p>
                <div className="mt-8 flex flex-wrap justify-center gap-3">
                  <ButtonLink href="/login?tab=register">
                    Commencer
                    <IconArrowRight className="h-4 w-4" />
                  </ButtonLink>
                  <ButtonLink
                    href="/docs"
                    variant="secondary"
                    className="border-white/25 bg-transparent text-white hover:border-brand hover:bg-brand hover:text-white"
                  >
                    Explorer l&apos;API
                  </ButtonLink>
                </div>
              </div>
            </div>
          </Reveal>
        </Container>
      </Section>
    </>
  );
}
