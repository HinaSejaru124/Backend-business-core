"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { cn } from "@/lib/cn";
import { useAuth } from "@/lib/auth-context";

export default function Logo({
  className,
  plain = false,
}: {
  className?: string;
  /** Rend le logo SANS son propre lien (pour l'imbriquer dans un <Link> parent, ex. sidebar console). */
  plain?: boolean;
}) {
  const { status, logout } = useAuth();
  const router = useRouter();

  function handleClick(e: React.MouseEvent<HTMLAnchorElement>) {
    if (status === "authed") {
      e.preventDefault();
      logout();
      router.push("/");
    }
  }

  const contenu = (
    <>
      <span className="grid h-8 w-8 place-items-center rounded-lg bg-gradient-to-br from-ink to-[#132a5c] shadow-glow-sm">
        {/* Cube dans un cube (isométrique) — reprend la marque de la maquette. */}
        <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="#4C7CFF" strokeWidth={1.6} strokeLinecap="round" strokeLinejoin="round">
          {/* Cube extérieur */}
          <path d="M12 2.6l8 4.4v9.9l-8 4.5-8-4.5V7z" />
          <path d="M4 7l8 4.5 8-4.5M12 11.5V21" opacity="0.9" />
          {/* Cube intérieur */}
          <path d="M12 7.3l3.7 2.05v4.6L12 16l-3.7-2.05v-4.6z" stroke="#8FB0FF" strokeWidth={1.3} />
        </svg>
      </span>
      <span className="font-display text-[15px] font-semibold tracking-tight text-ink">
        Business<span className="text-brand">Core</span>
      </span>
    </>
  );

  if (plain) {
    return <span className={cn("inline-flex items-center gap-2.5", className)}>{contenu}</span>;
  }

  return (
    <Link
      href="/"
      onClick={handleClick}
      className={cn("inline-flex items-center gap-2.5", className)}
      aria-label="Business Core — accueil"
    >
      {contenu}
    </Link>
  );
}

