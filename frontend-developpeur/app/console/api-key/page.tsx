"use client";

import { useCallback, useEffect, useState } from "react";
import CodeWindow from "@/components/CodeWindow";
import Field from "@/components/Field";
import { Button } from "@/components/Button";
import {
  API_BASE,
  ApiError,
  createApiKey,
  listApiKeys,
  renameApiKey,
  revokeApiKey,
  type ApiKey,
  type ApiKeyCreated,
} from "@/lib/api";
import { IconCopy, IconCheck, IconEye, IconEyeOff, IconClose } from "@/components/icons";
import { cn } from "@/lib/cn";

const MAX_CLES = 5;

/** Copie un texte dans le presse-papier et bascule un drapeau « copié » 1,4 s. */
function useCopie(): [string | null, (texte: string, id: string) => void] {
  const [copie, setCopie] = useState<string | null>(null);
  const copier = useCallback((texte: string, id: string) => {
    navigator.clipboard
      .writeText(texte)
      .then(() => {
        setCopie(id);
        setTimeout(() => setCopie((c) => (c === id ? null : c)), 1400);
      })
      .catch(() => {});
  }, []);
  return [copie, copier];
}

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" });
}

export default function ApiKeyPage() {
  const [cles, setCles] = useState<ApiKey[]>([]);
  const [etat, setEtat] = useState<"loading" | "error" | "ok">("loading");
  const [erreur, setErreur] = useState<string | null>(null);

  // Création
  const [formOuvert, setFormOuvert] = useState(false);
  const [nouveauNom, setNouveauNom] = useState("");
  const [creation, setCreation] = useState(false);
  const [creee, setCreee] = useState<ApiKeyCreated | null>(null); // secret affiché une seule fois

  // Renommage inline
  const [editionId, setEditionId] = useState<string | null>(null);
  const [editionNom, setEditionNom] = useState("");

  // Révocation (confirmation inline)
  const [confirmRevoke, setConfirmRevoke] = useState<string | null>(null);
  const [action, setAction] = useState(false);

  const [copie, copier] = useCopie();
  const [revelerCree, setRevelerCree] = useState(true);

  const charger = useCallback(async () => {
    setEtat("loading");
    try {
      const data = await listApiKeys();
      setCles(data);
      setEtat("ok");
    } catch (e) {
      setErreur(e instanceof ApiError ? e.detail || e.title : "Chargement impossible.");
      setEtat("error");
    }
  }, []);

  useEffect(() => {
    void charger();
  }, [charger]);

  const actives = cles.length; // le backend ne renvoie que les clés ACTIVE
  const limiteAtteinte = actives >= MAX_CLES;

  async function onCreer() {
    if (creation) return;
    setErreur(null);
    setCreation(true);
    try {
      const c = await createApiKey(nouveauNom);
      setCreee(c);
      setRevelerCree(true);
      setNouveauNom("");
      setFormOuvert(false);
      await charger();
    } catch (e) {
      setErreur(e instanceof ApiError ? e.detail || e.title : "Création impossible.");
    } finally {
      setCreation(false);
    }
  }

  async function onRenommer(id: string) {
    const nom = editionNom.trim();
    if (!nom) {
      setEditionId(null);
      return;
    }
    setAction(true);
    setErreur(null);
    try {
      await renameApiKey(id, nom);
      setEditionId(null);
      await charger();
    } catch (e) {
      setErreur(e instanceof ApiError ? e.detail || e.title : "Renommage impossible.");
    } finally {
      setAction(false);
    }
  }

  async function onRevoquer(id: string) {
    setAction(true);
    setErreur(null);
    try {
      await revokeApiKey(id);
      setConfirmRevoke(null);
      await charger();
    } catch (e) {
      setErreur(e instanceof ApiError ? e.detail || e.title : "Révocation impossible.");
    } finally {
      setAction(false);
    }
  }

  // Exemple d'utilisation : basé sur une clé réelle (la dernière créée, sinon la première active).
  const clientIdExemple = creee?.clientId || cles[0]?.prefix || "bck_xxxxxxxx";
  const secretExemple = creee && revelerCree ? creee.apiKey : "••••••••••••••••";
  const usage = `curl ${API_BASE}/v1/business-types \\
  -H "X-BC-Client-Id: ${clientIdExemple}" \\
  -H "X-BC-Api-Key: ${secretExemple}"`;

  return (
    <div className="animate-fade-up">
      {/* En-tête */}
      <div className="border-b border-line pb-6">
        <div className="font-mono text-[12px] uppercase tracking-wider text-brand">Accès</div>
        <h1 className="mt-2 font-display text-3xl font-bold text-ink">Clés d&apos;API</h1>
        <p className="mt-1 text-sm text-muted">
          Créez jusqu&apos;à {MAX_CLES} clés pour authentifier vos applications auprès de Business Core.
          Chaque clé est propre à votre compte et révocable à tout moment.
        </p>
      </div>

      {/* Bandeau secret — affiché UNE SEULE FOIS après création */}
      {creee && (
        <div className="mt-6 border-2 border-brand bg-tint p-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h2 className="font-display text-lg font-bold text-ink">
                Clé « {creee.name} » créée
              </h2>
              <p className="mt-1 text-sm text-ink">
                Copiez le secret maintenant : <strong>il ne sera plus jamais affiché</strong> après
                la fermeture de ce panneau. Le backend n&apos;en conserve qu&apos;une empreinte
                chiffrée.
              </p>
            </div>
            <button
              type="button"
              onClick={() => setCreee(null)}
              className="grid h-8 w-8 flex-none place-items-center border border-line bg-white text-muted hover:text-ink"
              aria-label="Fermer"
            >
              <IconClose className="h-4 w-4" />
            </button>
          </div>

          <dl className="mt-5 space-y-4">
            <div>
              <dt className="font-mono text-xs uppercase tracking-wider text-muted">
                Client-Id (X-BC-Client-Id)
              </dt>
              <dd className="mt-1 flex items-center justify-between gap-2 border border-line bg-white p-2.5">
                <code className="truncate font-mono text-sm text-ink select-all">{creee.clientId}</code>
                <button
                  type="button"
                  onClick={() => copier(creee.clientId, "cid")}
                  className="flex-none text-xs font-medium text-brand hover:underline"
                >
                  {copie === "cid" ? "Copié !" : "Copier"}
                </button>
              </dd>
            </div>
            <div>
              <dt className="font-mono text-xs uppercase tracking-wider text-muted">
                Clé secrète (X-BC-Api-Key)
              </dt>
              <dd className="mt-1 flex items-stretch gap-2">
                <div className="flex min-w-0 flex-1 items-center justify-between border border-line bg-white p-2.5">
                  <code className="truncate font-mono text-sm text-ink select-all">
                    {revelerCree ? creee.apiKey : "•".repeat(Math.min(40, creee.apiKey.length))}
                  </code>
                  <button
                    type="button"
                    onClick={() => copier(creee.apiKey, "sec")}
                    className="flex-none pl-2 text-xs font-medium text-brand hover:underline"
                  >
                    {copie === "sec" ? "Copié !" : "Copier"}
                  </button>
                </div>
                <button
                  type="button"
                  onClick={() => setRevelerCree((r) => !r)}
                  className="grid w-11 place-items-center border border-line bg-white text-muted hover:text-ink"
                  aria-label={revelerCree ? "Masquer" : "Révéler"}
                >
                  {revelerCree ? <IconEyeOff className="h-4 w-4" /> : <IconEye className="h-4 w-4" />}
                </button>
              </dd>
            </div>
          </dl>

          <Button size="sm" className="mt-5" onClick={() => setCreee(null)}>
            <IconCheck className="h-4 w-4" /> J&apos;ai copié ma clé
          </Button>
        </div>
      )}

      {erreur && (
        <div className="mt-6 border-l-2 border-danger bg-danger/5 px-4 py-3 text-sm text-danger">
          {erreur}
        </div>
      )}

      {/* Liste des clés */}
      <section className="mt-8">
        <div className="flex flex-col justify-between gap-3 border-b border-line pb-4 sm:flex-row sm:items-center">
          <div className="flex items-baseline gap-3">
            <h2 className="font-display text-lg font-semibold text-ink">Vos clés</h2>
            <span className="font-mono text-[12px] text-muted">
              {etat === "ok" ? `${actives} / ${MAX_CLES} actives` : ""}
            </span>
          </div>
          <Button
            size="sm"
            onClick={() => setFormOuvert((o) => !o)}
            disabled={limiteAtteinte || formOuvert}
            title={limiteAtteinte ? `Limite de ${MAX_CLES} clés atteinte` : undefined}
          >
            + Nouvelle clé
          </Button>
        </div>

        {limiteAtteinte && (
          <p className="mt-3 border-l-2 border-brand bg-tint px-4 py-2.5 text-xs text-ink">
            Vous avez atteint la limite de {MAX_CLES} clés actives. Révoquez une clé existante pour en
            créer une nouvelle.
          </p>
        )}

        {/* Formulaire de création */}
        {formOuvert && !limiteAtteinte && (
          <div className="mt-4 border border-line bg-white p-5">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
              <div className="flex-1">
                <Field
                  label="Nom de la clé (optionnel)"
                  id="nom-cle"
                  placeholder="ex. Production, PharmaCore, Staging…"
                  value={nouveauNom}
                  maxLength={64}
                  onChange={(e) => setNouveauNom(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") void onCreer();
                  }}
                  hint="Un libellé pour reconnaître à quoi sert cette clé. Par défaut : « Default »."
                />
              </div>
              <div className="flex gap-2">
                <Button size="md" onClick={() => void onCreer()} disabled={creation}>
                  {creation ? "Création…" : "Créer la clé"}
                </Button>
                <Button
                  size="md"
                  variant="secondary"
                  onClick={() => {
                    setFormOuvert(false);
                    setNouveauNom("");
                  }}
                  disabled={creation}
                >
                  Annuler
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* Tableau */}
        <div className="mt-4 border border-line bg-white">
          {etat === "loading" && <div className="p-5 text-sm text-muted">Chargement…</div>}
          {etat === "error" && (
            <div className="p-5 text-sm text-danger">Impossible de charger vos clés.</div>
          )}
          {etat === "ok" && cles.length === 0 && (
            <div className="p-6 text-center text-sm text-muted">
              Vous n&apos;avez aucune clé active. Créez-en une avec « Nouvelle clé ».
            </div>
          )}
          {etat === "ok" &&
            cles.map((cle, i) => (
              <div
                key={cle.id}
                className={cn(
                  "flex flex-col gap-3 p-5 sm:flex-row sm:items-center",
                  i !== 0 && "border-t border-line"
                )}
              >
                {/* Nom + prefix */}
                <div className="min-w-0 flex-1">
                  {editionId === cle.id ? (
                    <div className="flex items-center gap-2">
                      <input
                        autoFocus
                        value={editionNom}
                        maxLength={64}
                        onChange={(e) => setEditionNom(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter") void onRenommer(cle.id);
                          if (e.key === "Escape") setEditionId(null);
                        }}
                        className="h-9 w-48 border border-line bg-white px-2.5 text-sm text-ink outline-none focus:border-brand"
                      />
                      <button
                        type="button"
                        onClick={() => void onRenommer(cle.id)}
                        disabled={action}
                        className="text-xs font-medium text-brand hover:underline"
                      >
                        Enregistrer
                      </button>
                      <button
                        type="button"
                        onClick={() => setEditionId(null)}
                        className="text-xs text-muted hover:text-ink"
                      >
                        Annuler
                      </button>
                    </div>
                  ) : (
                    <div className="flex items-center gap-2">
                      <span className="truncate font-display text-[15px] font-semibold text-ink">
                        {cle.name}
                      </span>
                      <button
                        type="button"
                        onClick={() => {
                          setEditionId(cle.id);
                          setEditionNom(cle.name);
                        }}
                        className="text-xs text-muted hover:text-brand"
                      >
                        Renommer
                      </button>
                    </div>
                  )}
                  <div className="mt-1.5 flex items-center gap-2">
                    <code className="truncate font-mono text-[13px] text-muted">{cle.prefix}</code>
                    <button
                      type="button"
                      onClick={() => copier(cle.prefix, `p-${cle.id}`)}
                      className="flex-none text-muted hover:text-brand"
                      aria-label="Copier le Client-Id"
                    >
                      {copie === `p-${cle.id}` ? (
                        <IconCheck className="h-3.5 w-3.5 text-ok" />
                      ) : (
                        <IconCopy className="h-3.5 w-3.5" />
                      )}
                    </button>
                  </div>
                </div>

                {/* Dates */}
                <div className="flex flex-none gap-6 sm:gap-8">
                  <div>
                    <div className="font-mono text-[10px] uppercase tracking-wider text-muted">
                      Créée
                    </div>
                    <div className="mt-0.5 text-[13px] text-ink">{formatDate(cle.createdAt)}</div>
                  </div>
                  <div>
                    <div className="font-mono text-[10px] uppercase tracking-wider text-muted">
                      Dernier usage
                    </div>
                    <div className="mt-0.5 text-[13px] text-ink">{formatDate(cle.lastUsedAt)}</div>
                  </div>
                </div>

                {/* Révocation */}
                <div className="flex-none">
                  {confirmRevoke === cle.id ? (
                    <div className="flex items-center gap-2">
                      <span className="text-xs text-muted">Confirmer ?</span>
                      <button
                        type="button"
                        onClick={() => void onRevoquer(cle.id)}
                        disabled={action}
                        className="text-xs font-semibold text-danger hover:underline"
                      >
                        Révoquer
                      </button>
                      <button
                        type="button"
                        onClick={() => setConfirmRevoke(null)}
                        className="text-xs text-muted hover:text-ink"
                      >
                        Non
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setConfirmRevoke(cle.id)}
                      className="border border-line px-3 py-1.5 text-xs font-medium text-danger transition-colors hover:border-danger hover:bg-danger/5"
                    >
                      Révoquer
                    </button>
                  )}
                </div>
              </div>
            ))}
        </div>
      </section>

      {/* Exemple d'utilisation */}
      <section className="mt-10">
        <h3 className="font-mono text-[12px] uppercase tracking-wider text-muted">
          Exemple d&apos;utilisation
        </h3>
        <CodeWindow className="mt-3" filename="appel.sh" lang="bash" copyText={usage}>
          {usage}
        </CodeWindow>
        <div className="mt-4 border-l-2 border-brand bg-tint p-4 text-xs leading-relaxed text-ink">
          <span className="mb-1 block font-bold">Authentification machine-à-machine :</span>
          Vos applications fournissent les en-têtes{" "}
          <code className="bg-white/60 px-1 font-mono text-[11px]">X-BC-Client-Id</code> et{" "}
          <code className="bg-white/60 px-1 font-mono text-[11px]">X-BC-Api-Key</code> à chaque requête.
          Le secret n&apos;est visible qu&apos;au moment de la création d&apos;une clé — conservez-le
          dans le coffre de votre application.
        </div>
      </section>
    </div>
  );
}
