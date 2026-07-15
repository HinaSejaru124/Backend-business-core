"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { login, ApiError } from "@/lib/api";
import { useSession } from "@/lib/useSession";
import { homePour } from "@/lib/roles";
import { Button } from "@/components/Button";
import Field from "@/components/Field";
import { IconShield } from "@/components/icons";

/**
 * Point de connexion unique de PharmaCore — un seul formulaire, pour les 3 rôles (titulaire, pharmacien,
 * caissier). Le rôle est résolu côté serveur à partir de l'identité (cf. {@code AuthController}) : cet
 * écran ne mentionne jamais Kernel ni Business Core, PharmaCore se présente comme une application à
 * part entière.
 */
export default function ConnexionPage() {
  const router = useRouter();
  const { session, rafraichir } = useSession();
  const [email, setEmail] = useState("");
  const [motDePasse, setMotDePasse] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  useEffect(() => {
    if (session.state === "ok" && session.data?.connecte) {
      router.replace(homePour(session.data.role));
    }
  }, [session, router]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setErreur(null);
    setSubmitting(true);
    try {
      const statut = await login(email, motDePasse);
      rafraichir();
      router.replace(homePour(statut.role));
    } catch (err) {
      setErreur(err instanceof ApiError ? err.detail || err.title : "Connexion impossible.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      {/* Volet identité — plein vert foncé, cohérent avec le reste de l'application */}
      <div className="relative hidden flex-col justify-between overflow-hidden bg-ink px-14 py-12 text-white lg:flex">
        <div className="flex items-center gap-3">
          <span className="grid h-11 w-11 flex-none place-items-center bg-brand font-display text-xl font-bold text-white">
            +
          </span>
          <div className="font-display text-[19px] font-bold">PharmaCore</div>
        </div>

        <div>
          <div className="font-mono text-[12px] uppercase tracking-wider text-brand">Bienvenue</div>
          <h1 className="mt-3 font-display text-4xl font-bold leading-tight">Pharmacie du Centre</h1>
          <p className="mt-4 max-w-sm text-[15px] leading-relaxed text-white/70">
            Application de gestion de pharmacie — catalogue, ventes, ordonnances et suivi du stock,
            chacun connecté avec son propre accès.
          </p>
        </div>

        <p className="font-mono text-[11px] text-white/40">
          Bâtie sur Business Core — application de démonstration.
        </p>
      </div>

      {/* Volet formulaire */}
      <div className="flex items-center justify-center bg-subtle px-6 py-16">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex items-center gap-3 lg:hidden">
            <span className="grid h-10 w-10 flex-none place-items-center bg-brand font-display text-lg font-bold text-white">
              +
            </span>
            <div className="font-display text-lg font-bold text-ink">PharmaCore — Pharmacie du Centre</div>
          </div>

          <div className="mb-6 flex items-center gap-3">
            <IconShield className="h-6 w-6 text-brand" />
            <div>
              <h2 className="font-display text-xl font-semibold text-ink">Connexion</h2>
              <p className="text-xs text-muted">Titulaire, pharmacien ou caissier — un seul accès.</p>
            </div>
          </div>

          <form onSubmit={onSubmit} className="space-y-4 border border-line bg-white p-6 shadow-card">
            <Field
              label="E-mail"
              id="email"
              type="email"
              autoComplete="username"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <Field
              label="Mot de passe"
              id="motDePasse"
              type="password"
              autoComplete="current-password"
              value={motDePasse}
              onChange={(e) => setMotDePasse(e.target.value)}
              required
            />
            {erreur && (
              <p className="border-l-2 border-danger bg-danger/5 px-3 py-2 text-sm text-danger">{erreur}</p>
            )}
            <Button type="submit" className="w-full" disabled={submitting}>
              {submitting ? "Connexion…" : "Se connecter"}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
