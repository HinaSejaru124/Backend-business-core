import Container from "@/components/Container";
import { ButtonLink } from "@/components/Button";

export default function NotFound() {
  return (
    <Container className="flex min-h-[60vh] flex-col items-center justify-center py-24 text-center">
      <div className="font-mono text-sm uppercase tracking-widest text-brand">Erreur 404</div>
      <h1 className="mt-4 font-display text-[clamp(36px,6vw,56px)] font-bold text-ink">Page introuvable</h1>
      <p className="mt-3 max-w-md text-muted">
        La page que vous cherchez n&apos;existe pas ou a été déplacée.
      </p>
      <div className="mt-8 flex flex-wrap justify-center gap-3">
        <ButtonLink href="/">Retour à l&apos;accueil</ButtonLink>
        <ButtonLink href="/docs" variant="secondary">
          Documentation
        </ButtonLink>
      </div>
    </Container>
  );
}
