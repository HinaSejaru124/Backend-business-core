/** Concatène des classes conditionnelles (mini utilitaire, sans dépendance). */
export function cn(...parts: Array<string | false | null | undefined>): string {
  return parts.filter(Boolean).join(" ");
}
