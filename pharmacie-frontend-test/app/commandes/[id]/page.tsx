"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { getCommande, getFournisseur, receptionnerCommande, ApiError } from "@/lib/api";
import type { CommandeFournisseur, Fournisseur } from "@/lib/types";
import { Button } from "@/components/Button";
import Badge from "@/components/Badge";
import Table, { Th, Td } from "@/components/Table";
import { useToast } from "@/components/Toast";

type Charge = { state: "loading" | "error" | "ok"; data: CommandeFournisseur | null; message?: string };

export default function CommandeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const toast = useToast();
  const [charge, setCharge] = useState<Charge>({ state: "loading", data: null });
  const [fournisseur, setFournisseur] = useState<Fournisseur | null>(null);
  const [receptionLoading, setReceptionLoading] = useState(false);

  function recharger() {
    getCommande(id)
      .then((data) => {
        setCharge({ state: "ok", data });
        getFournisseur(data.fournisseurId).then(setFournisseur).catch(() => {});
      })
      .catch((err) =>
        setCharge({
          state: "error",
          data: null,
          message: err instanceof ApiError ? err.detail || err.title : "Chargement impossible.",
        })
      );
  }

  useEffect(recharger, [id]);

  async function onReceptionner() {
    setReceptionLoading(true);
    try {
      await receptionnerCommande(id);
      toast("success", "Commande réceptionnée — stock local mis à jour.");
      recharger();
    } catch (err) {
      toast("error", err instanceof ApiError ? err.detail || err.title : "Réception impossible.");
    } finally {
      setReceptionLoading(false);
    }
  }

  if (charge.state === "loading") return <p className="text-sm text-muted">Chargement…</p>;
  if (charge.state === "error" || !charge.data) {
    return (
      <p className="border-l-2 border-danger bg-danger/5 px-4 py-3 text-sm text-danger">
        {charge.message ?? "Commande introuvable."}
      </p>
    );
  }

  const c = charge.data;

  return (
    <div className="animate-fade-up max-w-2xl">
      <Link href="/commandes" className="text-sm text-muted hover:text-ink">
        ← Retour aux commandes
      </Link>

      <div className="mt-4 flex items-start justify-between border-b border-line pb-6">
        <div>
          <h1 className="font-display text-2xl font-bold text-ink">
            Commande — {fournisseur?.nom ?? c.fournisseurId}
          </h1>
          <p className="mt-1 text-sm text-muted">
            Passée le {new Date(c.dateCommande).toLocaleDateString("fr-FR")}
          </p>
        </div>
        <Badge value={c.statut} />
      </div>

      <div className="mt-6">
        <Table>
          <thead>
            <tr>
              <Th>Médicament</Th>
              <Th>Qté commandée</Th>
              <Th>Qté reçue</Th>
              <Th>Prix d&apos;achat</Th>
            </tr>
          </thead>
          <tbody>
            {c.lignes.map((l, i) => (
              <tr key={l.id} className={i !== 0 ? "border-t border-line" : ""}>
                <Td className="font-mono text-xs">{l.medicamentId}</Td>
                <Td>{l.quantiteCommandee}</Td>
                <Td>{l.quantiteRecue ?? "—"}</Td>
                <Td>{l.prixUnitaireAchat.toLocaleString("fr-FR")} XAF</Td>
              </tr>
            ))}
          </tbody>
        </Table>
      </div>

      {c.statut !== "RECUE" ? (
        <div className="mt-6 border-l-2 border-brand bg-brand-tint p-4">
          <p className="text-sm text-ink">
            La réception incrémente le <strong>stock local</strong> de chaque médicament de la quantité
            commandée. Cette réception met à jour le stock PharmaCore ; le stock côté plateforme
            Business Core n&apos;est pas modifiable via l&apos;API à ce jour (cf. limite documentée dans
            SPIKE-RESULTATS.md).
          </p>
          <Button className="mt-3" onClick={onReceptionner} disabled={receptionLoading}>
            {receptionLoading ? "Réception…" : "Réceptionner la commande"}
          </Button>
        </div>
      ) : (
        <p className="mt-6 border-l-2 border-ok bg-ok/5 px-4 py-3 text-sm text-ink">
          Commande réceptionnée le{" "}
          {c.dateReceptionReelle ? new Date(c.dateReceptionReelle).toLocaleDateString("fr-FR") : "—"}.
        </p>
      )}
    </div>
  );
}
