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

const BRICKS: { icon: React.ComponentType<{ className?: string }>; nom: string; desc: string }[] = [
  { icon: IconLayers, nom: "Types métier", desc: "Le modèle, déclaré une fois, versionné par épinglage." },
  { icon: IconBraces, nom: "Offres", desc: "Unités de valeur : socle + capacités activables." },
  { icon: IconShield, nom: "Acteurs", desc: "Opérateurs & bénéficiaires, rôles métier étanches." },
  { icon: IconShield, nom: "Règles", desc: "Déclencheur → condition → effet, jamais codé." },
  { icon: IconBolt, nom: "Opérations", desc: "Workflows déclarés, exécutés avec compensation." },
  { icon: IconActivity, nom: "Transactions", desc: "Façade unifiée sur les échanges de valeur." },
  { icon: IconKey, nom: "Configuration", desc: "Les valeurs de réglage, à deux niveaux." },
];

function Eyebrow({ children }: { children: React.ReactNode }) {
  return (
    <span className="inline-flex items-center gap-2 border border-brand/25 bg-tint px-3 py-1 font-mono text-[11px] uppercase tracking-wider text-brand">
      {children}
    </span>
  );
}

export default function HomePage() {
  return (
    <>
      {/* ── Hero (décalé à gauche, fond « environnement de code ») ── */}
      <section className="relative overflow-hidden border-b border-line">
        <div className="code-photo absolute inset-0" aria-hidden />
        <div className="absolute inset-0 bg-[rgba(243,246,253,0.93)]" aria-hidden />
        <div className="dotgrid absolute inset-0 opacity-50" aria-hidden />
        <Container className="relative py-16 md:py-24">
          <div className="grid items-center gap-14 lg:grid-cols-[1.1fr_0.9fr]">
            <Reveal className="max-w-2xl">
              <Eyebrow>Business Core · API</Eyebrow>
              <h1 className="mt-6 font-display text-[clamp(42px,6.4vw,72px)] font-bold leading-[1.03] text-ink">
                Le cœur métier que vous n&apos;aurez plus à réécrire.
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
                  Lire la documentation
                </ButtonLink>
              </div>
              <div className="mt-10 flex flex-wrap gap-x-8 gap-y-3 font-mono text-[12px] text-muted">
                <span>▸ Adossé au Kernel</span>
                <span>▸ API REST réactive</span>
                <span>▸ Multi-tenant (RLS)</span>
                <span>▸ Erreurs RFC 7807</span>
              </div>
            </Reveal>

            <div className="flex flex-col gap-4">
              <Reveal delay={140}>
                <CodeWindow filename="declarer-type.json" lang="json" typeText={HERO_JSON} typeSpeed={12} />
              </Reveal>
              <Reveal delay={320}>
                <CodeWindow filename="terminal" lang="bash" typeText={HERO_TERMINAL} typeSpeed={9} />
              </Reveal>
            </div>
          </div>
        </Container>
      </section>

      {/* ── Bandeau de confiance ─────────────────────────────── */}
      <div className="border-b border-line bg-white">
        <Container className="grid grid-cols-2 md:grid-cols-4">
          {[
            { k: "7", v: "briques métier" },
            { k: "REST", v: "réactif (WebFlux)" },
            { k: "RLS", v: "isolation multi-tenant" },
            { k: "JWT", v: "auth déléguée au Kernel" },
          ].map((s, i) => (
            <Reveal
              key={s.v}
              delay={i * 90}
              className={cnBorder(i)}
            >
              <div className="px-2 py-8 text-center md:px-6">
                <div className="font-display text-3xl font-semibold text-ink">{s.k}</div>
                <div className="mt-1 text-sm text-muted">{s.v}</div>
              </div>
            </Reveal>
          ))}
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
              <div className="h-full border border-line bg-white p-6 transition-colors hover:border-line">
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
              <div className="h-full border-l-2 border-brand bg-white p-6 shadow-card transition-all hover:-translate-y-1 hover:shadow-pop">
                <div className="font-mono text-[12px] uppercase tracking-wider text-brand">Avec Business Core</div>
                <ul className="mt-4 space-y-3 text-[15px] text-body">
                  {[
                    "Un modèle générique en 7 briques, déclaré en données",
                    "Règles déclarées : déclencheur → condition → effet",
                    "Façade unifiée vers le Kernel (ventes, stock, caisse)",
                    "Isolation RLS et traçabilité intégrées",
                  ].map((t) => (
                    <li key={t} className="flex gap-3">
                      <span className="mt-2.5 h-px w-4 flex-none bg-brand" />
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
                  <span className="font-mono text-sm text-brand">{s.n}</span>
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

      {/* ── Le modèle en briques ─────────────────────────────── */}
      <Section alt>
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

          <div className="mt-12 grid gap-px border border-line bg-line sm:grid-cols-2 lg:grid-cols-4">
            {BRICKS.map((b, i) => {
              const Icon = b.icon;
              return (
                <Reveal key={b.nom} delay={i * 70}>
                  <div className="group h-full bg-white p-6 transition-colors hover:bg-tint">
                    <Icon className="h-5 w-5 text-brand transition-transform group-hover:scale-110" />
                    <h3 className="mt-4 font-display text-base font-semibold text-ink">{b.nom}</h3>
                    <p className="mt-1.5 text-sm leading-relaxed text-muted">{b.desc}</p>
                  </div>
                </Reveal>
              );
            })}
            <Reveal delay={490}>
              <div className="flex h-full flex-col justify-center bg-ink p-6 text-white">
                <IconTerminal className="h-5 w-5 text-brand" />
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
            <div className="relative overflow-hidden border border-ink bg-ink px-8 py-16 text-center">
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

/** Bordures de séparation du bandeau (verticales en desktop). */
function cnBorder(i: number): string {
  const base = "border-line";
  const left = i % 2 === 1 ? "border-l" : "";
  const mdLeft = i % 4 !== 0 ? "md:border-l" : "";
  const top = i >= 2 ? "border-t md:border-t-0" : "";
  return [base, left, mdLeft, top].filter(Boolean).join(" ");
}
