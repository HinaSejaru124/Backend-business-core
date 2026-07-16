"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { login, ApiError } from "@/lib/api";
import { useSession } from "@/lib/useSession";
import { homePour } from "@/lib/roles";
import { Button } from "@/components/Button";
import Logo from "@/components/Logo";
import { IconEye, IconEyeOff, IconShield } from "@/components/icons";

/**
 * Point de connexion unique de PharmaCore — un seul formulaire, pour les 3 rôles (titulaire, pharmacien,
 * caissier). Le rôle est résolu côté serveur à partir de l'identité (cf. {@code AuthController}) : cet
 * écran ne mentionne jamais Kernel ni Business Core, PharmaCore se présente comme une application à
 * part entière. Refonte visuelle uniquement — {@code login()} et la logique de redirection par rôle
 * (backend, {@code lib/roles.ts}) sont strictement inchangées.
 */
export default function ConnexionPage() {
  const router = useRouter();
  const { session, rafraichir } = useSession();
  const [email, setEmail] = useState("");
  const [motDePasse, setMotDePasse] = useState("");
  const [voirMotDePasse, setVoirMotDePasse] = useState(false);
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
      {/* Volet identité — dégradé de marque, halos doux, cohérent avec la sidebar de l'application */}
      <div className="relative hidden flex-col justify-between overflow-hidden bg-ink-gradient px-14 py-12 text-white lg:flex">
        <div
          className="pointer-events-none absolute -left-24 top-1/3 h-[420px] w-[420px] rounded-full bg-brand/25 blur-[110px]"
          aria-hidden
        />
        <div
          className="pointer-events-none absolute -right-16 bottom-0 h-[320px] w-[320px] rounded-full bg-brand-light/20 blur-[100px]"
          aria-hidden
        />
        <svg
          className="pointer-events-none absolute -right-10 top-10 h-72 w-72 text-white/[0.05]"
          viewBox="0 0 40 40" fill="currentColor" aria-hidden
        >
          <rect x="16.5" y="4" width="7" height="32" rx="3.5" />
          <rect x="4" y="16.5" width="32" height="7" rx="3.5" />
        </svg>

        <Logo dark size="lg" />

        <div className="relative">
          <div className="font-mono text-[12px] uppercase tracking-wider text-brand-light">Bienvenue</div>
          <h1 className="mt-3 font-display text-4xl font-bold leading-tight">Pharmacie du Centre</h1>
          <p className="mt-4 max-w-sm text-[15px] leading-relaxed text-white/70">
            Application de gestion de pharmacie — catalogue, ventes, ordonnances et suivi du stock,
            chacun connecté avec son propre accès.
          </p>
        </div>

        <p className="relative font-mono text-[11px] text-white/40">
          Bâtie sur Business Core — application de démonstration.
        </p>
      </div>

      {/* Volet formulaire */}
      <div className="flex items-center justify-center bg-canvas px-6 py-16">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex justify-center lg:hidden">
            <Logo size="md" />
          </div>

          <div className="rounded-2xl border border-line bg-white p-8 shadow-pop animate-scale-in">
            <div className="flex flex-col items-center text-center">
              <span className="grid h-14 w-14 place-items-center rounded-full bg-brand-tint text-brand">
                <IconShield className="h-6 w-6" />
              </span>
              <h2 className="mt-4 font-display text-2xl font-bold text-ink">Bienvenue</h2>
              <p className="mt-1.5 text-sm text-muted">
                Connectez-vous à votre espace PharmaCore.
                <br />
                Titulaire, pharmacien ou caissier — un seul accès.
              </p>
            </div>

            <form onSubmit={onSubmit} className="mt-7 space-y-4">
              <label htmlFor="email" className="block">
                <span className="mb-1.5 block text-[13px] font-medium text-ink">Adresse e-mail</span>
                <input
                  id="email"
                  type="email"
                  autoComplete="username"
                  placeholder="vous@pharmacie.cm"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="h-12 w-full rounded-xl border border-line bg-subtle px-4 text-sm text-body outline-none transition-colors placeholder:text-muted/60 focus:border-brand focus:bg-white focus:ring-4 focus:ring-brand/10"
                />
              </label>

              <label htmlFor="motDePasse" className="block">
                <span className="mb-1.5 block text-[13px] font-medium text-ink">Mot de passe</span>
                <div className="relative">
                  <input
                    id="motDePasse"
                    type={voirMotDePasse ? "text" : "password"}
                    autoComplete="current-password"
                    placeholder="Entrez votre mot de passe"
                    value={motDePasse}
                    onChange={(e) => setMotDePasse(e.target.value)}
                    required
                    className="h-12 w-full rounded-xl border border-line bg-subtle px-4 pr-11 text-sm text-body outline-none transition-colors placeholder:text-muted/60 focus:border-brand focus:bg-white focus:ring-4 focus:ring-brand/10"
                  />
                  <button
                    type="button"
                    onClick={() => setVoirMotDePasse((v) => !v)}
                    className="absolute right-3.5 top-1/2 -translate-y-1/2 text-muted transition-colors hover:text-brand"
                    aria-label={voirMotDePasse ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                  >
                    {voirMotDePasse ? <IconEyeOff className="h-4.5 w-4.5" /> : <IconEye className="h-4.5 w-4.5" />}
                  </button>
                </div>
              </label>

              {erreur && (
                <p className="rounded-lg border border-danger/25 bg-danger/5 px-3.5 py-2.5 text-sm text-danger">
                  {erreur}
                </p>
              )}

              <Button type="submit" className="mt-2 w-full justify-center rounded-xl" disabled={submitting}>
                {submitting ? "Connexion…" : "Se connecter"}
              </Button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
