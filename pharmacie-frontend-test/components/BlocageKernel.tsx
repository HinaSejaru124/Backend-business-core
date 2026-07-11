import { IconAlertTriangle } from "./icons";

/**
 * Bandeau honnête affiché tant que POST /api/ventes n'existe pas côté backend Pharmacie —
 * lui-même bloqué par l'impossibilité de créer une Entreprise Business Core (le Kernel
 * n'accepte plus le grant OAuth2 client_credentials). Cf. SPIKE-RESULTATS.md pour le détail
 * technique complet. Pas de simulation : on explique la vraie raison plutôt que de masquer
 * l'écran ou d'inventer un comportement.
 */
export default function BlocageKernel({ contexte }: { contexte: string }) {
  return (
    <div className="border border-warning/30 bg-warning/5 p-6">
      <div className="flex items-start gap-3">
        <IconAlertTriangle className="mt-0.5 h-5 w-5 flex-none text-warning" />
        <div>
          <h2 className="font-display text-base font-semibold text-ink">
            {contexte} temporairement indisponible
          </h2>
          <p className="mt-2 text-sm leading-relaxed text-body">
            Cette fonctionnalité dépend d&apos;une Entreprise réelle côté Business Core, elle-même
            créée via un appel du Kernel qui échoue actuellement : le Kernel n&apos;accepte plus le
            grant OAuth2 <code className="bg-white px-1 font-mono text-xs">client_credentials</code>{" "}
            utilisé pour provisionner l&apos;organisation. Ce n&apos;est pas un bug de PharmaCore — c&apos;est
            un blocage d&apos;infrastructure Business Core, déjà remonté à l&apos;équipe.
          </p>
          <p className="mt-2 text-xs text-muted">
            Détail technique complet : <code className="font-mono">pharmacie-backend-test/SPIKE-RESULTATS.md</code>
          </p>
        </div>
      </div>
    </div>
  );
}
