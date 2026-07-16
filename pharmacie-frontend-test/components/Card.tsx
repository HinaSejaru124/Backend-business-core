import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

export default function Card({
  title,
  action,
  children,
  className,
  tinted,
}: {
  title?: string;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
  /** Fond légèrement vert au lieu de blanc — pour varier la densité de blanc à l'écran. */
  tinted?: boolean;
}) {
  return (
    <div
      className={cn(
        "rounded-2xl border shadow-card transition-shadow duration-200",
        tinted ? "border-brand/15 bg-brand-tint" : "border-line bg-white",
        className
      )}
    >
      {(title || action) && (
        <div className="flex items-center justify-between border-b border-line-soft px-6 py-4">
          {title && <h2 className="font-display text-[17px] font-semibold text-ink">{title}</h2>}
          {action}
        </div>
      )}
      <div className="p-6">{children}</div>
    </div>
  );
}
