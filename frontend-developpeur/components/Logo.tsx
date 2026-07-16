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
      <span className="grid h-7 w-7 place-items-center bg-ink">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="#1B4DF5" strokeWidth={2} strokeLinecap="square">
          <path d="M9 6L4 12l5 6M15 6l5 6-5 6" />
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

