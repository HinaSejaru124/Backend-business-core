"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { getMedicament, ApiError } from "@/lib/api";
import type { Medicament } from "@/lib/types";
import Card from "@/components/Card";
import Badge from "@/components/Badge";
import StockBadge from "@/components/StockBadge";

type Charge = { state: "loading" | "error" | "ok"; data: Medicament | null; message?: string };

export default function MedicamentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [charge, setCharge] = useState<Charge>({ state: "loading", data: null });

  useEffect(() => {
    getMedicament(id)
      .then((data) => setCharge({ state: "ok", data }))
      .catch((err) =>
        setCharge({
          state: "error",
          data: null,
          message: err instanceof ApiError ? err.detail || err.title : "Chargement impossible.",
        })
      );
  }, [id]);

  if (charge.state === "loading") return <p className="text-sm text-muted">Chargement…</p>;
  if (charge.state === "error" || !charge.data) {
    return (
      <p className="border-l-2 border-danger bg-danger/5 px-4 py-3 text-sm text-danger">
        {charge.message ?? "Médicament introuvable."}
      </p>
    );
  }

  const m = charge.data;

  return (
    <div className="animate-fade-up max-w-3xl">
      <Link href="/medicaments" className="text-sm text-muted hover:text-ink">
        ← Retour au catalogue
      </Link>
      <div className="mt-4 flex items-start justify-between border-b border-line pb-6">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">{m.nom}</h1>
          <p className="mt-1 text-sm text-muted">{m.dci ?? "DCI non renseignée"}</p>
        </div>
        <Badge value={m.statut} />
      </div>

      <div className="mt-6 grid gap-px border border-line bg-line sm:grid-cols-2">
        <Info label="Forme galénique" value={m.formeGalenique ?? "—"} />
        <Info label="Code CIP" value={m.codeCip ?? "—"} />
        <Info label="Catégorie" value={m.categorie} mono />
        <Info label="Ordonnance requise" value={m.ordonnanceRequise ? "Oui" : "Non"} />
        <Info label="Prix unitaire" value={`${m.prixUnitaire.toLocaleString("fr-FR")} XAF`} />
        <Info label="Seuil d'alerte" value={String(m.seuilAlerte)} />
        <Info label="Offre Business Core" value={m.offreId} mono small />
      </div>

      <div className="mt-6 flex items-center gap-3">
        <span className="text-sm text-muted">Stock actuel :</span>
        <StockBadge stock={m.stockActuel} seuil={m.seuilAlerte} />
      </div>

      <Card title="Historique des mouvements de stock" className="mt-8">
        <p className="text-sm text-muted">
          Le suivi détaillé des mouvements (ventes, réceptions) n&apos;est pas encore tracé en base —
          seul le solde courant (<strong>{m.stockActuel}</strong>) est connu. Un historique complet
          nécessiterait une table de mouvements dédiée, pas encore construite dans cette version de
          PharmaCore.
        </p>
      </Card>
    </div>
  );
}

function Info({ label, value, mono, small }: { label: string; value: string; mono?: boolean; small?: boolean }) {
  return (
    <div className="bg-white p-5">
      <div className="text-xs uppercase tracking-wider text-muted">{label}</div>
      <div className={`mt-1.5 truncate ${mono ? "font-mono" : ""} ${small ? "text-xs" : "text-sm"} text-ink`}>
        {value}
      </div>
    </div>
  );
}
