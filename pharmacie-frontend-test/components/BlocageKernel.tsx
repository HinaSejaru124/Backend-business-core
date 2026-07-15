import { IconAlertTriangle } from "./icons";

/**
 * Bandeau honnête affiché tant que POST /api/ventes n'aboutit pas côté backend Pharmacie.
 *
 * <p>Historique : le 13/07/2026, la saga passait VERIFIER_STOCK, EVALUER_REGLES, ENREGISTRER_VENTE et
 * échouait seulement à ENGAGER_STOCK. Retest en direct le 15/07/2026 (appel réel à
 * POST /v1/businesses/{id}/operations/Vendre:execute, clé API PharmaCore) : le blocage a reculé à la
 * toute première étape kernel, VERIFIER_STOCK (502 en ~1s, même cause). Cause racine inchangée et
 * confirmée à nouveau par un test isolé de POST /oauth2/token avec les identifiants plateforme réels :
 * Kernel rejette le grant OAuth2 client_credentials ("Unsupported grant_type"), en ~1,5s (pas un
 * timeout réseau — un rejet propre et rapide). Comme PharmaCore appelle Business Core par clé API
 * (jamais de JWT délégué), CHAQUE étape kernel de la vente (VERIFIER_STOCK, ENREGISTRER_VENTE,
 * ENGAGER_STOCK, ENCAISSER) emprunte ce même chemin machine-à-machine cassé — ce n'est donc plus
 * seulement l'engagement de stock qui est bloqué, c'est tout le tronçon kernel de la vente. Ce n'est
 * pas un bug de PharmaCore ni de Business Core — confirmé côté Kernel, remonté à l'équipe.
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
            La vente échoue dès la première étape kernel (vérification du stock), car Kernel rejette
            le grant OAuth2 <code className="bg-white px-1 font-mono text-xs">client_credentials</code>{" "}
            utilisé pour le jeton machine-à-machine (
            <code className="bg-white px-1 font-mono text-xs">Unsupported grant_type</code> sur{" "}
            <code className="bg-white px-1 font-mono text-xs">/oauth2/token</code>). PharmaCore appelant
            Business Core par clé API (jamais de JWT délégué), toutes les étapes kernel de la vente sont
            concernées. Ce n&apos;est pas un bug de PharmaCore ni de Business Core — c&apos;est un
            blocage d&apos;infrastructure Kernel, confirmé et remonté à l&apos;équipe.
          </p>
          <p className="mt-2 text-xs text-muted">
            Détail technique complet :{" "}
            <code className="font-mono">FEUILLE-DE-ROUTE.md §8</code> (racine du dépôt)
          </p>
        </div>
      </div>
    </div>
  );
}
