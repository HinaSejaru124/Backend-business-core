"use client";

import { useEffect, useState } from "react";
import { adminPricing, adminDefinirTarif, ApiError, type AdminPricingRow } from "@/lib/api";
import { Button } from "@/components/Button";
import { cn } from "@/lib/cn";

/**
 * L'administrateur fixe réellement le prix/quota de chaque plan — persisté en base
 * (POST /v1/admin/pricing/{code}), pris en compte immédiatement par toute la plateforme
 * (PlanCatalogue le relit). Aucune simulation : changer PRO ici change vraiment son prix facturé.
 */
export default function PricingEditor() {
  const [plans, setPlans] = useState<AdminPricingRow[] | null>(null);
  const [edition, setEdition] = useState<string | null>(null);
  const [quota, setQuota] = useState("");
  const [prix, setPrix] = useState("");
  const [devise, setDevise] = useState("XAF");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: "ok" | "err"; texte: string } | null>(null);

  function charger() {
    adminPricing().then(setPlans).catch(() => setPlans([]));
  }
  useEffect(charger, []);

  function ouvrir(p: AdminPricingRow) {
    setEdition(p.code);
    setQuota(p.illimite ? "-1" : String(p.quotaMensuel));
    setPrix(String(p.prixMensuel));
    setDevise(p.devise);
    setMessage(null);
  }

  async function enregistrer(code: string) {
    setSaving(true);
    setMessage(null);
    try {
      await adminDefinirTarif(code, Number(quota), Number(prix), devise);
      setMessage({ type: "ok", texte: `Tarif de ${code} mis à jour.` });
      setEdition(null);
      charger();
    } catch (e) {
      setMessage({ type: "err", texte: e instanceof ApiError ? e.detail || e.title : "Mise à jour impossible." });
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="mt-10 rounded-xl border border-line bg-white p-6 shadow-card">
      <h2 className="font-display text-lg font-semibold text-ink">Fixer les tarifs</h2>
      <p className="mt-1 text-sm text-muted">
        Vous décidez du prix et du quota de chaque plan — effet immédiat et persisté sur toute la plateforme.
      </p>

      {message && (
        <div className={cn("mt-4 rounded-lg border-l-2 px-4 py-2.5 text-sm", message.type === "ok" ? "border-ok bg-ok/5 text-ink" : "border-danger bg-danger/5 text-danger")}>
          {message.texte}
        </div>
      )}

      <div className="mt-5 space-y-3">
        {plans === null && <p className="text-sm text-muted">Chargement…</p>}
        {plans?.map((p) => (
          <div key={p.code} className="rounded-lg border border-line p-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <span className="font-display text-[15px] font-bold text-ink">{p.code}</span>
                <span className="text-sm text-muted">
                  {p.prixMensuel > 0 ? `${p.prixMensuel.toLocaleString("fr-FR")} ${p.devise}/mois` : "Gratuit"} ·{" "}
                  {p.illimite ? "Illimité" : `${p.quotaMensuel.toLocaleString("fr-FR")} req/mois`}
                </span>
              </div>
              {edition !== p.code && (
                <Button variant="secondary" size="sm" onClick={() => ouvrir(p)}>Modifier</Button>
              )}
            </div>

            {edition === p.code && (
              <div className="mt-4 grid gap-3 border-t border-line pt-4 sm:grid-cols-4">
                <label className="block">
                  <span className="mb-1 block text-[12px] font-medium text-ink">Quota mensuel (-1 = illimité)</span>
                  <input value={quota} onChange={(e) => setQuota(e.target.value)} type="number"
                    className="h-10 w-full rounded-lg border border-line bg-white px-3 text-sm outline-none focus:border-brand" />
                </label>
                <label className="block">
                  <span className="mb-1 block text-[12px] font-medium text-ink">Prix mensuel</span>
                  <input value={prix} onChange={(e) => setPrix(e.target.value)} type="number" min={0}
                    className="h-10 w-full rounded-lg border border-line bg-white px-3 text-sm outline-none focus:border-brand" />
                </label>
                <label className="block">
                  <span className="mb-1 block text-[12px] font-medium text-ink">Devise</span>
                  <input value={devise} onChange={(e) => setDevise(e.target.value)}
                    className="h-10 w-full rounded-lg border border-line bg-white px-3 text-sm outline-none focus:border-brand" />
                </label>
                <div className="flex items-end gap-2">
                  <Button size="sm" onClick={() => enregistrer(p.code)} disabled={saving}>
                    {saving ? "…" : "Enregistrer"}
                  </Button>
                  <Button variant="secondary" size="sm" onClick={() => setEdition(null)} disabled={saving}>Annuler</Button>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
