import Container from "@/components/Container";
import CodeWindow from "@/components/CodeWindow";
import HttpBadge from "@/components/HttpBadge";
import DocsNav from "@/components/DocsNav";
import { ENDPOINTS } from "@/lib/endpoints";
import { cn } from "@/lib/cn";

const CODE_KEY = `# 1. Créer votre compte développeur et obtenir votre clé d'API (une seule fois)
curl -X POST $API/v1/registration \\
  -H "Content-Type: application/json" \\
  -d '{ "firstName": "Miguel", "lastName": "Techlan",
        "email": "dev@exemple.com", "password": "••••••",
        "planCode": "FREE" }'

# → { "clientId": "bck_...", "apiKey": "...", "plan": "FREE" }`;

const CODE_LOGIN = `# 2. Vérifiez votre e-mail (lien envoyé par le Kernel), puis connectez-vous
curl -X POST $API/v1/auth/login \\
  -H "Content-Type: application/json" \\
  -d '{ "principal": "dev@exemple.com", "password": "••••••" }'

# → { "accessToken": "eyJ...", "expiresInSeconds": 900, "owner": true }`;

const CODE_BEARER = `curl $API/v1/business-types \\
  -H "Authorization: Bearer <accessToken>"`;

const CODE_ERROR = `HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Règle métier violée",
  "status": 422,
  "detail": "Un document ordonnance est requis pour cette vente.",
  "violatedRule": "ORDONNANCE_REQUISE"
}`;

export default function DocsPage() {
  return (
    <Container className="grid gap-12 py-12 md:py-16 lg:grid-cols-[220px_1fr]">
      {/* Sidebar (scroll-spy) */}
      <aside className="hidden lg:block">
        <DocsNav />
      </aside>

      {/* Contenu */}
      <div className="max-w-3xl">
        <section id="demarrage" className="scroll-mt-24">
          <h1 className="font-display text-3xl font-bold text-ink">Documentation</h1>
          <p className="mt-4 text-lg text-muted">
            Business Core expose un modèle métier piloté par les données. Déclarez vos types, offres et
            règles, puis exécutez vos opérations — le tout au-dessus du Kernel.
          </p>

          <h2 className="mt-12 font-display text-xl font-semibold text-ink">Démarrage rapide</h2>
          <p className="mt-3 text-sm leading-relaxed text-body">
            Récupérez votre clé d&apos;API, puis effectuez votre premier appel. La base d&apos;URL est
            configurable via <code className="bg-subtle px-1 font-mono text-[13px] text-ink">NEXT_PUBLIC_API_BASE_URL</code>.
          </p>
          <CodeWindow className="mt-5" filename="obtenir-cle.sh" lang="bash" copyText={CODE_KEY}>
            {CODE_KEY}
          </CodeWindow>
        </section>

        <section id="authentification" className="mt-14 scroll-mt-24">
          <h2 className="font-display text-xl font-semibold text-ink">Authentification</h2>
          <p className="mt-3 text-sm leading-relaxed text-body">
            La connexion est <strong>déléguée au Kernel</strong>. Vous obtenez un JWT que vous rejouez en
            en-tête <code className="bg-subtle px-1 font-mono text-[13px] text-ink">Authorization: Bearer</code> sur
            les appels protégés. Le token expire (~15 min) ; reconnectez-vous alors.
          </p>
          <CodeWindow className="mt-5" filename="login.sh" lang="bash" copyText={CODE_LOGIN}>
            {CODE_LOGIN}
          </CodeWindow>
          <CodeWindow className="mt-4" filename="appel-protege.sh" lang="bash" copyText={CODE_BEARER}>
            {CODE_BEARER}
          </CodeWindow>
        </section>

        <section id="reference" className="mt-14 scroll-mt-24">
          <h2 className="font-display text-xl font-semibold text-ink">Référence API</h2>
          <p className="mt-3 text-sm leading-relaxed text-body">
            Les endpoints exposés par le backend. <span className="text-muted">bearer</span> = requiert un
            JWT.
          </p>
          <div className="mt-6 space-y-8">
            {ENDPOINTS.map((group) => (
              <div key={group.group}>
                <h3 className="font-mono text-[12px] uppercase tracking-wider text-muted">{group.group}</h3>
                <div className="mt-3 border border-line">
                  {group.items.map((ep, i) => (
                    <div
                      key={ep.method + ep.path}
                      className={cn(
                        "flex flex-col gap-1.5 px-4 py-3 sm:flex-row sm:items-center sm:gap-4",
                        i !== 0 && "border-t border-line"
                      )}
                    >
                      <HttpBadge method={ep.method} />
                      <code className="font-mono text-[13px] text-ink">{ep.path}</code>
                      <span className="text-sm text-muted sm:ml-auto sm:text-right">{ep.usage}</span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </section>

        <section id="erreurs" className="mt-14 scroll-mt-24">
          <h2 className="font-display text-xl font-semibold text-ink">Format d&apos;erreur</h2>
          <p className="mt-3 text-sm leading-relaxed text-body">
            Les erreurs suivent la norme <strong>RFC 7807</strong>{" "}
            (<code className="bg-subtle px-1 font-mono text-[13px] text-ink">application/problem+json</code>), enrichies
            de champs métier (<code className="bg-subtle px-1 font-mono text-[13px] text-ink">violatedRule</code>…).
          </p>
          <CodeWindow className="mt-5" filename="erreur.http" lang="http" copyText={CODE_ERROR}>
            {CODE_ERROR}
          </CodeWindow>
        </section>
      </div>
    </Container>
  );
}
