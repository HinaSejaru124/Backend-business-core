"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import {
  listBusinessTypes,
  createBusinessType,
  publishBusinessType,
  archiveBusinessType,
  ApiError,
  type BusinessType,
} from "@/lib/api";
import { Button, ButtonLink } from "@/components/Button";
import Field from "@/components/Field";
import { Banner, EmptyState, LoadingBlock } from "@/components/Feedback";
import { IconPlus, IconArrowRight } from "@/components/icons";
import { cn } from "@/lib/cn";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T };

function StatutBadge({ statut }: { statut: string }) {
  const style =
    statut === "PUBLIE"
      ? "border-ok/60 bg-ok-tint text-ok-strong"
      : statut === "ARCHIVE"
        ? "border-line bg-subtle text-muted"
        : "border-brand/40 bg-brand/5 text-brand";
  return (
    <span className={cn("inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-[11px] font-semibold", style)}>
      {statut === "PUBLIE" && <span className="h-1.5 w-1.5 rounded-full bg-ok" />}
      {statut}
    </span>
  );
}

export default function BusinessTypesPage() {
  const [types, setTypes] = useState<Charge<BusinessType[]>>({ state: "loading", data: [] });
  const [formOuvert, setFormOuvert] = useState(false);
  const [action, setAction] = useState<string | null>(null); // id en cours d'action
  const [erreur, setErreur] = useState<string | null>(null);

  const charger = useCallback(() => {
    listBusinessTypes()
      .then((data) => setTypes({ state: "ok", data }))
      .catch(() => setTypes({ state: "error", data: [] }));
  }, []);

  useEffect(charger, [charger]);

  async function onPublier(t: BusinessType) {
    setAction(t.id);
    setErreur(null);
    try {
      await publishBusinessType(t.id);
      charger();
    } catch (e) {
      setErreur(e instanceof ApiError ? e.detail || e.title : "Publication impossible.");
    } finally {
      setAction(null);
    }
  }

  async function onArchiver(t: BusinessType) {
    setAction(t.id);
    setErreur(null);
    try {
      await archiveBusinessType(t.id);
      charger();
    } catch (e) {
      setErreur(e instanceof ApiError ? e.detail || e.title : "Archivage impossible.");
    } finally {
      setAction(null);
    }
  }

  return (
    <div className="animate-fade-up">
      <div className="flex flex-col justify-between gap-4 border-b border-line pb-6 sm:flex-row sm:items-end">
        <div>
          <div className="text-[12px] font-semibold uppercase tracking-wider text-brand">Modélisation</div>
          <h1 className="mt-2 font-display text-3xl font-bold text-ink">Types métier</h1>
          <p className="mt-1 max-w-2xl text-sm text-muted">
            Le modèle générique que vous déclarez pour votre métier (ex. « Pharmacie », « Boutique ») —
            une entreprise s&apos;instancie ensuite à partir d&apos;un type <strong>publié</strong>.
          </p>
        </div>
        <Button onClick={() => setFormOuvert((v) => !v)}>
          <IconPlus className="h-4 w-4" /> {formOuvert ? "Fermer" : "Nouveau type"}
        </Button>
      </div>

      {erreur && (
        <div className="mt-6">
          <Banner variant="error">{erreur}</Banner>
        </div>
      )}

      {formOuvert && (
        <div className="mt-6">
          <NouveauTypeForm
            onSuccess={() => {
              setFormOuvert(false);
              charger();
            }}
          />
        </div>
      )}

      <div className="mt-8">
        {types.state === "loading" && <LoadingBlock lines={3} />}
        {types.state === "error" && <Banner variant="error">Chargement des types impossible.</Banner>}
        {types.state === "ok" && types.data.length === 0 && (
          <EmptyState
            title="Aucun type métier"
            description="Créez votre premier type métier avec le bouton ci-dessus — il reste privé à votre compte."
          />
        )}
        {types.state === "ok" && types.data.length > 0 && (
          <div className="divide-y divide-line border border-line bg-white">
            {types.data.map((t) => (
              <div key={t.id} className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2.5">
                    <Link href={`/console/business-types/${t.id}`} className="font-display text-[15px] font-semibold text-ink hover:text-brand">
                      {t.nom}
                    </Link>
                    <code className="font-mono text-[12px] text-muted">{t.code}</code>
                  </div>
                </div>
                <StatutBadge statut={t.statut} />
                <div className="flex flex-none items-center gap-2">
                  {t.statut === "BROUILLON" && (
                    <Button size="sm" variant="secondary" onClick={() => onPublier(t)} disabled={action === t.id}>
                      {action === t.id ? "…" : "Publier"}
                    </Button>
                  )}
                  {t.statut === "PUBLIE" && (
                    <Button size="sm" variant="secondary" onClick={() => onArchiver(t)} disabled={action === t.id}>
                      {action === t.id ? "…" : "Archiver"}
                    </Button>
                  )}
                  <ButtonLink href={`/console/business-types/${t.id}`} size="sm">
                    Gérer <IconArrowRight className="h-3.5 w-3.5" />
                  </ButtonLink>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="mt-6 border-l-2 border-brand bg-tint px-4 py-3 text-xs leading-relaxed text-ink">
        <strong>Vos types métier sont privés</strong> — même publiés, ils ne sont jamais visibles ni
        utilisables par un autre compte développeur, y compris sur cette même base de données. La
        publication signifie seulement « prêt à instancier des entreprises pour votre propre compte »,
        pas « partagé publiquement ».
      </div>
    </div>
  );
}

function NouveauTypeForm({ onSuccess }: { onSuccess: () => void }) {
  const [code, setCode] = useState("");
  const [nom, setNom] = useState("");
  const [domainCode, setDomainCode] = useState("");
  const [domainNom, setDomainNom] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setErreur(null);
    setSubmitting(true);
    try {
      await createBusinessType(code.trim(), nom.trim(), domainCode.trim() || undefined, domainNom.trim() || undefined);
      setCode("");
      setNom("");
      setDomainCode("");
      setDomainNom("");
      onSuccess();
    } catch (err) {
      setErreur(err instanceof ApiError ? err.detail || err.title : "Création impossible.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-4 border border-line bg-white p-5">
      <div className="grid gap-4 sm:grid-cols-2">
        <Field
          label="Code (unique, majuscules)"
          id="code"
          placeholder="ex. RETAIL"
          value={code}
          onChange={(e) => setCode(e.target.value.toUpperCase())}
          required
        />
        <Field
          label="Nom"
          id="nom"
          placeholder="ex. Commerce de détail"
          value={nom}
          onChange={(e) => setNom(e.target.value)}
          required
        />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field
          label="Code domaine kernel (optionnel)"
          id="domainCode"
          placeholder="ex. COMMERCE"
          value={domainCode}
          onChange={(e) => setDomainCode(e.target.value)}
        />
        <Field
          label="Nom du domaine (optionnel)"
          id="domainNom"
          placeholder="ex. Commerce"
          value={domainNom}
          onChange={(e) => setDomainNom(e.target.value)}
        />
      </div>
      {erreur && <Banner variant="error">{erreur}</Banner>}
      <Button type="submit" disabled={submitting || !code.trim() || !nom.trim()}>
        {submitting ? "Création…" : "Créer le type (brouillon)"}
      </Button>
    </form>
  );
}
