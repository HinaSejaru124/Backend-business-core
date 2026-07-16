"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { listCommandes, listFournisseurs } from "@/lib/api";
import type { CommandeFournisseur, Fournisseur } from "@/lib/types";
import { ButtonLink } from "@/components/Button";
import PageHeader from "@/components/PageHeader";
import Badge from "@/components/Badge";
import Table, { Th, Td, EmptyRow } from "@/components/Table";
import { IconPlus } from "@/components/icons";
import { cn } from "@/lib/cn";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T };

export default function CommandesPage() {
  const [commandes, setCommandes] = useState<Charge<CommandeFournisseur[]>>({ state: "loading", data: [] });
  const [fournisseurs, setFournisseurs] = useState<Record<string, Fournisseur>>({});

  useEffect(() => {
    listCommandes()
      .then((data) => setCommandes({ state: "ok", data }))
      .catch(() => setCommandes({ state: "error", data: [] }));
    listFournisseurs()
      .then((data) => setFournisseurs(Object.fromEntries(data.map((f) => [f.id, f]))))
      .catch(() => {});
  }, []);

  return (
    <div className="animate-fade-up">
      <PageHeader
        eyebrow="Approvisionnement"
        title="Commandes fournisseurs"
        action={
          <ButtonLink href="/commandes/nouvelle">
            <IconPlus className="h-4 w-4" /> Nouvelle commande
          </ButtonLink>
        }
      />

      <div className="mt-8">
        {commandes.state === "loading" && <p className="text-sm text-muted">Chargement…</p>}
        {commandes.state === "error" && (
          <p className="border-l-2 border-danger bg-danger/5 px-4 py-3 text-sm text-danger">
            Impossible de charger les commandes.
          </p>
        )}
        {commandes.state === "ok" && (
          <Table>
            <thead>
              <tr>
                <Th>Fournisseur</Th>
                <Th>Date commande</Th>
                <Th>Lignes</Th>
                <Th>Statut</Th>
              </tr>
            </thead>
            <tbody>
              {commandes.data.length === 0 && (
                <EmptyRow colSpan={4}>Aucune commande pour l&apos;instant.</EmptyRow>
              )}
              {commandes.data.map((c, i) => (
                <tr key={c.id} className={cn("transition-colors hover:bg-subtle", i !== 0 && "border-t border-line")}>
                  <Td>
                    <Link href={`/commandes/${c.id}`} className="font-medium text-ink hover:text-brand">
                      {fournisseurs[c.fournisseurId]?.nom ?? c.fournisseurId}
                    </Link>
                  </Td>
                  <Td>{new Date(c.dateCommande).toLocaleDateString("fr-FR")}</Td>
                  <Td>{c.lignes.length}</Td>
                  <Td>
                    <Badge value={c.statut} />
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
