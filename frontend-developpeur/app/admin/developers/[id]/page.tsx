"use client";

import { useCallback, useEffect, useState, type ReactNode } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import {
  adminDeveloperDetail,
  adminBlockDeveloper,
  adminUnblockDeveloper,
  adminRevokeKey,
  ApiError,
  type AdminDeveloperDetail,
} from "@/lib/api";
import { Button } from "@/components/Button";
import { IconActivity, IconBan, IconCheck } from "@/components/icons";
import { PlanBadge, StatutBadge, ConsoBar } from "@/components/admin-ui";
import { cn } from "@/lib/cn";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T | null };

function fmt(n: number): string {
  return n.toLocaleString("fr-FR");
}

function fmtDate(iso: string | null): string {
  if (!iso) return "—";
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? "—" : d.toLocaleDateString("fr-FR", { day: "2-digit", month: "short", year: "numeric" });
}

export default function AdminDeveloperDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const [charge, setCharge] = useState<Charge<AdminDeveloperDetail>>({ state: "loading", data: null });
  const [action, setAction] = useState(false);
  const [message, setMessage] = useState<{ type: "ok" | "err"; texte: string } | null>(null);

  const recharger = useCallback(() => {
    adminDeveloperDetail(id)
      .then((data) => setCharge({ state: "ok", data }))
      .catch(() => setCharge({ state: "error", data: null }));
  }, [id]);

  useEffect(recharger, [recharger]);

  async function basculerBlocage(bloque: boolean) {
    setAction(true);
    setMessage(null);
    try {
      if (bloque) await adminUnblockDeveloper(id);
      else await adminBlockDeveloper(id);
      setMessage({ type: "ok", texte: bloque ? "Développeur réactivé." : "Développeur bloqué — ses clés cessent de fonctionner." });
      recharger();
    } catch (e) {
      setMessage({ type: "err", texte: e instanceof ApiError ? e.detail || e.title : "Action impossible." });
    } finally {
      setAction(false);
    }
  }

  async function revoquer(cleId: string, nom: string) {
    if (!window.confirm(`Révoquer définitivement la clé « ${nom} » ? Le développeur devra en créer une nouvelle.`)) return;
    setAction(true);
    setMessage(null);
    try {
      await adminRevokeKey(cleId);
      setMessage({ type: "ok", texte: "Clé révoquée." });
      recharger();
    } catch (e) {
      setMessage({ type: "err", texte: e instanceof ApiError ? e.detail || e.title : "Révocation impossible." });
    } finally {
      setAction(false);
    }
  }

  if (charge.state === "loading") {
    return <p className="animate-fade-up text-sm text-muted">Chargement…</p>;
  }
  if (charge.state === "error" || !charge.data) {
    return (
      <div className="animate-fade-up">
        <Link href="/admin/developers" className="text-sm text-muted hover:text-ink">← Développeurs</Link>
        <p className="mt-6 rounded-lg border border-danger/25 bg-danger/5 px-4 py-3 text-sm text-danger">
          Développeur introuvable.
        </p>
      </div>
    );
  }

  const { resume, entreprises, cles } = charge.data;
  const bloque = resume.status !== "ACTIVE";

  return (
    <div className="animate-fade-up">
      <Link href="/admin/developers" className="text-sm text-muted hover:text-ink">← Développeurs</Link>

      <div className="mt-4 flex flex-col justify-between gap-4 border-b border-line pb-6 sm:flex-row sm:items-end">
        <div>
          <div className="text-[12px] font-semibold uppercase tracking-wider text-brand">Développeur</div>
          <h1 className="mt-2 font-display text-2xl font-bold text-ink">{resume.email}</h1>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <PlanBadge plan={resume.plan} />
            <StatutBadge status={resume.status} />
          </div>
        </div>
        <div className="flex flex-none items-center gap-3">
          <Link
            href={`/admin/track?dev=${resume.id}`}
            className="inline-flex items-center gap-1.5 rounded-lg border border-line bg-white px-3.5 py-2 text-[13px] font-semibold text-ink transition-all hover:-translate-y-0.5 hover:border-brand/40"
          >
            <IconActivity className="h-4 w-4 text-brand" /> Track des requêtes
          </Link>
          {bloque ? (
            <Button variant="secondary" onClick={() => basculerBlocage(true)} disabled={action}>
              <IconCheck className="h-4 w-4" /> Débloquer
            </Button>
          ) : (
            <Button variant="danger" onClick={() => basculerBlocage(false)} disabled={action}>
              <IconBan className="h-4 w-4" /> Bloquer
            </Button>
          )}
        </div>
      </div>

      {message && (
        <div
          className={cn(
            "mt-5 rounded-lg border-l-2 px-4 py-3 text-sm",
            message.type === "ok" ? "border-ok bg-ok/5 text-ink" : "border-danger bg-danger/5 text-danger"
          )}
        >
          {message.texte}
        </div>
      )}

      {/* Résumé chiffré */}
      <div className="mt-8 grid gap-px overflow-hidden rounded-xl border border-line bg-line sm:grid-cols-3">
        <Info label="Entreprises" value={fmt(resume.nbEntreprises)} />
        <Info label="Clés actives" value={fmt(resume.nbClesActives)} />
        <Info
          label="Consommation (mois)"
          value={resume.illimite ? "Illimité" : `${fmt(resume.consoMois)} / ${fmt(resume.quota)}`}
          extra={<ConsoBar pct={resume.pctConso} illimite={resume.illimite} />}
        />
      </div>

      {/* Entreprises */}
      <section className="mt-10">
        <h2 className="font-display text-lg font-semibold text-ink">Entreprises</h2>
        <div className="mt-3 overflow-hidden rounded-xl border border-line bg-white shadow-card">
          {entreprises.length === 0 && <div className="p-5 text-sm text-muted">Aucune entreprise créée.</div>}
          {entreprises.map((e, i) => (
            <div key={e.id} className={cn("flex items-center gap-4 px-5 py-3.5", i !== 0 && "border-t border-line")}>
              <span className="truncate text-sm font-medium text-ink">{e.nom}</span>
              <span className="font-mono text-[12px] text-muted">v{e.numeroVersion}</span>
              <span className="ml-auto rounded-full border border-line bg-subtle px-2.5 py-0.5 text-[11px] font-semibold text-muted">
                {e.cycleVie}
              </span>
            </div>
          ))}
        </div>
      </section>

      {/* Clés API */}
      <section className="mt-10">
        <h2 className="font-display text-lg font-semibold text-ink">Clés API</h2>
        <div className="mt-3 overflow-hidden rounded-xl border border-line bg-white shadow-card">
          {cles.length === 0 && <div className="p-5 text-sm text-muted">Aucune clé.</div>}
          {cles.map((c, i) => {
            const active = c.status === "ACTIVE";
            return (
              <div key={c.id} className={cn("flex flex-wrap items-center gap-4 px-5 py-4", i !== 0 && "border-t border-line")}>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="truncate text-sm font-medium text-ink">{c.nom}</span>
                    <span
                      className={cn(
                        "rounded-full border px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider",
                        active ? "border-ok/40 bg-ok-tint text-ok-strong" : "border-line bg-subtle text-muted"
                      )}
                    >
                      {active ? "Active" : "Révoquée"}
                    </span>
                  </div>
                  <div className="mt-0.5 font-mono text-[11px] text-muted">
                    créée {fmtDate(c.createdAt)} · dernier usage {fmtDate(c.lastUsedAt)}
                  </div>
                </div>
                {active && (
                  <Button variant="danger" size="sm" onClick={() => revoquer(c.id, c.nom)} disabled={action}>
                    Révoquer
                  </Button>
                )}
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
}

function Info({ label, value, extra }: { label: string; value: string; extra?: ReactNode }) {
  return (
    <div className="bg-white p-5">
      <div className="text-xs uppercase tracking-wider text-muted">{label}</div>
      <div className="mt-1.5 font-display text-lg font-bold text-ink">{value}</div>
      {extra && <div className="mt-2">{extra}</div>}
    </div>
  );
}
