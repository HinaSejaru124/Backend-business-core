import type { ReactNode } from "react";
import Link from "next/link";
import { cn } from "@/lib/cn";
import { IconCheck } from "@/components/icons";

type BannerVariant = "error" | "success" | "info" | "warning";

const BANNER_STYLES: Record<BannerVariant, string> = {
  error: "border-danger bg-danger/5 text-danger",
  success: "border-ok bg-ok/5 text-ink",
  info: "border-brand bg-tint text-ink",
  warning: "border-brand bg-tint text-ink",
};

export function Banner({
  variant = "info",
  children,
  className,
}: {
  variant?: BannerVariant;
  children: ReactNode;
  className?: string;
}) {
  return (
    <p className={cn("border-l-2 px-3 py-2 text-sm", BANNER_STYLES[variant], className)}>
      {children}
    </p>
  );
}

export function EmptyState({
  title,
  description,
  actionHref,
  actionLabel,
}: {
  title: string;
  description: string;
  actionHref?: string;
  actionLabel?: string;
}) {
  return (
    <div className="border border-dashed border-line bg-white p-6 text-sm">
      <div className="font-medium text-ink">{title}</div>
      <p className="mt-1 text-muted">{description}</p>
      {actionHref && actionLabel && (
        <Link
          href={actionHref}
          className="mt-3 inline-block text-sm font-medium text-brand underline-offset-2 hover:underline"
        >
          {actionLabel}
        </Link>
      )}
    </div>
  );
}

export function LoadingBlock({ lines = 3 }: { lines?: number }) {
  return (
    <div className="space-y-3 p-5">
      {Array.from({ length: lines }).map((_, i) => (
        <div key={i} className="h-4 animate-pulse bg-line" style={{ width: `${70 + (i % 3) * 10}%` }} />
      ))}
    </div>
  );
}

export type OnboardingStep = {
  label: string;
  done: boolean;
  href?: string;
};

export function OnboardingSteps({ steps, title = "Parcours de démarrage" }: { steps: OnboardingStep[]; title?: string }) {
  const remaining = steps.filter((s) => !s.done).length;
  if (remaining === 0) return null;

  return (
    <div className="border border-line bg-white p-5">
      <h3 className="font-display text-[15px] font-semibold text-ink">{title}</h3>
      <ul className="mt-4 space-y-3">
        {steps.map((step) => (
          <li key={step.label} className="flex items-start gap-3 text-sm">
            <span
              className={cn(
                "mt-0.5 grid h-5 w-5 flex-none place-items-center border text-[11px]",
                step.done ? "border-ok bg-ok/10 text-ok" : "border-line bg-subtle text-muted"
              )}
            >
              {step.done ? <IconCheck className="h-3 w-3" /> : "·"}
            </span>
            <div className="min-w-0 flex-1">
              <span className={step.done ? "text-muted line-through" : "text-ink"}>{step.label}</span>
              {!step.done && step.href && (
                <Link
                  href={step.href}
                  className="ml-2 text-brand underline-offset-2 hover:underline"
                >
                  Commencer →
                </Link>
              )}
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
