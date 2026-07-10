import type { ReactNode } from "react";
import Container from "./Container";

export default function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="border-b border-line bg-white">
      <Container className="py-12 md:py-16">
        <div className="flex flex-col justify-between gap-6 md:flex-row md:items-end">
          <div>
            {eyebrow && (
              <div className="font-mono text-[12px] uppercase tracking-wider text-brand">{eyebrow}</div>
            )}
            <h1 className="mt-3 font-display text-[clamp(28px,4vw,40px)] font-bold text-ink">{title}</h1>
            {description && <p className="mt-3 max-w-2xl text-lg text-muted">{description}</p>}
          </div>
          {actions && <div className="flex flex-none items-center gap-3">{actions}</div>}
        </div>
      </Container>
    </div>
  );
}
