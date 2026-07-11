"use client";

import { createPortal } from "react-dom";
import { useEffect, useState, type ReactNode } from "react";
import { Button } from "./Button";
import { IconClose } from "./icons";

/**
 * Panneau latéral coulissant (droite → gauche, ~1/3 d'écran, pleine hauteur réelle de l'écran).
 *
 * Monté via un Portail React directement dans document.body. C'est nécessaire, pas cosmétique :
 * chaque page a une animation d'entrée (animate-fade-up) qui laisse un `transform` actif sur son
 * conteneur même après la fin de l'animation (fill-mode "both"). En CSS, TOUT ancêtre avec un
 * `transform` devient le référentiel de positionnement de ses descendants en `position: fixed`
 * (pas la fenêtre) — le panneau se retrouvait donc confiné à la hauteur de la carte de contenu au
 * lieu de couvrir tout l'écran. Le Portail sort le panneau de cette arborescence, il est alors
 * toujours positionné par rapport à la fenêtre entière, quelle que soit la page.
 */
export default function SidePanel({
  title,
  subtitle,
  open,
  onClose,
  formId,
  submitLabel = "Enregistrer",
  submitting = false,
  children,
}: {
  title: string;
  subtitle?: string;
  open: boolean;
  onClose: () => void;
  /** id du <form> à soumettre — le bouton "Enregistrer" de l'en-tête lui est rattaché (attribut form=). */
  formId: string;
  submitLabel?: string;
  submitting?: boolean;
  children: ReactNode;
}) {
  const [mounted, setMounted] = useState(false);
  const [container, setContainer] = useState<HTMLElement | null>(null);

  useEffect(() => {
    setContainer(document.body);
  }, []);

  useEffect(() => {
    if (open) {
      setMounted(true);
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [open]);

  if (!mounted || !container) return null;

  return createPortal(
    <div
      className={`fixed inset-0 z-[100] transition-opacity duration-300 ${
        open ? "opacity-100" : "pointer-events-none opacity-0"
      }`}
      onTransitionEnd={() => {
        if (!open) setMounted(false);
      }}
    >
      {/* Fond flouté */}
      <div className="fixed inset-0 bg-ink/20 backdrop-blur-sm" onClick={onClose} />

      {/* Panneau — position fixed directe (plus de conteneur intermédiaire), couvre toute
          la hauteur de l'écran, du tout en haut au tout en bas. */}
      <div
        className={`fixed inset-y-0 right-0 flex w-full max-w-xl flex-col border-l-4 border-brand bg-white shadow-2xl transition-transform duration-300 ease-out ${
          open ? "translate-x-0" : "translate-x-full"
        }`}
      >
        {/* En-tête : titre + actions (Annuler / Enregistrer), comme un vrai panneau applicatif */}
        <div className="flex flex-none items-center justify-between gap-4 border-b border-line bg-ink px-6 py-5">
          <div className="min-w-0">
            <div className="font-mono text-[11px] uppercase tracking-wider text-brand">Nouveau</div>
            <h2 className="mt-1 truncate font-display text-xl font-bold text-white">{title}</h2>
            {subtitle && <p className="mt-1 truncate text-[13px] text-white/70">{subtitle}</p>}
          </div>
          <div className="flex flex-none items-center gap-2.5">
            <Button type="button" variant="secondary" size="sm" onClick={onClose}>
              Annuler
            </Button>
            <Button type="submit" form={formId} size="sm" disabled={submitting}>
              {submitting ? "Enregistrement…" : submitLabel}
            </Button>
            <button
              onClick={onClose}
              className="grid h-9 w-9 flex-none place-items-center border border-white/20 text-white/80 transition-colors hover:bg-white/10 hover:text-white"
              aria-label="Fermer"
            >
              <IconClose className="h-4 w-4" />
            </button>
          </div>
        </div>

        {/* Corps — tout le formulaire visible du haut vers le bas, scroll seulement si vraiment necessaire */}
        <div className="flex-1 overflow-y-auto px-7 py-7">{children}</div>
      </div>
    </div>,
    container
  );
}
