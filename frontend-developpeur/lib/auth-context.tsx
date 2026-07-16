"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { me, getToken, getPrincipal, clearToken, type Me } from "@/lib/api";

/**
 * État d'authentification GLOBAL de l'application (source de vérité côté UI).
 * - `loading` : session en cours de vérification (GET /v1/auth/me) ;
 * - `anon`    : personne n'est connecté (pas de token, ou token invalide → purgé) ;
 * - `authed`  : session réelle validée par le backend (profil kernel).
 * La navbar, la console et toutes les gardes consomment CE contexte — jamais le
 * localStorage directement — pour que l'UI reflète toujours la réalité.
 */
type AuthStatus = "loading" | "anon" | "authed";

type AuthState = {
  status: AuthStatus;
  profil: Me | null;
  /** E-mail utilisé au login (affichage du compte). */
  principal: string | null;
  /** Re-vérifie la session auprès du backend (après login). */
  refresh: () => Promise<void>;
  /** Déconnexion réelle : purge le token et repasse en anonyme. */
  logout: () => void;
};

const AuthContext = createContext<AuthState>({
  status: "loading",
  profil: null,
  principal: null,
  refresh: async () => {},
  logout: () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [profil, setProfil] = useState<Me | null>(null);
  const [principal, setPrincipal] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!getToken()) {
      setProfil(null);
      setPrincipal(null);
      setStatus("anon");
      return;
    }
    setStatus("loading");
    try {
      const p = await me(); // valide le token auprès du backend (JWT kernel vérifié)
      setProfil(p);
      setPrincipal(getPrincipal());
      setStatus("authed");
    } catch {
      // Token périmé/invalide : apiFetch a déjà purgé sur 401 — on reflète la réalité.
      clearToken();
      setProfil(null);
      setPrincipal(null);
      setStatus("anon");
    }
  }, []);

  const logout = useCallback(() => {
    clearToken();
    setProfil(null);
    setPrincipal(null);
    setStatus("anon");
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  // Un appel API quelconque a reçu un 401 en cours de session (token expiré/révoqué) :
  // on resynchronise immédiatement l'état affiché au lieu de rester sur "connecté" jusqu'au
  // prochain rechargement complet de la page.
  useEffect(() => {
    function onSessionExpired() {
      void refresh();
    }
    window.addEventListener("bc:session-expired", onSessionExpired);
    return () => window.removeEventListener("bc:session-expired", onSessionExpired);
  }, [refresh]);

  return (
    <AuthContext.Provider value={{ status, profil, principal, refresh, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  return useContext(AuthContext);
}
