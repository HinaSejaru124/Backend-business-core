"use client";

import CodeWindow from "@/components/CodeWindow";
import HttpBadge from "@/components/HttpBadge";
import DocsNav from "@/components/DocsNav";
import { ENDPOINTS } from "@/lib/endpoints";
import { cn } from "@/lib/cn";

const CODE_BEARER = `curl $API/v1/business-types \\
  -H "X-BC-Client-Id: <developerId>" \\
  -H "X-BC-Api-Key: <apiKey>"`;

const CODE_ERROR = `HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Règle métier violée",
  "status": 422,
  "detail": "Un document ordonnance est requis pour cette vente.",
  "violatedRule": "ORDONNANCE_REQUISE"
}`;

export default function ConsoleDocsPage() {
    return (
        <div className="animate-fade-up grid gap-12 py-4 lg:grid-cols-[220px_1fr]">
            {/* Sidebar (scroll-spy) */}
            <aside className="hidden lg:block">
                <DocsNav />
            </aside>

            {/* Contenu */}
            <div className="max-w-3xl">
                <section id="demarrage" className="scroll-mt-24">
                    <div className="font-mono text-[12px] uppercase tracking-wider text-brand">Ressources</div>
                    <h1 className="mt-2 font-display text-3xl font-bold text-ink">Documentation de l&apos;API</h1>
                    <p className="mt-4 text-base text-muted">
                        Business Core expose un modèle métier piloté par les données. Déclarez vos types, offres et
                        règles, puis exécutez vos opérations — le tout au-dessus du Kernel.
                    </p>

                    <h2 className="mt-12 font-display text-xl font-semibold text-ink">Démarrage rapide</h2>
                    <p className="mt-3 text-sm leading-relaxed text-body">
                        Après inscription et connexion, créez une entreprise puis générez sa clé API dans{" "}
                        <span className="font-medium text-ink">Clés d&apos;API</span>. Utilisez votre{" "}
                        <code className="font-mono text-[13px]">developerId</code> (GET /v1/auth/me) comme{" "}
                        <code className="font-mono text-[13px]">X-BC-Client-Id</code>.
                    </p>
                    <CodeWindow className="mt-5" filename="appel-api.sh" lang="bash" copyText={CODE_BEARER}>
                        {CODE_BEARER}
                    </CodeWindow>
                </section>

                <section id="authentification" className="mt-14 scroll-mt-24">
                    <h2 className="font-display text-xl font-semibold text-ink">En-têtes d&apos;authentification</h2>
                    <p className="mt-3 text-sm leading-relaxed text-body">
                        Chaque appel à l&apos;API Business Core doit inclure deux en-têtes obligatoires :
                    </p>
                    <ul className="mt-3 space-y-2 text-sm text-body pl-5 list-disc">
                        <li><code className="bg-subtle px-1 font-mono text-[13px] text-ink">X-BC-Client-Id</code> : Votre identifiant développeur stable (<code className="font-mono text-[13px]">developerId</code> via GET /v1/auth/me).</li>
                        <li><code className="bg-subtle px-1 font-mono text-[13px] text-ink">X-BC-Api-Key</code> : Le secret de la clé API d&apos;une entreprise (généré via POST /v1/businesses/&#123;id&#125;/api-keys).</li>
                    </ul>
                    <p className="mt-3 text-sm text-muted">
                        La console utilise un JWT (<code className="font-mono text-[13px]">Authorization: Bearer</code>) ;
                        les applications externes utilisent les en-têtes ci-dessus.
                    </p>
                </section>

                <section id="reference" className="mt-14 scroll-mt-24">
                    <h2 className="font-display text-xl font-semibold text-ink">Référence API</h2>
                    <p className="mt-3 text-sm leading-relaxed text-body">
                        Les endpoints exposés par le backend Business Core.
                    </p>
                    <div className="mt-6 space-y-8">
                        {ENDPOINTS.map((group) => (
                            <div key={group.group}>
                                <h3 className="font-mono text-[12px] uppercase tracking-wider text-muted">{group.group}</h3>
                                <div className="mt-3 border border-line bg-white">
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
        </div>
    );
}
