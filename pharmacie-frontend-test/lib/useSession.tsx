"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { statutSession } from "./api";
import type { StatutSession } from "./types";

export type ChargeSession = { state: "loading" | "ok" | "error"; data: StatutSession | null };

type SessionContextValue = { session: ChargeSession; rafraichir: () => void };

const SessionContext = createContext<SessionContextValue | null>(null);

/**
 * Source unique de vérité pour le statut de connexion — partagée par toute l'application via Context.
 *
 * <p>Avant ce correctif, {@code useSession} créait un état indépendant à chaque appel : {@code AppShell}
 * (monté une fois, persiste entre les navigations) et {@code ConnexionPage} (remontée à chaque visite)
 * avaient chacun leur propre copie, jamais synchronisée. Après une connexion réussie, seule la copie de
 * {@code ConnexionPage} se mettait à jour ; celle d'{@code AppShell} restait figée sur « non connecté »,
 * le faisant rediriger vers {@code /connexion} — qui, elle, remontait avec une copie fraîche disant
 * « connecté » et repartait vers {@code /}. Boucle infinie de redirection (visible à l'écran comme un
 * clignotement rapide noir/blanc). Avec un seul état partagé, {@code rafraichir()} appelé n'importe où
 * (ex. juste après le login) met à jour la même source que celle lue par {@code AppShell}.
 */
export function SessionProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<ChargeSession>({ state: "loading", data: null });

  const rafraichir = useCallback(() => {
    statutSession()
      .then((data) => setSession({ state: "ok", data }))
      .catch(() => setSession({ state: "error", data: null }));
  }, []);

  useEffect(rafraichir, [rafraichir]);

  const value = useMemo(() => ({ session, rafraichir }), [session, rafraichir]);

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionContextValue {
  const ctx = useContext(SessionContext);
  if (!ctx) {
    throw new Error("useSession() doit être appelé sous <SessionProvider> (cf. app/layout.tsx).");
  }
  return ctx;
}
