"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import Field from "@/components/Field";
import { Button, ButtonLink } from "@/components/Button";
import { Banner, EmptyState, LoadingBlock } from "@/components/Feedback";
import { useAuth } from "@/lib/auth-context";
import {
  ApiError,
  createBusiness,
  listBusinesses,
  listBusinessTypes,
  listBusinessTypeVersions,
  type Business,
  type BusinessType,
  type BusinessTypeVersion,
} from "@/lib/api";
import { cn } from "@/lib/cn";

function StatutBadge({ value }: { value: string }) {
  const actif = value === "PUBLIE" || value === "ACTIVE";
  const style = actif
    ? "text-ok-strong border-ok/60 bg-ok-tint"
    : value === "ARCHIVE" || value === "FERMEE"
      ? "text-muted border-line bg-subtle"
      : "text-brand border-brand/40 bg-brand/5";
  return (
    <span className={cn("inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-[11px] font-semibold", style)}>
      {actif && <span className="h-1.5 w-1.5 rounded-full bg-ok" />}
      {value}
    </span>
  );
}

export default function BusinessesPage() {
  const { profil } = useAuth();
  const [businesses, setBusinesses] = useState<Business[] | null>(null);
  const [types, setTypes] = useState<BusinessType[] | null>(null);
  const [versions, setVersions] = useState<BusinessTypeVersion[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loadingVersions, setLoadingVersions] = useState(false);

  const [typeId, setTypeId] = useState("");
  const [versionNumber, setVersionNumber] = useState("");
  const [nom, setNom] = useState("");
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [created, setCreated] = useState<Business | null>(null);

  const charger = useCallback(async () => {
    setLoadError(null);
    try {
      const [biz, tps] = await Promise.all([listBusinesses(), listBusinessTypes()]);
      setBusinesses(biz);
      setTypes(tps);
    } catch (e) {
      setLoadError(e instanceof ApiError ? e.detail || e.title : "Chargement impossible.");
    }
  }, []);

  useEffect(() => {
    void charger();
  }, [charger]);

  const typesPublies = (types ?? []).filter((t) => t.statut === "PUBLIE");
  const versionsPubliees = versions.filter((v) => v.immuable || v.publieeLe);

  useEffect(() => {
    if (!typeId) {
      setVersions([]);
      setVersionNumber("");
      return;
    }
    setLoadingVersions(true);
    listBusinessTypeVersions(typeId)
      .then((data) => {
        setVersions(data);
        const pub = data.filter((v) => v.immuable || v.publieeLe);
        if (pub.length > 0) setVersionNumber(String(pub[0].numero));
        else setVersionNumber("");
      })
      .catch(() => setVersions([]))
      .finally(() => setLoadingVersions(false));
  }, [typeId]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!typeId || !versionNumber || !nom.trim()) return;
    setCreating(true);
    setCreateError(null);
    setCreated(null);
    try {
      const b = await createBusiness({
        typeId,
        versionNumber: parseInt(versionNumber, 10),
        nom: nom.trim(),
      });
      setCreated(b);
      setNom("");
      await charger();
    } catch (err) {
      setCreateError(
        err instanceof ApiError
          ? err.detail || err.title
          : "Création impossible."
      );
    } finally {
      setCreating(false);
    }
  }

  return (
    <div className="animate-fade-up">
      <div className="border-b border-line pb-6">
        <div className="text-[12px] font-semibold uppercase tracking-wider text-brand">Ressources</div>
        <h1 className="mt-2 font-display text-3xl font-bold text-ink">Entreprises</h1>
        <p className="mt-1 text-sm text-muted">
          Instanciez vos types métier en entreprises opérationnelles. Chaque entreprise dispose
          d&apos;une clé API dédiée.
        </p>
      </div>

      {loadError && (
        <div className="mt-6">
          <Banner variant="error">{loadError}</Banner>
        </div>
      )}

      {/* Liste */}
      <section className="mt-8">
        <h2 className="font-display text-lg font-semibold text-ink">Vos entreprises</h2>
        <div className="mt-3 border border-line bg-white">
          {businesses === null && !loadError && <LoadingBlock />}
          {businesses && businesses.length === 0 && (
            <div className="p-5">
              <EmptyState
                title="Aucune entreprise"
                description="Créez votre première entreprise avec le formulaire ci-dessous, à partir d'un type métier publié."
              />
            </div>
          )}
          {businesses &&
            businesses.map((b, i) => (
              <div
                key={b.id}
                className={cn("flex flex-col gap-2 px-5 py-4 sm:flex-row sm:items-center", i !== 0 && "border-t border-line")}
              >
                <div className="min-w-0 flex-1">
                  <div className="font-medium text-ink">{b.nom}</div>
                  <div className="mt-0.5 font-mono text-[12px] text-muted">
                    v{b.versionNumber} · {b.id.slice(0, 8)}…
                  </div>
                </div>
                <StatutBadge value={b.cycleVie} />
                <ButtonLink href="/console/api-key" size="sm">
                  Gérer la clé API →
                </ButtonLink>
              </div>
            ))}
        </div>
      </section>

      {/* Création */}
      <section className="mt-10">
        <h2 className="font-display text-lg font-semibold text-ink">Créer une entreprise</h2>

        {!profil?.owner && (
          <div className="mt-3">
            <Banner variant="info">
              Pour créer une entreprise, vous devez être <strong>OWNER</strong>. Rapprochez-vous de
              l&apos;administrateur de votre tenant.
            </Banner>
          </div>
        )}

        {profil?.owner && types !== null && typesPublies.length === 0 && (
          <div className="mt-3">
            <EmptyState
              title="Aucun type métier publié"
              description="Publiez d'abord un type métier et une version via l'API avant de créer une entreprise."
              actionHref="/console/docs"
              actionLabel="Voir la documentation →"
            />
          </div>
        )}

        {profil?.owner && typesPublies.length > 0 && (
          <form onSubmit={(e) => void onSubmit(e)} className="mt-4 space-y-5 border border-line bg-white p-5">
            <p className="border-l-2 border-brand bg-tint px-3 py-2.5 text-[12.5px] leading-relaxed text-ink">
              Une entreprise est l&apos;<strong>instance concrète</strong> d&apos;un type métier déjà
              déclaré (offres, rôles, règles, opérations). Le <strong>type</strong> définit le modèle ;
              la <strong>version</strong> le fige (une version publiée est immuable — c&apos;est ce qui
              garantit que votre entreprise ne changera jamais de comportement sous vos pieds).
            </p>

            <label className="block">
              <span className="mb-1.5 block text-[13px] font-medium text-ink">Type métier</span>
              <span className="mb-1.5 block text-[12px] text-muted">
                Le modèle (offres, rôles, règles) que cette entreprise va suivre.
              </span>
              <select
                value={typeId}
                onChange={(e) => setTypeId(e.target.value)}
                required
                className="h-11 w-full border border-line bg-white px-3 text-sm outline-none focus:border-brand"
              >
                <option value="">Sélectionner un type publié…</option>
                {typesPublies.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.code} — {t.nom}
                  </option>
                ))}
              </select>
            </label>

            {typeId && versionsPubliees.length <= 1 && (
              // Cas le plus courant (une seule version publiée) : le champ reste réel et connecté au
              // backend, mais s'efface visuellement puisqu'il n'y a rien à choisir.
              <input type="hidden" value={versionNumber} readOnly />
            )}

            {typeId && versionsPubliees.length > 1 && (
              <label className="block">
                <span className="mb-1.5 block text-[13px] font-medium text-ink">Version</span>
                <span className="mb-1.5 block text-[12px] text-muted">
                  Plusieurs versions publiées existent pour ce type — chacune est figée (immuable),
                  choisissez celle que cette entreprise doit suivre.
                </span>
                <select
                  value={versionNumber}
                  onChange={(e) => setVersionNumber(e.target.value)}
                  required
                  disabled={loadingVersions}
                  className="h-11 w-full border border-line bg-white px-3 text-sm outline-none focus:border-brand disabled:bg-subtle"
                >
                  {loadingVersions && <option value="">Chargement…</option>}
                  {versionsPubliees.map((v) => (
                    <option key={v.id} value={v.numero}>
                      v{v.numero}{v.libelle ? ` — ${v.libelle}` : ""}
                    </option>
                  ))}
                </select>
              </label>
            )}

            {typeId && !loadingVersions && versionsPubliees.length === 0 && (
              <Banner variant="info">
                Ce type métier n&apos;a aucune version <strong>publiée</strong> — publiez-en une (elle
                devient alors immuable) avant de pouvoir créer une entreprise dessus.
              </Banner>
            )}

            <Field
              label="Nom de l'entreprise"
              id="nom-entreprise"
              placeholder="ex. Pharmacie Centrale"
              value={nom}
              onChange={(e) => setNom(e.target.value)}
              required
            />

            {createError && <Banner variant="error">{createError}</Banner>}

            {created && (
              <Banner variant="success">
                Entreprise « {created.nom} » créée.{" "}
                <ButtonLink href="/console/api-key" size="sm" className="ml-1 inline-flex">
                  Gérer la clé API
                </ButtonLink>
              </Banner>
            )}

            <Button type="submit" disabled={creating || !typeId || !versionNumber || !nom.trim()}>
              {creating ? "Création…" : "Créer l'entreprise"}
            </Button>
          </form>
        )}
      </section>
    </div>
  );
}
