"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { listClients, creerClient, ApiError } from "@/lib/api";
import type { Client } from "@/lib/types";
import { Button } from "@/components/Button";
import Field from "@/components/Field";
import SidePanel from "@/components/SidePanel";
import PageHeader from "@/components/PageHeader";
import Table, { Th, Td, EmptyRow } from "@/components/Table";
import { IconPlus } from "@/components/icons";
import { cn } from "@/lib/cn";
import { useToast } from "@/components/Toast";

type Charge<T> = { state: "loading" | "error" | "ok"; data: T };

const FORM_ID = "form-nouveau-client";

export default function ClientsPage() {
  const [clients, setClients] = useState<Charge<Client[]>>({ state: "loading", data: [] });
  const [open, setOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const toast = useToast();

  function recharger() {
    listClients()
      .then((data) => setClients({ state: "ok", data }))
      .catch(() => setClients({ state: "error", data: [] }));
  }

  useEffect(recharger, []);

  return (
    <div className="animate-fade-up">
      <PageHeader
        eyebrow="Registre"
        title="Clients"
        action={
          <Button onClick={() => setOpen(true)}>
            <IconPlus className="h-4 w-4" /> Nouveau client
          </Button>
        }
      />

      <div className="mt-8">
        {clients.state === "loading" && <p className="text-sm text-muted">Chargement…</p>}
        {clients.state === "error" && (
          <p className="border-l-2 border-danger bg-danger/5 px-4 py-3 text-sm text-danger">
            Impossible de charger les clients.
          </p>
        )}
        {clients.state === "ok" && (
          <Table>
            <thead>
              <tr>
                <Th>Nom</Th>
                <Th>Téléphone</Th>
                <Th>E-mail</Th>
              </tr>
            </thead>
            <tbody>
              {clients.data.length === 0 && (
                <EmptyRow colSpan={3}>Aucun client pour l&apos;instant.</EmptyRow>
              )}
              {clients.data.map((c, i) => (
                <tr key={c.id} className={cn("transition-colors hover:bg-subtle", i !== 0 && "border-t border-line")}>
                  <Td>
                    <Link href={`/clients/${c.id}`} className="font-medium text-ink hover:text-brand">
                      {c.prenom ? `${c.prenom} ${c.nom}` : c.nom}
                    </Link>
                  </Td>
                  <Td>{c.telephone ?? "—"}</Td>
                  <Td>{c.email ?? "—"}</Td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </div>

      <SidePanel
        title="Nouveau client"
        open={open}
        onClose={() => setOpen(false)}
        formId={FORM_ID}
        submitLabel="Créer le client"
        submitting={submitting}
      >
        <NouveauClientForm
          onSubmitting={setSubmitting}
          onSuccess={() => {
            setOpen(false);
            recharger();
            toast("success", "Client créé.");
          }}
          onError={(msg) => toast("error", msg)}
        />
      </SidePanel>
    </div>
  );
}

function NouveauClientForm({
  onSubmitting,
  onSuccess,
  onError,
}: {
  onSubmitting: (v: boolean) => void;
  onSuccess: () => void;
  onError: (m: string) => void;
}) {
  const [nom, setNom] = useState("");
  const [prenom, setPrenom] = useState("");
  const [telephone, setTelephone] = useState("");
  const [email, setEmail] = useState("");
  const [adresse, setAdresse] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    onSubmitting(true);
    try {
      await creerClient({
        nom,
        prenom: prenom || undefined,
        telephone: telephone || undefined,
        email: email || undefined,
        adresse: adresse || undefined,
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
    <form id="form-nouveau-client" onSubmit={onSubmit} className="space-y-5">
      <div className="grid grid-cols-2 gap-5">
        <Field label="Nom" id="nom" value={nom} onChange={(e) => setNom(e.target.value)} required />
        <Field label="Prénom" id="prenom" value={prenom} onChange={(e) => setPrenom(e.target.value)} />
      </div>
      <div className="grid grid-cols-2 gap-5">
        <Field label="Téléphone" id="telephone" value={telephone} onChange={(e) => setTelephone(e.target.value)} />
        <Field label="E-mail" id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
      </div>
      <Field label="Adresse" id="adresse" value={adresse} onChange={(e) => setAdresse(e.target.value)} />
      {error && <p className="rounded-lg border-l-2 border-danger bg-danger/5 px-3.5 py-2.5 text-sm text-danger">{error}</p>}
    </form>
  );
}
