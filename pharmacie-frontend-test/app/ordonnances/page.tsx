"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { listOrdonnances, listClients, API_BASE } from "@/lib/api";
import type { Ordonnance, Client } from "@/lib/types";
import { ButtonLink } from "@/components/Button";
import PageHeader from "@/components/PageHeader";
import Badge from "@/components/Badge";
import Table, { Th, Td, EmptyRow } from "@/components/Table";
import { IconFileText, IconPlus } from "@/components/icons";
import { cn } from "@/lib/cn";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T };

export default function OrdonnancesPage() {
  const [ordonnances, setOrdonnances] = useState<Charge<Ordonnance[]>>({ state: "loading", data: [] });
  const [clients, setClients] = useState<Record<string, Client>>({});

  useEffect(() => {
    listOrdonnances()
      .then((data) => setOrdonnances({ state: "ok", data }))
      .catch(() => setOrdonnances({ state: "error", data: [] }));
    listClients()
      .then((data) => setClients(Object.fromEntries(data.map((c) => [c.id, c]))))
      .catch(() => {});
  }, []);

  return (
    <div className="animate-fade-up">
      <PageHeader
        eyebrow="Prescriptions"
        title="Ordonnances"
        action={
          <ButtonLink href="/ordonnances/nouvelle">
            <IconPlus className="h-4 w-4" /> Nouvelle ordonnance
          </ButtonLink>
        }
      />

      <div className="mt-8">
        {ordonnances.state === "loading" && <p className="text-sm text-muted">Chargement…</p>}
        {ordonnances.state === "error" && (
          <p className="border-l-2 border-danger bg-danger/5 px-4 py-3 text-sm text-danger">
            Impossible de charger les ordonnances.
          </p>
        )}
        {ordonnances.state === "ok" && (
          <Table>
            <thead>
              <tr>
                <Th>Client</Th>
                <Th>Médecin</Th>
                <Th>Date</Th>
                <Th>Lignes</Th>
                <Th>Document</Th>
                <Th>Statut</Th>
              </tr>
            </thead>
            <tbody>
              {ordonnances.data.length === 0 && (
                <EmptyRow colSpan={6}>Aucune ordonnance pour l&apos;instant.</EmptyRow>
              )}
              {ordonnances.data.map((o, i) => {
                const client = clients[o.clientId];
                return (
                  <tr key={o.id} className={cn("transition-colors hover:bg-subtle", i !== 0 && "border-t border-line")}>
                    <Td>
                      {client ? (
                        <Link href={`/clients/${client.id}`} className="text-ink hover:text-brand">
                          {client.prenom ? `${client.prenom} ${client.nom}` : client.nom}
                        </Link>
                      ) : (
                        o.clientId
                      )}
                    </Td>
                    <Td>{o.medecinNom}</Td>
                    <Td>{new Date(o.dateEmission).toLocaleDateString("fr-FR")}</Td>
                    <Td>{o.lignes.length}</Td>
                    <Td>
                      {o.documentDisponible ? (
                        <a
                          href={`${API_BASE}/api/ordonnances/${o.id}/document`}
                          target="_blank"
                          rel="noreferrer"
                          className="inline-flex items-center gap-1.5 text-brand hover:underline"
                        >
                          <IconFileText className="h-4 w-4" /> {o.documentNom ?? "Voir"}
                        </a>
                      ) : (
                        <span className="text-xs text-muted">—</span>
                      )}
                    </Td>
                    <Td>
                      <Badge value={o.statut} />
                    </Td>
                  </tr>
                );
              })}
            </tbody>
          </Table>
        )}
      </div>
    </div>
  );
}
