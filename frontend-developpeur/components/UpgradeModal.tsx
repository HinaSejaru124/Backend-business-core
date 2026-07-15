"use client";

import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { getPlans, upgradePlan, ApiError, type Plan } from "@/lib/api";
import { Button } from "@/components/Button";
import { IconCheck, IconClose } from "@/components/icons";
import { cn } from "@/lib/cn";

function fmt(n: number): string {
  return n.toLocaleString("fr-FR");
}

/**
 * Fenêtre "Changer de plan" — VRAIE modal plein écran, rendue via portail directement sur
 * `document.body` (échappe à tout ancêtre à overflow/transform contraint, garantit une couverture
 * intégrale de l'application — sidebar et navbar incluses — quel que soit l'endroit d'où elle est
 * ouverte). Le défilement de la page derrière est verrouillé tant qu'elle est ouverte.
 *
 * Mêmes appels réels que /console/pricing (GET /v1/plans, POST /v1/plan/upgrade), juste présentés en
 * survol plutôt qu'en page dédiée. Deux étapes honnêtes : (1) choisir un plan réel, (2) confirmer — il
 * n'y a pas de vrai formulaire de carte bancaire à afficher puisque le paiement est simulé côté
 * plateforme (Kernel Core indisponible) ; on le dit plutôt que d'inventer un écran de paiement qui ne
 * ferait rien de réel.
 */
export default function UpgradeModal({
  planActuel,
  onClose,
  onChanged,
}: {
  planActuel: string | null;
  onClose: () => void;
  onChanged: () => void;
}) {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [selection, setSelection] = useState<Plan | null>(null);
  const [enCours, setEnCours] = useState(false);
  const [resultat, setResultat] = useState<{ type: "ok" | "err"; texte: string } | null>(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    // Un frame avant de passer visible=true pour laisser le navigateur peindre l'état initial
    // (opacité 0 / échelle réduite), sinon la transition d'entrée ne joue pas.
    const id = requestAnimationFrame(() => setVisible(true));
    return () => cancelAnimationFrame(id);
  }, []);

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  useEffect(() => {
    getPlans()
      .then(setPlans)
      .catch(() => setPlans([]))
      .finally(() => setLoading(false));
  }, []);

  function fermer() {
    setVisible(false);
    window.setTimeout(onClose, 220);
  }

  useEffect(() => {
    function onEsc(e: KeyboardEvent) {
      if (e.key === "Escape") fermer();
    }
    window.addEventListener("keydown", onEsc);
    return () => window.removeEventListener("keydown", onEsc);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function confirmer() {
    if (!selection) return;
    setEnCours(true);
    setResultat(null);
    try {
      const res = await upgradePlan(selection.code);
      if (res.statut === "CONFIRME") {
        setResultat({ type: "ok", texte: `Plan changé : vous êtes maintenant en ${res.plan}.` });
        onChanged();
        // Laisse le message de confirmation s'afficher un court instant avant de refermer
        // avec l'animation (plutôt qu'une disparition brutale).
        window.setTimeout(fermer, 900);
      } else {
        setResultat({ type: "ok", texte: `Paiement en attente pour activer le plan ${selection.code}.` });
      }
    } catch (e) {
      setResultat({
        type: "err",
        texte: e instanceof ApiError ? e.detail || e.title : "Changement de plan impossible.",
      });
    } finally {
      setEnCours(false);
    }
  }

  const modal = (
    <div
      className={cn(
        "fixed inset-0 z-[200] grid place-items-center bg-ink/60 p-4 backdrop-blur-md transition-opacity duration-200 ease-out",
        visible ? "opacity-100" : "opacity-0"
      )}
      onClick={fermer}
    >
      <div
        className={cn(
          "max-h-[calc(100vh-2rem)] w-full max-w-3xl overflow-y-auto border border-line bg-white p-6 shadow-glow transition-all duration-220 ease-out md:p-8",
          visible ? "translate-y-0 scale-100 opacity-100" : "translate-y-3 scale-[0.97] opacity-0"
        )}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <div className="text-[11px] font-semibold uppercase tracking-wider text-brand">Facturation</div>
            <h2 className="mt-1.5 font-display text-2xl font-bold text-ink">
              {selection ? "Confirmer le changement" : "Choisir un plan"}
            </h2>
          </div>
          <button
            onClick={fermer}
            className="grid h-9 w-9 flex-none place-items-center text-muted transition-colors hover:bg-subtle hover:text-ink"
            aria-label="Fermer"
          >
            <IconClose className="h-4 w-4" />
          </button>
        </div>

        {resultat && (
          <div
            className={cn(
              "mt-5 border-l-2 px-4 py-3 text-sm",
              resultat.type === "ok" ? "border-ok bg-ok-tint text-ink" : "border-danger bg-danger/5 text-danger"
            )}
          >
            {resultat.texte}
          </div>
        )}

        {/* Étape 1 — grille des plans réels */}
        {!selection && (
          <div className="mt-6">
            {loading && <div className="text-sm text-muted">Chargement des plans…</div>}
            {!loading && (
              <div className="grid gap-4 md:grid-cols-3">
                {plans.map((p) => {
                  const actuel = planActuel === p.code;
                  return (
                    <div
                      key={p.code}
                      className={cn(
                        "flex flex-col border bg-white p-5",
                        actuel ? "border-brand shadow-card" : "border-line"
                      )}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-display text-lg font-bold text-ink">{p.code}</span>
                        {actuel && (
                          <span className="border border-brand/35 bg-brand/5 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-brand">
                            Actuel
                          </span>
                        )}
                      </div>
                      <div className="mt-3 font-display text-2xl font-bold text-ink">
                        {p.illimite ? "Illimité" : fmt(p.quotaMensuel)}
                        {!p.illimite && <span className="text-sm font-normal text-muted"> req/mois</span>}
                      </div>
                      <div className="mt-1 text-xs text-muted">
                        {p.prixMensuel > 0 ? `${fmt(p.prixMensuel)} ${p.devise}/mois` : "Prix géré par Kernel Core"}
                      </div>
                      <div className="mt-4 flex-1" />
                      <Button
                        variant={actuel ? "secondary" : "primary"}
                        size="sm"
                        disabled={actuel}
                        className="w-full"
                        onClick={() => setSelection(p)}
                      >
                        {actuel ? "Votre plan" : `Choisir ${p.code}`}
                      </Button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* Étape 2 — confirmation honnête (pas de faux formulaire de carte) */}
        {selection && (
          <div className="mt-6">
            <div className="border border-line bg-subtle p-5">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-xs font-semibold uppercase tracking-wider text-muted">Nouveau plan</div>
                  <div className="mt-1 font-display text-xl font-bold text-ink">{selection.code}</div>
                </div>
                <div className="text-right">
                  <div className="font-display text-lg font-bold text-ink">
                    {selection.illimite ? "Illimité" : `${fmt(selection.quotaMensuel)} req/mois`}
                  </div>
                  <div className="text-xs text-muted">
                    {selection.prixMensuel > 0 ? `${fmt(selection.prixMensuel)} ${selection.devise}/mois` : "Prix géré par Kernel Core"}
                  </div>
                </div>
              </div>
              <ul className="mt-4 space-y-2 border-t border-line pt-4 text-sm text-body">
                <li className="flex items-center gap-2">
                  <IconCheck className="h-4 w-4 text-ok" /> Quota appliqué immédiatement sur votre compte
                </li>
                <li className="flex items-center gap-2">
                  <IconCheck className="h-4 w-4 text-ok" /> Vos clés d&apos;API existantes restent inchangées
                </li>
              </ul>
              <p className="mt-4 border-t border-line pt-4 text-xs text-muted">
                Le paiement réel est géré par Kernel Core, actuellement indisponible — le changement de
                plan est donc confirmé directement, sans prélèvement, pour cette démonstration.
              </p>
            </div>

            <div className="mt-5 flex gap-3">
              <Button variant="secondary" className="flex-1" onClick={() => setSelection(null)} disabled={enCours}>
                Retour
              </Button>
              <Button className="flex-1" onClick={confirmer} disabled={enCours}>
                {enCours ? "Confirmation…" : `Confirmer le passage à ${selection.code}`}
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );

  if (typeof document === "undefined") return null;
  return createPortal(modal, document.body);
}
