"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { listMedicaments, listFournisseurs, creerMedicament, ApiError } from "@/lib/api";
import type { Medicament, Fournisseur } from "@/lib/types";
import { Button } from "@/components/Button";
import Field from "@/components/Field";
import Select from "@/components/Select";
import SidePanel from "@/components/SidePanel";
import PageHeader from "@/components/PageHeader";
import StockBadge from "@/components/StockBadge";
import Badge from "@/components/Badge";
import Table, { Th, Td, EmptyRow } from "@/components/Table";
import { IconPlus } from "@/components/icons";
import { useToast } from "@/components/Toast";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T };

const FORM_ID = "form-nouveau-medicament";

export default function MedicamentsPage() {
  const [medicaments, setMedicaments] = useState<Charge<Medicament[]>>({ state: "loading", data: [] });
  const [fournisseurs, setFournisseurs] = useState<Fournisseur[]>([]);
  const [open, setOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const toast = useToast();

  function recharger() {
    listMedicaments()
      .then((data) => setMedicaments({ state: "ok", data }))
      .catch(() => setMedicaments({ state: "error", data: [] }));
  }

  useEffect(() => {
    recharger();
    listFournisseurs().then(setFournisseurs).catch(() => {});
  }, []);

  return (
    <div className="animate-fade-up">
      <PageHeader
        eyebrow="Catalogue"
        title="Médicaments"
        description="Chaque médicament créé ici déclare une vraie Offre côté Business Core."
        action={
          <Button onClick={() => setOpen(true)}>
            <IconPlus className="h-4 w-4" /> Nouveau médicament
          </Button>
        }
      />

      <div className="mt-8">
        {medicaments.state === "loading" && <p className="text-sm text-muted">Chargement…</p>}
        {medicaments.state === "error" && (
          <p className="border-l-2 border-danger bg-danger/5 px-4 py-3 text-sm text-danger">
            Impossible de charger le catalogue.
          </p>
        )}
        {medicaments.state === "ok" && (
          <Table>
            <thead>
              <tr>
                <Th>Nom</Th>
                <Th>DCI</Th>
                <Th>Catégorie</Th>
                <Th>Prix</Th>
                <Th>Stock</Th>
                <Th>Statut</Th>
              </tr>
            </thead>
            <tbody>
              {medicaments.data.length === 0 && (
                <EmptyRow colSpan={6}>
                  Aucun médicament pour l&apos;instant — créez le premier avec « Nouveau médicament ».
                </EmptyRow>
              )}
              {medicaments.data.map((m, i) => (
                <tr key={m.id} className={i !== 0 ? "border-t border-line" : ""}>
                  <Td>
                    <Link href={`/medicaments/${m.id}`} className="font-medium text-ink hover:text-brand">
                      {m.nom}
                    </Link>
                  </Td>
                  <Td>{m.dci ?? "—"}</Td>
                  <Td>
                    {m.ordonnanceRequise ? (
                      <Badge value="EXPIREE" />
                    ) : (
                      <span className="text-xs text-muted">Libre</span>
                    )}
                    {m.ordonnanceRequise && (
                      <span className="ml-1.5 text-xs text-danger">Sur ordonnance</span>
                    )}
                  </Td>
                  <Td className="font-mono">{m.prixUnitaire.toLocaleString("fr-FR")} XAF</Td>
                  <Td>
                    <StockBadge stock={m.stockActuel} seuil={m.seuilAlerte} />
                  </Td>
                  <Td>
                    <Badge value={m.statut} />
                  </Td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </div>

      <SidePanel
        title="Nouveau médicament"
        subtitle="Déclare une vraie Offre côté Business Core"
        open={open}
        onClose={() => setOpen(false)}
        formId={FORM_ID}
        submitLabel="Créer le médicament"
        submitting={submitting}
      >
        <NouveauMedicamentForm
          fournisseurs={fournisseurs}
          onSubmitting={setSubmitting}
          onSuccess={() => {
            setOpen(false);
            recharger();
            toast("success", "Médicament créé — Offre Business Core déclarée avec succès.");
          }}
          onError={(msg) => toast("error", msg)}
        />
      </SidePanel>
    </div>
  );
}

function NouveauMedicamentForm({
  fournisseurs,
  onSubmitting,
  onSuccess,
  onError,
}: {
  fournisseurs: Fournisseur[];
  onSubmitting: (v: boolean) => void;
  onSuccess: () => void;
  onError: (msg: string) => void;
}) {
  const [nom, setNom] = useState("");
  const [dci, setDci] = useState("");
  const [formeGalenique, setFormeGalenique] = useState("");
  const [codeCip, setCodeCip] = useState("");
  const [categorie, setCategorie] = useState<"medicament_libre" | "medicament_prescription">(
    "medicament_libre"
  );
  const [prixUnitaire, setPrixUnitaire] = useState("");
  const [stockInitial, setStockInitial] = useState("0");
  const [seuilAlerte, setSeuilAlerte] = useState("10");
  const [fournisseurId, setFournisseurId] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    onSubmitting(true);
    try {
      await creerMedicament({
        nom,
        dci: dci || undefined,
        formeGalenique: formeGalenique || undefined,
        codeCip: codeCip || undefined,
        categorie,
        ordonnanceRequise: categorie === "medicament_prescription",
        prixUnitaire: Number(prixUnitaire),
        stockInitial: Number(stockInitial),
        seuilAlerte: Number(seuilAlerte),
        fournisseurId: fournisseurId || undefined,
      });
      onSuccess();
    } catch (err) {
      const msg = err instanceof ApiError ? err.detail || err.title : "Création impossible.";
      setError(msg);
      onError(msg);
    } finally {
      onSubmitting(false);
    }
  }

  return (
    <form id="form-nouveau-medicament" onSubmit={onSubmit} className="space-y-5">
      <Field label="Nom" id="nom" value={nom} onChange={(e) => setNom(e.target.value)} required />
      <div className="grid grid-cols-2 gap-5">
        <Field label="DCI (molécule)" id="dci" value={dci} onChange={(e) => setDci(e.target.value)} />
        <Field
          label="Forme galénique"
          id="forme"
          placeholder="Comprimé, sirop…"
          value={formeGalenique}
          onChange={(e) => setFormeGalenique(e.target.value)}
        />
      </div>
      <div className="grid grid-cols-2 gap-5">
        <Field label="Code CIP" id="cip" value={codeCip} onChange={(e) => setCodeCip(e.target.value)} />
        <Select
          label="Catégorie"
          id="categorie"
          value={categorie}
          onChange={(e) => setCategorie(e.target.value as typeof categorie)}
        >
          <option value="medicament_libre">Libre</option>
          <option value="medicament_prescription">Sur ordonnance</option>
        </Select>
      </div>
      <div className="grid grid-cols-3 gap-5">
        <Field
          label="Prix (XAF)"
          id="prix"
          type="number"
          min={0}
          value={prixUnitaire}
          onChange={(e) => setPrixUnitaire(e.target.value)}
          required
        />
        <Field
          label="Stock initial"
          id="stock"
          type="number"
          min={0}
          value={stockInitial}
          onChange={(e) => setStockInitial(e.target.value)}
        />
        <Field
          label="Seuil d'alerte"
          id="seuil"
          type="number"
          min={0}
          value={seuilAlerte}
          onChange={(e) => setSeuilAlerte(e.target.value)}
        />
      </div>
      <Select
        label="Fournisseur (optionnel)"
        id="fournisseur"
        value={fournisseurId}
        onChange={(e) => setFournisseurId(e.target.value)}
      >
        <option value="">—</option>
        {fournisseurs.map((f) => (
          <option key={f.id} value={f.id}>
            {f.nom}
          </option>
        ))}
      </Select>
      {error && <p className="border-l-2 border-danger bg-danger/5 px-3 py-2 text-sm text-danger">{error}</p>}
    </form>
  );
}
