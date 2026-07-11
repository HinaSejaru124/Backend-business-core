"use client";

import { useEffect, useState, type FormEvent } from "react";
import { listFournisseurs, creerFournisseur, ApiError } from "@/lib/api";
import type { Fournisseur } from "@/lib/types";
import { Button, ButtonLink } from "@/components/Button";
import Field from "@/components/Field";
import SidePanel from "@/components/SidePanel";
import PageHeader from "@/components/PageHeader";
import Table, { Th, Td, EmptyRow } from "@/components/Table";
import { IconPlus } from "@/components/icons";
import { useToast } from "@/components/Toast";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T };

const FORM_ID = "form-nouveau-fournisseur";

export default function FournisseursPage() {
  const [fournisseurs, setFournisseurs] = useState<Charge<Fournisseur[]>>({ state: "loading", data: [] });
  const [open, setOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const toast = useToast();

  function recharger() {
    listFournisseurs()
      .then((data) => setFournisseurs({ state: "ok", data }))
      .catch(() => setFournisseurs({ state: "error", data: [] }));
  }

  useEffect(recharger, []);

  return (
    <div className="animate-fade-up">
      <PageHeader
        eyebrow="Approvisionnement"
        title="Fournisseurs"
        action={
          <div className="flex gap-3">
            <ButtonLink href="/commandes" variant="secondary">
              Voir les commandes
            </ButtonLink>
            <Button onClick={() => setOpen(true)}>
              <IconPlus className="h-4 w-4" /> Nouveau fournisseur
            </Button>
          </div>
        }
      />

      <div className="mt-8">
        {fournisseurs.state === "loading" && <p className="text-sm text-muted">Chargement…</p>}
        {fournisseurs.state === "error" && (
          <p className="border-l-2 border-danger bg-danger/5 px-4 py-3 text-sm text-danger">
            Impossible de charger les fournisseurs.
          </p>
        )}
        {fournisseurs.state === "ok" && (
          <Table>
            <thead>
              <tr>
                <Th>Nom</Th>
                <Th>Contact</Th>
                <Th>Téléphone</Th>
                <Th>Délai de livraison</Th>
              </tr>
            </thead>
            <tbody>
              {fournisseurs.data.length === 0 && (
                <EmptyRow colSpan={4}>Aucun fournisseur pour l&apos;instant.</EmptyRow>
              )}
              {fournisseurs.data.map((f, i) => (
                <tr key={f.id} className={i !== 0 ? "border-t border-line" : ""}>
                  <Td className="font-medium text-ink">{f.nom}</Td>
                  <Td>{f.contactNom ?? "—"}</Td>
                  <Td>{f.contactTelephone ?? "—"}</Td>
                  <Td>{f.delaiLivraisonJours != null ? `${f.delaiLivraisonJours} j` : "—"}</Td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </div>

      <SidePanel
        title="Nouveau fournisseur"
        open={open}
        onClose={() => setOpen(false)}
        formId={FORM_ID}
        submitLabel="Créer le fournisseur"
        submitting={submitting}
      >
        <NouveauFournisseurForm
          onSubmitting={setSubmitting}
          onSuccess={() => {
            setOpen(false);
            recharger();
            toast("success", "Fournisseur créé.");
          }}
          onError={(msg) => toast("error", msg)}
        />
      </SidePanel>
    </div>
  );
}

function NouveauFournisseurForm({
  onSubmitting,
  onSuccess,
  onError,
}: {
  onSubmitting: (v: boolean) => void;
  onSuccess: () => void;
  onError: (m: string) => void;
}) {
  const [nom, setNom] = useState("");
  const [contactNom, setContactNom] = useState("");
  const [contactTelephone, setContactTelephone] = useState("");
  const [email, setEmail] = useState("");
  const [delai, setDelai] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    onSubmitting(true);
    try {
      await creerFournisseur({
        nom,
        contactNom: contactNom || undefined,
        contactTelephone: contactTelephone || undefined,
        email: email || undefined,
        delaiLivraisonJours: delai ? Number(delai) : undefined,
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
    <form id="form-nouveau-fournisseur" onSubmit={onSubmit} className="space-y-5">
      <Field label="Nom" id="nom" value={nom} onChange={(e) => setNom(e.target.value)} required />
      <div className="grid grid-cols-2 gap-5">
        <Field label="Contact" id="contact" value={contactNom} onChange={(e) => setContactNom(e.target.value)} />
        <Field
          label="Téléphone"
          id="telephone"
          value={contactTelephone}
          onChange={(e) => setContactTelephone(e.target.value)}
        />
      </div>
      <div className="grid grid-cols-2 gap-5">
        <Field label="E-mail" id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        <Field
          label="Délai de livraison (jours)"
          id="delai"
          type="number"
          min={0}
          value={delai}
          onChange={(e) => setDelai(e.target.value)}
        />
      </div>
      {error && <p className="border-l-2 border-danger bg-danger/5 px-3 py-2 text-sm text-danger">{error}</p>}
    </form>
  );
}
