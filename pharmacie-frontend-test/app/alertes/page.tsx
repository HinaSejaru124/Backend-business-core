"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { listAlertesStock } from "@/lib/api";
import type { Medicament } from "@/lib/types";
import { ButtonLink } from "@/components/Button";
import PageHeader from "@/components/PageHeader";
import StockBadge from "@/components/StockBadge";
import Table, { Th, Td, EmptyRow } from "@/components/Table";
import { IconCheck, IconPlus } from "@/components/icons";
import { cn } from "@/lib/cn";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T };

export default function AlertesPage() {
  const [alertes, setAlertes] = useState<Charge<Medicament[]>>({ state: "loading", data: [] });

  useEffect(() => {
    listAlertesStock()
      .then((data) => setAlertes({ state: "ok", data }))
      .catch(() => setAlertes({ state: "error", data: [] }));
  }, []);

  return (
    <div className="animate-fade-up">
      <PageHeader
        eyebrow="Surveillance"
        title="Alertes stock"
        description="Médicaments dont le stock local est au ou sous le seuil d'alerte."
      />

      <div className="mt-8">
        {alertes.state === "loading" && <p className="text-sm text-muted">Chargement…</p>}
        {alertes.state === "error" && (
          <p className="border-l-2 border-danger bg-danger/5 px-4 py-3 text-sm text-danger">
            Impossible de charger les alertes.
          </p>
        )}
        {alertes.state === "ok" && alertes.data.length === 0 && (
          <div className="flex items-center gap-3 border border-dashed border-line bg-white p-6">
            <IconCheck className="h-5 w-5 text-ok" />
            <p className="text-sm text-muted">Aucune alerte — tous les stocks sont au-dessus de leur seuil.</p>
          </div>
        )}
        {alertes.state === "ok" && alertes.data.length > 0 && (
          <Table>
            <thead>
              <tr>
                <Th>Médicament</Th>
                <Th>Stock</Th>
                <Th>Seuil</Th>
                <Th></Th>
              </tr>
            </thead>
            <tbody>
              {alertes.data.map((m, i) => (
                <tr key={m.id} className={cn("transition-colors hover:bg-subtle", i !== 0 && "border-t border-line")}>
                  <Td>
                    <Link href={`/medicaments/${m.id}`} className="font-medium text-ink hover:text-brand">
                      {m.nom}
                    </Link>
                  </Td>
                  <Td>
                    <StockBadge stock={m.stockActuel} seuil={m.seuilAlerte} />
                  </Td>
                  <Td>{m.seuilAlerte}</Td>
                  <Td>
                    <ButtonLink href="/commandes/nouvelle" size="sm" variant="secondary">
                      <IconPlus className="h-3.5 w-3.5" /> Commander
                    </ButtonLink>
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
