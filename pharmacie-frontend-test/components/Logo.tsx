import { cn } from "@/lib/cn";

/**
 * Logo unique de PharmaCore — un seul style, réutilisé partout (connexion, sidebar). Croix pharmacie
 * en trait (pas de pastille pleine) + logotype bicolore "PHARMA / CORE", comme la maquette de
 * référence. Le sous-titre reprend le vrai nom de la pharmacie déjà utilisé dans l'app (jamais de nom
 * d'établissement inventé — cf. AUDIT-PHARMACORE.md, « aucune invention »).
 */
export default function Logo({
  size = "md",
  subtitle,
  dark = false,
  className,
}: {
  size?: "sm" | "md" | "lg";
  /** Sous-titre affiché sous le logotype. Omis = pas de sous-titre (usages compacts). */
  subtitle?: string;
  /** Fond sombre (volet de connexion, sidebar) : éclaircit le trait de la croix et l'encre du texte. */
  dark?: boolean;
  className?: string;
}) {
  const iconPx = size === "lg" ? 40 : size === "sm" ? 26 : 32;
  const wordmarkCls = size === "lg" ? "text-2xl" : size === "sm" ? "text-[15px]" : "text-lg";

  return (
    <div className={cn("inline-flex items-center gap-3", className)}>
      <svg
        width={iconPx}
        height={iconPx}
        viewBox="0 0 40 40"
        fill="none"
        aria-hidden
        className="flex-none"
      >
        <rect
          x="16.5" y="4" width="7" height="32" rx="3.5"
          stroke={dark ? "#5EE0A0" : "#16A34A"} strokeWidth="2.4"
        />
        <rect
          x="4" y="16.5" width="32" height="7" rx="3.5"
          stroke={dark ? "#5EE0A0" : "#16A34A"} strokeWidth="2.4"
        />
        <circle cx="30.5" cy="9.5" r="3.2" fill={dark ? "#5EE0A0" : "#16A34A"} />
      </svg>
      <div className="min-w-0 leading-none">
        <div className={cn("font-display font-bold tracking-tight", wordmarkCls)}>
          <span className={dark ? "text-white" : "text-ink"}>PHARMA</span>
          <span className={dark ? "text-brand-light" : "text-brand"}>CORE</span>
        </div>
        {subtitle && (
          <div
            className={cn(
              "mt-1 truncate font-mono text-[10.5px] uppercase tracking-wider",
              dark ? "text-white/50" : "text-muted"
            )}
          >
            {subtitle}
          </div>
        )}
      </div>
    </div>
  );
}
