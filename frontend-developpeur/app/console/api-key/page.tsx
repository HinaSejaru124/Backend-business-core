"use client";

import { useCallback, useEffect, useState } from "react";
import CodeWindow from "@/components/CodeWindow";
import Field from "@/components/Field";
import { Button } from "@/components/Button";
import { Banner, EmptyState, LoadingBlock } from "@/components/Feedback";
import { useAuth } from "@/lib/auth-context";
import {
  API_BASE,
  ApiError,
  createBusinessApiKey,
  getBusinessApiKey,
  listBusinesses,
  renameBusinessApiKey,
  revokeBusinessApiKey,
  type Business,
  type BusinessApiKey,
  type BusinessApiKeyCreated,
} from "@/lib/api";
import { IconCopy, IconCheck, IconEye, IconEyeOff, IconClose } from "@/components/icons";
import { cn } from "@/lib/cn";

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
  const { profil } = useAuth();
  const developerId = profil?.developerId ?? "<developerId>";

  const [businesses, setBusinesses] = useState<Business[] | null>(null);
  const [selectedId, setSelectedId] = useState("");
  const [cle, setCle] = useState<BusinessApiKey | null>(null);
  const [pasDeCle, setPasDeCle] = useState(false);
  const [etat, setEtat] = useState<"loading" | "error" | "ok">("loading");
  const [erreur, setErreur] = useState<string | null>(null);

  const [formOuvert, setFormOuvert] = useState(false);
  const [nouveauNom, setNouveauNom] = useState("");
  const [creation, setCreation] = useState(false);
  const [creee, setCreee] = useState<BusinessApiKeyCreated | null>(null);

  const [editionNom, setEditionNom] = useState("");
  const [edition, setEdition] = useState(false);
  const [confirmRevoke, setConfirmRevoke] = useState(false);
  const [action, setAction] = useState(false);

  const [copie, copier] = useCopie();
  const [revelerCree, setRevelerCree] = useState(true);

  const chargerBusinesses = useCallback(async () => {
    try {
      const data = await listBusinesses();
      setBusinesses(data);
      if (data.length > 0 && !selectedId) setSelectedId(data[0].id);
      return data;
    } catch (e) {
      setErreur(e instanceof ApiError ? e.detail || e.title : "Chargement des entreprises impossible.");
      setEtat("error");
      return [];
    }
  }, [selectedId]);

  const chargerCle = useCallback(async (businessId: string) => {
    if (!businessId) return;
    setEtat("loading");
    setErreur(null);
    setPasDeCle(false);
    setCle(null);
    try {
      const k = await getBusinessApiKey(businessId);
      setCle(k);
      setEditionNom(k.name);
      setEtat("ok");
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        setPasDeCle(true);
        setEtat("ok");
      } else {
        setErreur(e instanceof ApiError ? e.detail || e.title : "Chargement impossible.");
        setEtat("error");
      }
    }
  }, []);

  useEffect(() => {
    void chargerBusinesses();
  }, [chargerBusinesses]);

  useEffect(() => {
    if (selectedId) void chargerCle(selectedId);
  }, [selectedId, chargerCle]);

  const entrepriseCourante = businesses?.find((b) => b.id === selectedId);

  async function onCreer() {
    if (!selectedId || creation) return;
    setErreur(null);
    setCreation(true);
    try {
      const c = await createBusinessApiKey(selectedId, nouveauNom);
      setCreee(c);
      setRevelerCree(true);
      setNouveauNom("");
      setFormOuvert(false);
      await chargerCle(selectedId);
    } catch (e) {
      setErreur(e instanceof ApiError ? e.detail || e.title : "Création impossible.");
    } finally {
      setCreation(false);
    }
  }

  async function onRenommer() {
    const nom = editionNom.trim();
    if (!selectedId || !nom) return;
    setAction(true);
    setErreur(null);
    try {
      const k = await renameBusinessApiKey(selectedId, nom);
      setCle(k);
      setEdition(false);
    } catch (e) {
      setErreur(e instanceof ApiError ? e.detail || e.title : "Renommage impossible.");
    } finally {
      setAction(false);
    }
  }

  async function onRevoquer() {
    if (!selectedId) return;
    setAction(true);
    setErreur(null);
    try {
      await revokeBusinessApiKey(selectedId);
      setConfirmRevoke(false);
      setCle(null);
      setPasDeCle(true);
    } catch (e) {
      setErreur(e instanceof ApiError ? e.detail || e.title : "Révocation impossible.");
    } finally {
      setAction(false);
    }
  }

  const secretExemple = creee && revelerCree ? creee.apiKey : "••••••••••••••••";
  const usage = `curl ${API_BASE}/v1/business-types \\
  -H "X-BC-Client-Id: ${developerId}" \\
  -H "X-BC-Api-Key: ${secretExemple}"`;

  return (
    <div className="animate-fade-up">
      <div className="border-b border-line pb-6">
        <div className="text-[12px] font-semibold uppercase tracking-wider text-brand">Accès</div>
        <h1 className="mt-2 font-display text-3xl font-bold text-ink">Clés d&apos;API</h1>
        <p className="mt-1 text-sm text-muted">
          Chaque entreprise dispose d&apos;une clé API active à la fois.{" "}
          <code className="font-mono text-[12px]">X-BC-Client-Id</code> est votre identifiant
          développeur stable (<code className="font-mono text-[12px]">GET /v1/auth/me</code>).
        </p>
      </div>

      {businesses && businesses.length === 0 && (
        <div className="mt-6">
          <EmptyState
            title="Aucune entreprise"
            description="Créez d'abord une entreprise pour générer une clé API scopée à celle-ci."
            actionHref="/console/businesses"
            actionLabel="Créer une entreprise →"
          />
        </div>
      )}

      {businesses && businesses.length > 0 && (
        <div className="mt-6">
          <label className="block max-w-md">
            <span className="mb-1.5 block text-[13px] font-medium text-ink">Entreprise</span>
            <select
              value={selectedId}
              onChange={(e) => setSelectedId(e.target.value)}
              className="h-11 w-full border border-line bg-white px-3 text-sm outline-none focus:border-brand"
            >
              {businesses.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.nom} — v{b.versionNumber}
                </option>
              ))}
            </select>
          </label>
        </div>
      )}

      {creee && (
        <div className="mt-6 border-2 border-brand bg-tint p-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h2 className="font-display text-lg font-bold text-ink">
                Clé « {creee.name} » créée pour {entrepriseCourante?.nom}
              </h2>
              <p className="mt-1 text-sm text-ink">
                Copiez le secret maintenant : <strong>il ne sera plus jamais affiché</strong>.
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
                Developer ID (X-BC-Client-Id)
              </dt>
              <dd className="mt-1 flex items-center justify-between gap-2 border border-line bg-white p-2.5">
                <code className="truncate font-mono text-sm text-ink select-all">{developerId}</code>
                <button
                  type="button"
                  onClick={() => copier(developerId, "did")}
                  className="flex-none text-xs font-medium text-brand hover:underline"
                >
                  {copie === "did" ? "Copié !" : "Copier"}
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
        <div className="mt-6">
          <Banner variant="error">{erreur}</Banner>
        </div>
      )}

      {businesses && businesses.length > 0 && (
        <section className="mt-8">
          <div className="flex flex-col justify-between gap-3 border-b border-line pb-4 sm:flex-row sm:items-center">
            <h2 className="font-display text-lg font-semibold text-ink">
              Clé de {entrepriseCourante?.nom ?? "l'entreprise"}
            </h2>
            {pasDeCle && !formOuvert && (
              <Button size="sm" onClick={() => setFormOuvert(true)}>
                + Créer la clé
              </Button>
            )}
          </div>

          {formOuvert && pasDeCle && (
            <div className="mt-4 border border-line bg-white p-5">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
                <div className="flex-1">
                  <Field
                    label="Nom de la clé (optionnel)"
                    id="nom-cle"
                    placeholder="ex. Production, Staging…"
                    value={nouveauNom}
                    maxLength={64}
                    onChange={(e) => setNouveauNom(e.target.value)}
                    hint="Par défaut : « Default »."
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

          <div className="mt-4 border border-line bg-white">
            {etat === "loading" && <LoadingBlock lines={2} />}
            {etat === "error" && (
              <div className="p-5 text-sm text-danger">Impossible de charger la clé.</div>
            )}
            {etat === "ok" && pasDeCle && !formOuvert && (
              <div className="p-6 text-center text-sm text-muted">
                Aucune clé active pour cette entreprise. Créez-en une avec le bouton ci-dessus.
              </div>
            )}
            {etat === "ok" && cle && (
              <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center">
                <div className="min-w-0 flex-1">
                  {edition ? (
                    <div className="flex items-center gap-2">
                      <input
                        autoFocus
                        value={editionNom}
                        maxLength={64}
                        onChange={(e) => setEditionNom(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter") void onRenommer();
                          if (e.key === "Escape") setEdition(false);
                        }}
                        className="h-9 w-48 border border-line bg-white px-2.5 text-sm outline-none focus:border-brand"
                      />
                      <button
                        type="button"
                        onClick={() => void onRenommer()}
                        disabled={action}
                        className="text-xs font-medium text-brand hover:underline"
                      >
                        Enregistrer
                      </button>
                      <button
                        type="button"
                        onClick={() => setEdition(false)}
                        className="text-xs text-muted hover:text-ink"
                      >
                        Annuler
                      </button>
                    </div>
                  ) : (
                    <div className="flex items-center gap-2">
                      <span className="font-display text-[15px] font-semibold text-ink">{cle.name}</span>
                      <button
                        type="button"
                        onClick={() => {
                          setEdition(true);
                          setEditionNom(cle.name);
                        }}
                        className="text-xs text-muted hover:text-brand"
                      >
                        Renommer
                      </button>
                    </div>
                  )}
                  <div className="mt-1.5">
                    <span
                      className={cn(
                        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-[11px] font-semibold",
                        cle.status === "ACTIVE"
                          ? "border-ok/60 bg-ok-tint text-ok-strong"
                          : "border-line bg-subtle text-muted"
                      )}
                    >
                      {cle.status === "ACTIVE" && <span className="h-1.5 w-1.5 rounded-full bg-ok" />}
                      {cle.status}
                    </span>
                  </div>
                </div>
                <div className="flex gap-6">
                  <div>
                    <div className="font-mono text-[10px] uppercase tracking-wider text-muted">Créée</div>
                    <div className="mt-0.5 text-[13px] text-ink">{formatDate(cle.createdAt)}</div>
                  </div>
                  <div>
                    <div className="font-mono text-[10px] uppercase tracking-wider text-muted">Dernier usage</div>
                    <div className="mt-0.5 text-[13px] text-ink">{formatDate(cle.lastUsedAt)}</div>
                  </div>
                </div>
                <div className="flex-none">
                  {confirmRevoke ? (
                    <div className="flex items-center gap-2">
                      <span className="text-xs text-muted">Confirmer ?</span>
                      <Button variant="danger" size="sm" onClick={() => void onRevoquer()} disabled={action}>
                        Révoquer
                      </Button>
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => setConfirmRevoke(false)}
                        disabled={action}
                      >
                        Non
                      </Button>
                    </div>
                  ) : (
                    <Button variant="danger" size="sm" onClick={() => setConfirmRevoke(true)}>
                      Révoquer la clé
                    </Button>
                  )}
                </div>
              </div>
            )}
          </div>
        </section>
      )}

      <section className="mt-10">
        <h3 className="font-mono text-[12px] uppercase tracking-wider text-muted">
          Exemple d&apos;utilisation
        </h3>
        <CodeWindow className="mt-3" filename="appel.sh" lang="bash" copyText={usage}>
          {usage}
        </CodeWindow>
        <div className="mt-4 border-l-2 border-brand bg-tint p-4 text-xs leading-relaxed text-ink">
          <span className="mb-1 block font-bold">Authentification machine-à-machine :</span>
          Vos applications fournissent{" "}
          <code className="bg-white/60 px-1 font-mono text-[11px]">X-BC-Client-Id</code> (votre{" "}
          <code className="font-mono text-[11px]">developerId</code>) et{" "}
          <code className="bg-white/60 px-1 font-mono text-[11px]">X-BC-Api-Key</code> (secret de
          l&apos;entreprise). Connectez-vous au moins une fois via{" "}
          <code className="font-mono text-[11px]">POST /v1/auth/login</code> pour activer la clé.
        </div>
      </section>
    </div>
  );
}
