import type { ReactNode } from "react";

/**
 * Coloration syntaxique sobre pour les blocs de code (bleu / cyan / gris uniquement).
 * K=clé, S=chaîne, N=nombre/booléen, P=ponctuation, C=commentaire, M=méthode/mot-clé.
 */
export const K = ({ children }: { children: ReactNode }) => <span className="text-sky-300">{children}</span>;
export const S = ({ children }: { children: ReactNode }) => <span className="text-slate-300">{children}</span>;
export const N = ({ children }: { children: ReactNode }) => <span className="text-cyan-300">{children}</span>;
export const P = ({ children }: { children: ReactNode }) => <span className="text-slate-500">{children}</span>;
export const C = ({ children }: { children: ReactNode }) => (
  <span className="italic text-slate-500">{children}</span>
);
export const M = ({ children }: { children: ReactNode }) => <span className="text-sky-400">{children}</span>;
