"use client";

import { useEffect, useState, Suspense, type FormEvent } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Field from "@/components/Field";
import PasswordField from "@/components/PasswordField";
import CodeWindow from "@/components/CodeWindow";
import { Button } from "@/components/Button";
import { login, requestApiKey, ApiError, type ApiKeyResponse } from "@/lib/api";
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
{ "clientId": "bck_m9sA2b...",
  "apiKey":   "aBcDeF123...",
  "plan":     "PRO" }`;

function LoginFormContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const auth = useAuth();

  const tabParam = searchParams.get("tab") === "register" ? "register" : "login";
  const [tab, setTab] = useState<Tab>(tabParam);

  const planParam = (searchParams.get("plan") || "FREE").toUpperCase();
  const initialPlan = ["FREE", "PRO", "ENTERPRISE"].includes(planParam) ? planParam : "FREE";
  const [selectedPlan, setSelectedPlan] = useState(initialPlan);

  // Déjà connecté ? Cette page n'a pas de sens : direction la console.
  useEffect(() => {
    if (auth.status === "authed") router.replace("/console");
  }, [auth.status, router]);

  // Connexion (réelle → /v1/auth/login)
  const [principal, setPrincipal] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Inscription (réelle → /v1/registration)
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [regPassword, setRegPassword] = useState("");
  const [regConfirm, setRegConfirm] = useState("");
  const [accept, setAccept] = useState(false);
  const [regLoading, setRegLoading] = useState(false);
  const [regError, setRegError] = useState<string | null>(null);
  const [regSuccess, setRegSuccess] = useState<string | null>(null);
  const [regSuccessData, setRegSuccessData] = useState<ApiKeyResponse | null>(null);
  const [copiedKey, setCopiedKey] = useState(false);
  const [copiedId, setCopiedId] = useState(false);

  async function copyToClipboard(text: string, setCopied: (v: boolean) => void) {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 1400);
    } catch { }
  }

  async function onLogin(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(principal, password);
      await auth.refresh(); // met à jour l'état global (navbar, console) AVANT la redirection
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
    setRegSuccessData(null);
    if (regPassword !== regConfirm) {
      setRegError("Les mots de passe ne correspondent pas.");
      return;
    }
    setRegLoading(true);
    try {
      const res = await requestApiKey(firstName, lastName, email, regPassword, selectedPlan);
      setRegSuccessData(res);
      setRegSuccess("Information de clé d'API générée avec succès.");
      if (typeof window !== "undefined") {
        window.localStorage.setItem("bc_client_id", res.clientId);
        window.localStorage.setItem("bc_api_key", res.apiKey);
        window.localStorage.setItem("bc_plan", res.plan);
      }
    } catch (err) {
      setRegError(
        err instanceof ApiError
          ? err.detail || err.title
          : "Inscription impossible — vérifiez que le backend est démarré."
      );
    } finally {
      setRegLoading(false);
    }
  }


  return (
    <div className="grid h-[calc(100vh-4rem)] overflow-hidden lg:grid-cols-2">
      {/* ── Présentation (GAUCHE) — hauteur figée, ne bouge jamais ── */}
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

      {/* ── Formulaire (DROITE) — cadre figé ; seul le contenu du formulaire défile si besoin ── */}
      <div className="order-1 flex h-full items-center justify-center overflow-hidden border-b border-line px-6 py-6 lg:order-2 lg:border-b-0 lg:border-l">
        <div className="flex max-h-full w-full max-w-sm flex-col">
          {/* Onglets FIXES — ne bougent jamais, quel que soit le formulaire actif */}
          <div className="flex flex-none border border-line">
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
                  "h-11 flex-1 text-sm font-medium transition-colors",
                  tab === t.id ? "bg-ink text-white" : "bg-white text-muted hover:bg-tint hover:text-ink"
                )}
              >
                {t.label}
              </button>
            ))}
          </div>

          {/* Seul le formulaire change (animé) — défile en interne si trop grand, le cadre autour ne bouge pas */}
          <div key={tab} className="mt-6 min-h-0 flex-1 animate-fade-up overflow-y-auto pr-1">
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
                {error && (
                  <p className="border-l-2 border-danger bg-danger/5 px-3 py-2 text-sm text-danger">{error}</p>
                )}
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
                    Obtenez votre clé d&apos;API Business Core et créez votre compte développeur.
                  </p>
                </div>

                {regSuccessData ? (
                  <div className="border border-line bg-white p-6 space-y-4">
                    <div className="flex flex-col items-center text-center">
                      <IconCheck className="h-8 w-8 text-brand" />
                      <h2 className="mt-2 font-display text-xl font-bold text-ink">Compte et Clé créés</h2>
                      <p className="mt-1 text-xs text-muted">
                        Votre clé d&apos;API ne sera affichée <strong>qu&apos;une seule fois</strong>. Veuillez la sauvegarder.
                      </p>
                    </div>

                    <div className="space-y-3 pt-2 text-left">
                      <div>
                        <div className="text-[11px] font-medium uppercase tracking-wider text-muted font-mono">Client ID</div>
                        <div className="mt-1 flex items-center justify-between border border-line bg-subtle p-2">
                          <code className="truncate font-mono text-xs text-ink">{regSuccessData.clientId}</code>
                          <button
                            type="button"
                            onClick={() => copyToClipboard(regSuccessData.clientId, setCopiedId)}
                            className="text-xs text-brand hover:underline pl-2 font-medium"
                          >
                            {copiedId ? "Copié !" : "Copier"}
                          </button>
                        </div>
                      </div>

                      <div>
                        <div className="text-[11px] font-medium uppercase tracking-wider text-muted font-mono">Clé d&apos;API</div>
                        <div className="mt-1 flex items-center justify-between border border-line bg-subtle p-2">
                          <code className="truncate font-mono text-xs text-brand font-semibold">{regSuccessData.apiKey}</code>
                          <button
                            type="button"
                            onClick={() => copyToClipboard(regSuccessData.apiKey, setCopiedKey)}
                            className="text-xs text-brand hover:underline pl-2 font-medium"
                          >
                            {copiedKey ? "Copié !" : "Copier"}
                          </button>
                        </div>
                      </div>

                      <div>
                        <div className="text-[11px] font-medium uppercase tracking-wider text-muted font-mono">Plan de démarrage</div>
                        <div className="mt-1 font-mono text-xs text-ink font-semibold">{regSuccessData.plan}</div>
                      </div>
                    </div>

                    <Button
                      type="button"
                      className="mt-4 w-full"
                      onClick={() => {
                        setTab("login");
                        setPrincipal(email);
                        setRegSuccessData(null);
                        setRegSuccess(null);
                      }}
                    >
                      Se connecter maintenant
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
                      <span className="mb-1.5 block text-[13px] font-medium text-ink">Plan initial de clé d&apos;API</span>
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
                    {regError && (
                      <p className="border-l-2 border-danger bg-danger/5 px-3 py-2 text-sm text-danger">
                        {regError}
                      </p>
                    )}
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
