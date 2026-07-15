"use client";

import { useEffect, useState, Suspense, type FormEvent } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Field from "@/components/Field";
import PasswordField from "@/components/PasswordField";
import CodeWindow from "@/components/CodeWindow";
import { Button } from "@/components/Button";
import { Banner, OnboardingSteps } from "@/components/Feedback";
import { login, registerDeveloper, ApiError, type InscriptionResponse } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { cn } from "@/lib/cn";
import { IconCheck } from "@/components/icons";

type Tab = "login" | "register";

const SNIP_REQ = `POST /v1/registration
{ "firstName": "Miguel",
  "lastName":  "Techlan",
  "email":     "…@gmail.com",
  "password":  "••••••",
  "planCode":  "PRO" }`;
const SNIP_RES = `→ 201 Created
{ "plan":    "PRO",
  "message": "Compte créé. Vérifiez votre email…" }`;

const POST_REGISTER_STEPS = [
  { label: "Vérifier votre e-mail de confirmation", done: false },
  { label: "Se connecter avec votre compte", done: false, href: undefined },
  { label: "Créer une entreprise", done: false, href: "/console/businesses" },
  { label: "Générer une clé API pour cette entreprise", done: false, href: "/console/api-key" },
];

function LoginFormContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const auth = useAuth();

  const tabParam = searchParams.get("tab") === "register" ? "register" : "login";
  const [tab, setTab] = useState<Tab>(tabParam);

  const planParam = (searchParams.get("plan") || "FREE").toUpperCase();
  const initialPlan = ["FREE", "PRO", "ENTERPRISE"].includes(planParam) ? planParam : "FREE";
  const [selectedPlan, setSelectedPlan] = useState(initialPlan);

  useEffect(() => {
    if (auth.status === "authed") router.replace("/console");
  }, [auth.status, router]);

  const [principal, setPrincipal] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [regPassword, setRegPassword] = useState("");
  const [regConfirm, setRegConfirm] = useState("");
  const [accept, setAccept] = useState(false);
  const [regLoading, setRegLoading] = useState(false);
  const [regError, setRegError] = useState<string | null>(null);
  const [regSuccess, setRegSuccess] = useState<InscriptionResponse | null>(null);

  async function onLogin(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(principal, password);
      await auth.refresh();
      router.push("/console");
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.detail || err.title
          : "Connexion impossible — vérifiez que le backend est démarré (NEXT_PUBLIC_API_BASE_URL)."
      );
    } finally {
      setLoading(false);
    }
  }

  async function onRegister(e: FormEvent) {
    e.preventDefault();
    setRegError(null);
    setRegSuccess(null);
    if (regPassword !== regConfirm) {
      setRegError("Les mots de passe ne correspondent pas.");
      return;
    }
    setRegLoading(true);
    try {
      const res = await registerDeveloper(firstName, lastName, email, regPassword, selectedPlan);
      setRegSuccess(res);
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setRegError("Cet e-mail est déjà utilisé. Connectez-vous ou utilisez une autre adresse.");
      } else {
        setRegError(
          err instanceof ApiError
            ? err.detail || err.title
            : "Inscription impossible — vérifiez que le backend est démarré."
        );
      }
    } finally {
      setRegLoading(false);
    }
  }

  return (
    <div className="grid h-[calc(100vh-4rem)] overflow-hidden lg:grid-cols-2">
      <div className="relative order-2 hidden overflow-hidden bg-ink lg:order-1 lg:block">
        <div className="code-photo absolute inset-0 opacity-25" aria-hidden />
        <div className="absolute inset-0 bg-gradient-to-br from-ink/95 to-ink/75" aria-hidden />
        <div className="linegrid absolute inset-0 opacity-20" aria-hidden />
        <div className="relative flex h-full flex-col justify-center gap-10 px-14">
          <div>
            <div className="font-mono text-[12px] uppercase tracking-wider text-brand">Business Core</div>
            <h2 className="mt-4 max-w-md font-display text-3xl font-bold leading-tight text-white">
              Un cœur métier prêt à l&apos;emploi, au-dessus du Kernel.
            </h2>
            <ul className="mt-8 space-y-4 text-[15px] text-slate-300">
              {[
                "Déclarez votre métier en données, pas en code",
                "Authentification déléguée, sécurisée par le Kernel",
                "Isolation multi-tenant et traçabilité intégrées",
              ].map((t) => (
                <li key={t} className="flex gap-3">
                  <span className="mt-2.5 h-px w-5 flex-none bg-brand" />
                  {t}
                </li>
              ))}
            </ul>
          </div>
          <div className="grid max-w-md gap-3">
            <CodeWindow filename="requête" lang="http" typeText={SNIP_REQ} typeSpeed={20} />
            <CodeWindow filename="réponse" lang="json" typeText={SNIP_RES} typeSpeed={20} />
          </div>
        </div>
      </div>

      <div className="relative order-1 flex h-full items-center justify-center overflow-hidden bg-subtle px-6 py-6 lg:order-2">
        {/* Halo lumineux animé derrière le grand rectangle — respire doucement, ne bloque jamais les clics. */}
        <div
          className="glow-orb pointer-events-none absolute left-1/2 top-1/2 h-[420px] w-[420px] -translate-x-1/2 -translate-y-1/2 animate-pulse-soft"
          aria-hidden
        />

        <div className="relative flex max-h-full w-full max-w-sm flex-col rounded-2xl border border-line bg-white p-6 shadow-glow transition-shadow duration-300 hover:shadow-[0_35px_80px_-20px_rgba(27,77,245,.4),0_12px_32px_-10px_rgba(11,27,58,.28)] sm:p-7">
          <div className="flex flex-none rounded-lg border border-line bg-subtle p-1">
            {(
              [
                { id: "login", label: "Se connecter" },
                { id: "register", label: "Créer un compte" },
              ] as { id: Tab; label: string }[]
            ).map((t) => (
              <button
                key={t.id}
                onClick={() => {
                  setTab(t.id);
                  setError(null);
                  setRegError(null);
                }}
                className={cn(
                  "h-10 flex-1 rounded-md text-sm font-semibold transition-all duration-200",
                  tab === t.id ? "bg-ink text-white shadow-card" : "bg-transparent text-muted hover:text-ink"
                )}
              >
                {t.label}
              </button>
            ))}
          </div>

          <div key={tab} className="no-scrollbar mt-6 min-h-0 flex-1 animate-fade-up overflow-y-auto pr-1">
            {tab === "login" ? (
              <form onSubmit={onLogin} className="space-y-5">
                <div>
                  <h1 className="font-display text-2xl font-bold text-ink">Bon retour.</h1>
                  <p className="mt-1 text-sm text-muted">Connectez-vous pour accéder à votre console.</p>
                </div>
                <Field
                  label="E-mail"
                  id="principal"
                  type="email"
                  autoComplete="email"
                  placeholder="exemple : vous@domaine.com"
                  value={principal}
                  onChange={(e) => setPrincipal(e.target.value)}
                  required
                />
                <PasswordField
                  label="Mot de passe"
                  id="password"
                  autoComplete="current-password"
                  placeholder="Entrez votre mot de passe"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
                {error && <Banner variant="error">{error}</Banner>}
                <Button type="submit" className="w-full" disabled={loading}>
                  {loading ? "Connexion…" : "Se connecter"}
                </Button>
                <p className="text-center text-xs text-muted">
                  Connexion déléguée au Kernel — aucun mot de passe n&apos;est stocké.
                </p>
              </form>
            ) : (
              <form onSubmit={onRegister} className="space-y-4">
                <div>
                  <h1 className="font-display text-xl font-bold text-ink">Créer un compte.</h1>
                  <p className="mt-1 text-sm text-muted">
                    Créez votre compte développeur Business Core. La clé API sera générée après
                    connexion, pour chaque entreprise.
                  </p>
                </div>

                {regSuccess ? (
                  <div className="space-y-4 border border-line bg-white p-6">
                    <div className="flex flex-col items-center text-center">
                      <IconCheck className="h-8 w-8 text-brand" />
                      <h2 className="mt-2 font-display text-xl font-bold text-ink">Compte créé</h2>
                      <p className="mt-1 text-xs text-muted">
                        Plan <strong>{regSuccess.plan}</strong>
                      </p>
                    </div>

                    <Banner variant="info">{regSuccess.message}</Banner>

                    <OnboardingSteps steps={POST_REGISTER_STEPS} title="Prochaines étapes" />

                    <Button
                      type="button"
                      className="w-full"
                      onClick={() => {
                        setTab("login");
                        setPrincipal(email);
                        setRegSuccess(null);
                      }}
                    >
                      Aller à la connexion
                    </Button>
                  </div>
                ) : (
                  <>
                    <div className="grid gap-4 sm:grid-cols-2">
                      <Field
                        label="Prénom"
                        id="firstName"
                        placeholder="Miguel"
                        value={firstName}
                        onChange={(e) => setFirstName(e.target.value)}
                        required
                      />
                      <Field
                        label="Nom"
                        id="lastName"
                        placeholder="Techlan"
                        value={lastName}
                        onChange={(e) => setLastName(e.target.value)}
                        required
                      />
                    </div>
                    <Field
                      label="E-mail"
                      id="email"
                      type="email"
                      autoComplete="off"
                      placeholder="exemple : vous@domaine.com"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      required
                    />
                    <label className="block">
                      <span className="mb-1.5 block text-[13px] font-medium text-ink">Plan initial</span>
                      <select
                        value={selectedPlan}
                        onChange={(e) => setSelectedPlan(e.target.value)}
                        className="h-11 w-full border border-line bg-white px-3 text-sm text-body outline-none focus:border-brand"
                      >
                        <option value="FREE">FREE — Gratuit pour démarrer</option>
                        <option value="PRO">PRO — Standard pour production</option>
                        <option value="ENTERPRISE">ENTERPRISE — Sur devis</option>
                      </select>
                    </label>
                    <PasswordField
                      label="Mot de passe"
                      id="new-password"
                      autoComplete="new-password"
                      placeholder="Choisissez un mot de passe (10 caractères min.)"
                      minLength={10}
                      value={regPassword}
                      onChange={(e) => setRegPassword(e.target.value)}
                      required
                    />
                    <PasswordField
                      label="Confirmer le mot de passe"
                      id="confirm-password"
                      autoComplete="new-password"
                      placeholder="Répétez le mot de passe"
                      minLength={10}
                      value={regConfirm}
                      onChange={(e) => setRegConfirm(e.target.value)}
                      required
                    />
                    <label className="flex items-start gap-2.5 text-sm">
                      <input
                        type="checkbox"
                        checked={accept}
                        onChange={(e) => setAccept(e.target.checked)}
                        className="mt-0.5 h-4 w-4 flex-none accent-[#1B4DF5]"
                      />
                      <span className="text-muted">
                        J&apos;accepte les conditions d&apos;utilisation et la politique de confidentialité.
                      </span>
                    </label>
                    {regError && <Banner variant="error">{regError}</Banner>}
                    <Button type="submit" className="w-full" disabled={!accept || regLoading}>
                      {regLoading ? "Création…" : "Créer mon compte"}
                    </Button>
                  </>
                )}
              </form>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center">
          <p className="text-sm text-muted">Chargement…</p>
        </div>
      }
    >
      <LoginFormContent />
    </Suspense>
  );
}
