import Link from "next/link";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";

type Variant = "primary" | "secondary" | "ghost" | "success" | "danger";
type Size = "sm" | "md";

const BASE =
  "inline-flex items-center justify-center gap-2 rounded-lg border font-semibold whitespace-nowrap transition-all duration-200 ease-out select-none active:translate-y-px disabled:opacity-50 disabled:pointer-events-none";

const VARIANTS: Record<Variant, string> = {
  primary:
    "bg-brand text-white border-brand shadow-glow-sm hover:bg-brand-hover hover:border-brand-hover hover:shadow-glow hover:-translate-y-0.5",
  secondary: "bg-white text-ink border-line hover:border-ink hover:-translate-y-0.5 hover:shadow-card",
  ghost: "bg-transparent text-ink border-transparent hover:bg-tint",
  // Vert plein — action positive / état actif.
  success: "bg-ok text-white border-ok hover:bg-ok-strong hover:border-ok-strong hover:-translate-y-0.5 hover:shadow-card",
  // Rouge plein — action destructive (révocation).
  danger: "bg-danger text-white border-danger hover:bg-[#9c1e15] hover:border-[#9c1e15] hover:-translate-y-0.5 hover:shadow-card",
};

const SIZES: Record<Size, string> = {
  sm: "h-9 px-3.5 text-[13px]",
  md: "h-11 px-5 text-sm",
};

export function Button({
  variant = "primary",
  size = "md",
  className,
  children,
  ...rest
}: {
  variant?: Variant;
  size?: Size;
  children: ReactNode;
} & ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button className={cn(BASE, VARIANTS[variant], SIZES[size], className)} {...rest}>
      {children}
    </button>
  );
}

export function ButtonLink({
  href,
  variant = "primary",
  size = "md",
  className,
  children,
  external = false,
  onClick,
}: {
  href: string;
  variant?: Variant;
  size?: Size;
  className?: string;
  children: ReactNode;
  external?: boolean;
  onClick?: () => void;
}) {
  const cls = cn(BASE, VARIANTS[variant], SIZES[size], className);
  if (external) {
    return (
      <a href={href} className={cls} target="_blank" rel="noreferrer" onClick={onClick}>
        {children}
      </a>
    );
  }
  return (
    <Link href={href} className={cls} onClick={onClick}>
      {children}
    </Link>
  );
}
