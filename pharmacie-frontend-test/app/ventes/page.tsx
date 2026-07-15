"use client";

import { useEffect, useState } from "react";
import { listVentes } from "@/lib/api";
import type { Vente } from "@/lib/types";
import PageHeader from "@/components/PageHeader";
import BlocageKernel from "@/components/BlocageKernel";
import Badge from "@/components/Badge";
import Table, { Th, Td, EmptyRow } from "@/components/Table";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T };

function fmtDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString("fr-FR", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

/**
 * Historique des ventes — GET /api/ventes (lecture locale PharmaCore, aucun appel Kernel). Une vente
 * n'y apparaît que si sa saga Business Core a abouti (COMPLETEE) : tant qu'ENGAGER_STOCK échoue côté
 * Kernel, cette liste reste vide — c'est un état honnête, pas un bug d'affichage (cf. BlocageKernel).
 */
export default function VentesPage() {
  const [ventes, setVentes] = useState<Charge<Vente[]>>({ state: "loading", data: [] });

  useEffect(() => {
    listVentes()
      .then((data) => setVentes({ state: "ok", data }))
      .catch(() => setVentes({ state: "error", data: [] }));
  }, []);

  return (
    <div className="animate-fade-up">
      <PageHeader
        eyebrow="Historique"
        title="Ventes"
        description="Liste des ventes dont la vente a réellement abouti côté Business Core."
      />

      <div className="mt-6">
        <BlocageKernel contexte="L'encaissement" />
      </div>

      <div className="mt-8">
        {ventes.state === "loading" && <p className="text-sm text-muted">Chargement…</p>}
        {ventes.state === "error" && (
          <p className="border-l-2 border-danger bg-danger/5 px-4 py-3 text-sm text-danger">
            Impossible de charger l&apos;historique des ventes.
          </p>
        )}
        {ventes.state === "ok" && (
          <Table>
            <thead>
              <tr>
                <Th>Date</Th>
                <Th>Articles</Th>
                <Th>Montant</Th>
                <Th>Paiement</Th>
                <Th>Statut</Th>
                <Th>Trace</Th>
              </tr>
            </thead>
            <tbody>
              {ventes.data.length === 0 && (
                <EmptyRow colSpan={6}>
                  Aucune vente aboutie pour l&apos;instant — voir le bandeau ci-dessus.
                </EmptyRow>
              )}
              {ventes.data.map((v, i) => (
                <tr key={v.id} className={i !== 0 ? "border-t border-line" : ""}>
                  <Td>{fmtDate(v.creeLe)}</Td>
                  <Td>
                    {v.lignes.reduce((n, l) => n + l.quantite, 0)} article
                    {v.lignes.reduce((n, l) => n + l.quantite, 0) > 1 ? "s" : ""}
                  </Td>
                  <Td className="font-mono">
                    {v.montantTotal.toLocaleString("fr-FR")} {v.devise}
                  </Td>
                  <Td>{v.modePaiement}</Td>
                  <Td>
                    <Badge value={v.statutBcaas} />
                  </Td>
                  <Td className="font-mono text-xs text-muted">
                    {v.traceId ? v.traceId.slice(0, 8) + "…" : "—"}
                  </Td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </div>
    </div>
  );
}
