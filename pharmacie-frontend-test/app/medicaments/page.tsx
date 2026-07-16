"use client";

import { useEffect, useMemo, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { listMedicaments, creerMedicamentAdmin, ApiError } from "@/lib/api";
import type { Medicament } from "@/lib/types";
import { Button } from "@/components/Button";
import Field from "@/components/Field";
import Select from "@/components/Select";
import SidePanel from "@/components/SidePanel";
import PageHeader from "@/components/PageHeader";
import StockBadge from "@/components/StockBadge";
import Badge from "@/components/Badge";
import Table, { Th, Td, EmptyRow } from "@/components/Table";
import { IconClose, IconPlus, IconSearch } from "@/components/icons";
import { useToast } from "@/components/Toast";
import { cn } from "@/lib/cn";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T };

const FORM_ID = "form-nouveau-medicament";

/**
 * Catalogue — le titulaire est déjà pharmacien : il crée un médicament ici même, dans le panneau
 * coulissant, sans passer par un « espace admin » séparé. La création déclare l'Offre Business Core
 * (design-time, JWT) PUIS la fiche locale (cf. AdminMedicamentController) ; le nouveau médicament
 * apparaît directement dans la liste, sans rechargement de page.
 */
export default function MedicamentsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const q = searchParams.get("q") ?? "";
  const [medicaments, setMedicaments] = useState<Charge<Medicament[]>>({ state: "loading", data: [] });
  const [open, setOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const toast = useToast();

  function recharger() {
    listMedicaments()
      .then((data) => setMedicaments({ state: "ok", data }))
      .catch(() => setMedicaments({ state: "error", data: [] }));
  }

  useEffect(recharger, []);

  /** Filtre réel sur le catalogue déjà chargé — alimenté par la recherche de l'en-tête (?q=). */
  const filtres = useMemo(() => {
    if (medicaments.state !== "ok" || !q.trim()) return medicaments.state === "ok" ? medicaments.data : [];
    const terme = q.trim().toLowerCase();
    return medicaments.data.filter(
      (m) => m.nom.toLowerCase().includes(terme) || (m.dci ?? "").toLowerCase().includes(terme)
    );
  }, [medicaments, q]);

  return (
    <div className="animate-fade-up">
      <PageHeader
        eyebrow="Catalogue"
        title="Médicaments"
        description="Le catalogue réel, déclaré côté Business Core."
        action={
          <Button onClick={() => setOpen(true)}>
            <IconPlus className="h-4 w-4" /> Nouveau médicament
          </Button>
        }
      />

      {q.trim() && (
        <div className="mt-6 flex items-center gap-2 rounded-xl border border-brand/20 bg-brand-tint px-4 py-2.5 text-sm text-ink">
          <IconSearch className="h-4 w-4 flex-none text-brand" />
          <span>
            Résultats pour <strong>« {q} »</strong> ({filtres.length})
          </span>
          <button
            onClick={() => router.push("/medicaments")}
            className="ml-auto flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-medium text-brand hover:bg-white"
          >
            <IconClose className="h-3.5 w-3.5" /> Effacer
          </button>
        </div>
      )}

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
              {filtres.length === 0 && (
                <EmptyRow colSpan={6}>
                  {q.trim()
                    ? "Aucun médicament ne correspond à cette recherche."
                    : "Aucun médicament pour l'instant — créez-en un avec le bouton ci-dessus."}
                </EmptyRow>
              )}
              {filtres.map((m, i) => (
                <tr key={m.id} className={cn("transition-colors hover:bg-subtle", i !== 0 && "border-t border-line")}>
                  <Td>
                    <Link href={`/medicaments/${m.id}`} className="font-medium text-ink hover:text-brand">
                      {m.nom}
                    </Link>
                  </Td>
                  <Td>{m.dci ?? "—"}</Td>
                  <Td>
                    {m.ordonnanceRequise ? (
                      <span className="text-xs text-danger">Sur ordonnance</span>
                    ) : (
                      <span className="text-xs text-muted">Libre</span>
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
        subtitle="Déclare l'Offre côté Business Core, puis la fiche locale."
        open={open}
        onClose={() => setOpen(false)}
        formId={FORM_ID}
        submitLabel="Créer le médicament"
        submitting={submitting}
      >
        <NouveauMedicamentForm
          onSubmitting={setSubmitting}
          onSuccess={() => {
            setOpen(false);
            recharger();
            toast("success", "Médicament créé — Offre Business Core déclarée.");
          }}
          onError={(msg) => toast("error", msg)}
        />
      </SidePanel>
    </div>
  );
}

function NouveauMedicamentForm({
  onSubmitting,
  onSuccess,
  onError,
}: {
  onSubmitting: (v: boolean) => void;
  onSuccess: () => void;
  onError: (msg: string) => void;
}) {
  const [nom, setNom] = useState("");
  const [dci, setDci] = useState("");
  const [categorie, setCategorie] = useState<"medicament_libre" | "medicament_prescription">(
    "medicament_libre"
  );
  const [prixUnitaire, setPrixUnitaire] = useState("");
  const [stockInitial, setStockInitial] = useState("0");
  const [seuilAlerte, setSeuilAlerte] = useState("10");
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    onSubmitting(true);
    try {
      await creerMedicamentAdmin({
        nom,
        dci: dci || undefined,
        categorie,
        ordonnanceRequise: categorie === "medicament_prescription",
        prixUnitaire: Number(prixUnitaire),
        stockInitial: Number(stockInitial),
        seuilAlerte: Number(seuilAlerte),
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
    <form id={FORM_ID} onSubmit={onSubmit} className="space-y-5">
      <Field label="Nom" id="nom" value={nom} onChange={(e) => setNom(e.target.value)} required />
      <Field label="DCI (molécule)" id="dci" value={dci} onChange={(e) => setDci(e.target.value)} />
      <Select
        label="Catégorie"
        id="categorie"
        value={categorie}
        onChange={(e) => setCategorie(e.target.value as typeof categorie)}
      >
        <option value="medicament_libre">Libre</option>
        <option value="medicament_prescription">Sur ordonnance</option>
      </Select>
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
      {error && <p className="rounded-lg border-l-2 border-danger bg-danger/5 px-3.5 py-2.5 text-sm text-danger">{error}</p>}
    </form>
  );
}
