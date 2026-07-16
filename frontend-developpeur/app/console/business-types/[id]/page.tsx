"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import {
  getBusinessType,
  listBusinessTypeVersions,
  createBusinessTypeVersion,
  publishBusinessTypeVersion,
  listConfigParams,
  defineConfigParam,
  publishBusinessType,
  archiveBusinessType,
  ApiError,
  type BusinessType,
  type BusinessTypeVersion,
  type ConfigParam,
} from "@/lib/api";
import { Button } from "@/components/Button";
import Field from "@/components/Field";
import { Banner, EmptyState, LoadingBlock } from "@/components/Feedback";
import { IconPlus, IconCheck } from "@/components/icons";
import { cn } from "@/lib/cn";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T | null };

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

export default function BusinessTypeDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;

  const [type, setType] = useState<Charge<BusinessType>>({ state: "loading", data: null });
  const [versions, setVersions] = useState<{ state: "loading" | "error" | "ok"; data: BusinessTypeVersion[] }>({
    state: "loading",
    data: [],
  });
  const [selectedVersion, setSelectedVersion] = useState<number | null>(null);
  const [action, setAction] = useState(false);
  const [message, setMessage] = useState<{ type: "ok" | "err"; texte: string } | null>(null);

  const chargerType = useCallback(() => {
    getBusinessType(id)
      .then((data) => setType({ state: "ok", data }))
      .catch(() => setType({ state: "error", data: null }));
  }, [id]);

  const chargerVersions = useCallback(() => {
    listBusinessTypeVersions(id)
      .then((data) => {
        setVersions({ state: "ok", data });
        if (data.length > 0 && selectedVersion === null) setSelectedVersion(data[data.length - 1].numero);
      })
      .catch(() => setVersions({ state: "error", data: [] }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  useEffect(chargerType, [chargerType]);
  useEffect(chargerVersions, [chargerVersions]);

  async function onCreerVersion() {
    setAction(true);
    setMessage(null);
    try {
      await createBusinessTypeVersion(id);
      setMessage({ type: "ok", texte: "Nouvelle version créée." });
      chargerVersions();
    } catch (e) {
      setMessage({ type: "err", texte: e instanceof ApiError ? e.detail || e.title : "Création impossible." });
    } finally {
      setAction(false);
    }
  }

  async function onPublierVersion(numero: number) {
    setAction(true);
    setMessage(null);
    try {
      await publishBusinessTypeVersion(id, numero);
      setMessage({ type: "ok", texte: `Version ${numero} publiée — verrouillée et utilisable.` });
      chargerVersions();
    } catch (e) {
      setMessage({ type: "err", texte: e instanceof ApiError ? e.detail || e.title : "Publication impossible." });
    } finally {
      setAction(false);
    }
  }

  async function onPublierType() {
    if (!type.data) return;
    setAction(true);
    setMessage(null);
    try {
      await publishBusinessType(type.data.id);
      setMessage({ type: "ok", texte: "Type publié." });
      chargerType();
    } catch (e) {
      setMessage({ type: "err", texte: e instanceof ApiError ? e.detail || e.title : "Publication impossible." });
    } finally {
      setAction(false);
    }
  }

  async function onArchiverType() {
    if (!type.data) return;
    setAction(true);
    setMessage(null);
    try {
      await archiveBusinessType(type.data.id);
      setMessage({ type: "ok", texte: "Type archivé." });
      chargerType();
    } catch (e) {
      setMessage({ type: "err", texte: e instanceof ApiError ? e.detail || e.title : "Archivage impossible." });
    } finally {
      setAction(false);
    }
  }

  if (type.state === "loading") return <LoadingBlock lines={3} />;
  if (type.state === "error" || !type.data) {
    return (
      <div className="animate-fade-up">
        <Link href="/console/business-types" className="text-sm text-muted hover:text-ink">← Types métier</Link>
        <div className="mt-6"><Banner variant="error">Type métier introuvable.</Banner></div>
      </div>
    );
  }

  const t = type.data;
  const versionCourante = versions.state === "ok" ? versions.data.find((v) => v.numero === selectedVersion) : null;

  return (
    <div className="animate-fade-up">
      <Link href="/console/business-types" className="text-sm text-muted hover:text-ink">← Types métier</Link>

      <div className="mt-4 flex flex-col justify-between gap-4 border-b border-line pb-6 sm:flex-row sm:items-end">
        <div>
          <div className="text-[12px] font-semibold uppercase tracking-wider text-brand">Type métier</div>
          <h1 className="mt-2 font-display text-2xl font-bold text-ink">{t.nom}</h1>
          <div className="mt-2 flex items-center gap-2.5">
            <code className="font-mono text-[13px] text-muted">{t.code}</code>
            <StatutBadge statut={t.statut} />
          </div>
        </div>
        <div className="flex flex-none gap-2">
          {t.statut === "BROUILLON" && (
            <Button variant="secondary" onClick={onPublierType} disabled={action}>Publier le type</Button>
          )}
          {t.statut === "PUBLIE" && (
            <Button variant="secondary" onClick={onArchiverType} disabled={action}>Archiver le type</Button>
          )}
        </div>
      </div>

      {message && (
        <div className={cn("mt-5 rounded-lg border-l-2 px-4 py-3 text-sm", message.type === "ok" ? "border-ok bg-ok/5 text-ink" : "border-danger bg-danger/5 text-danger")}>
          {message.texte}
        </div>
      )}

      {/* Versions */}
      <section className="mt-8">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-lg font-semibold text-ink">Versions</h2>
          <Button size="sm" onClick={onCreerVersion} disabled={action}>
            <IconPlus className="h-4 w-4" /> Nouvelle version
          </Button>
        </div>

        <div className="mt-3">
          {versions.state === "loading" && <LoadingBlock lines={2} />}
          {versions.state === "ok" && versions.data.length === 0 && (
            <EmptyState title="Aucune version" description="Créez la première version pour pouvoir y ajouter offres, règles et opérations." />
          )}
          {versions.state === "ok" && versions.data.length > 0 && (
            <div className="divide-y divide-line border border-line bg-white">
              {versions.data.map((v) => (
                <button
                  key={v.id}
                  onClick={() => setSelectedVersion(v.numero)}
                  className={cn(
                    "flex w-full items-center gap-4 px-5 py-3.5 text-left transition-colors",
                    selectedVersion === v.numero ? "bg-tint" : "hover:bg-subtle"
                  )}
                >
                  <span className="font-mono text-sm font-semibold text-ink">v{v.numero}</span>
                  <span className="text-sm text-muted">{v.libelle}</span>
                  {v.immuable ? (
                    <span className="ml-auto inline-flex items-center gap-1.5 rounded-full border border-ok/60 bg-ok-tint px-2.5 py-0.5 text-[11px] font-semibold text-ok-strong">
                      <IconCheck className="h-3 w-3" /> Publiée
                    </span>
                  ) : (
                    <Button
                      size="sm"
                      variant="secondary"
                      className="ml-auto"
                      onClick={(e) => { e.stopPropagation(); onPublierVersion(v.numero); }}
                      disabled={action}
                    >
                      Publier cette version
                    </Button>
                  )}
                </button>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Configuration de la version sélectionnée */}
      {versionCourante && (
        <section className="mt-10">
          <h2 className="font-display text-lg font-semibold text-ink">
            Configuration — v{versionCourante.numero}
            {versionCourante.immuable && <span className="ml-2 text-sm font-normal text-muted">(publiée, mais les paramètres restent modifiables sauf s&apos;ils sont verrouillés)</span>}
          </h2>
          <ConfigParamsSection typeId={id} numero={versionCourante.numero} />
        </section>
      )}
    </div>
  );
}

function ConfigParamsSection({ typeId, numero }: { typeId: string; numero: number }) {
  const [params, setParams] = useState<{ state: "loading" | "error" | "ok"; data: ConfigParam[] }>({
    state: "loading",
    data: [],
  });
  const [cle, setCle] = useState("");
  const [valeur, setValeur] = useState("");
  const [verrouille, setVerrouille] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  const charger = useCallback(() => {
    listConfigParams(typeId, numero)
      .then((data) => setParams({ state: "ok", data }))
      .catch(() => setParams({ state: "error", data: [] }));
  }, [typeId, numero]);

  useEffect(charger, [charger]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setErreur(null);
    setSubmitting(true);
    try {
      await defineConfigParam(typeId, numero, cle.trim(), valeur.trim(), verrouille);
      setCle("");
      setValeur("");
      setVerrouille(false);
      charger();
    } catch (err) {
      setErreur(err instanceof ApiError ? err.detail || err.title : "Enregistrement impossible.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mt-3 border border-line bg-white p-5">
      {params.state === "loading" && <LoadingBlock lines={2} />}
      {params.state === "ok" && params.data.length > 0 && (
        <ul className="mb-5 divide-y divide-line border border-line">
          {params.data.map((p) => (
            <li key={p.id} className="flex items-center gap-3 px-4 py-2.5 text-sm">
              <code className="font-mono font-semibold text-ink">{p.cle}</code>
              <span className="text-muted">=</span>
              <span className="text-body">{p.valeur}</span>
              {p.verrouille && (
                <span className="ml-auto font-mono text-[11px] uppercase tracking-wider text-muted">Verrouillé</span>
              )}
            </li>
          ))}
        </ul>
      )}
      {params.state === "ok" && params.data.length === 0 && (
        <p className="mb-5 text-sm text-muted">Aucun paramètre pour cette version.</p>
      )}

      <form onSubmit={onSubmit} className="grid gap-3 sm:grid-cols-4 sm:items-end">
        <Field label="Clé" id="cle" placeholder="ex. devise" value={cle} onChange={(e) => setCle(e.target.value)} required />
        <Field label="Valeur" id="valeur" placeholder="ex. XOF" value={valeur} onChange={(e) => setValeur(e.target.value)} required />
        <label className="flex items-center gap-2 pb-2.5 text-sm text-ink">
          <input type="checkbox" checked={verrouille} onChange={(e) => setVerrouille(e.target.checked)} />
          Verrouillé
        </label>
        <Button type="submit" size="sm" disabled={submitting || !cle.trim() || !valeur.trim()}>
          {submitting ? "…" : "Ajouter"}
        </Button>
      </form>
      {erreur && <div className="mt-3"><Banner variant="error">{erreur}</Banner></div>}
    </div>
  );
}
