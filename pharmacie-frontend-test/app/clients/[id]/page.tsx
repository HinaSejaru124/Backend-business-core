"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { getClient, ApiError } from "@/lib/api";
import type { Client } from "@/lib/types";
import Card from "@/components/Card";

type Charge = { state: "loading" | "error" | "ok"; data: Client | null; message?: string };

export default function ClientDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [charge, setCharge] = useState<Charge>({ state: "loading", data: null });

  useEffect(() => {
    getClient(id)
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
        {charge.message ?? "Client introuvable."}
      </p>
    );
  }

  const c = charge.data;

  return (
    <div className="animate-fade-up max-w-2xl">
      <Link href="/clients" className="text-sm text-muted hover:text-ink">
        ← Retour aux clients
      </Link>
      <h1 className="mt-4 border-b border-line pb-6 font-display text-2xl font-bold text-ink">
        {c.prenom ? `${c.prenom} ${c.nom}` : c.nom}
      </h1>

      <div className="mt-6 grid gap-px border border-line bg-line sm:grid-cols-2">
        <Info label="Téléphone" value={c.telephone ?? "—"} />
        <Info label="E-mail" value={c.email ?? "—"} />
        <Info label="Adresse" value={c.adresse ?? "—"} />
        <Info label="Client depuis" value={new Date(c.creeLe).toLocaleDateString("fr-FR")} />
      </div>

      <Card title="Historique des achats" className="mt-8">
        <p className="text-sm text-muted">
          L&apos;écran Vente n&apos;est pas encore disponible (cf. <Link href="/vente" className="underline">page Vente</Link>) —
          aucun achat ne peut donc encore exister pour ce client.
        </p>
      </Card>
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-white p-5">
      <div className="text-xs uppercase tracking-wider text-muted">{label}</div>
      <div className="mt-1.5 text-sm text-ink">{value}</div>
    </div>
  );
}
