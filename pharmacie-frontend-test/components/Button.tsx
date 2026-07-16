import Link from "next/link";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";

type Variant = "primary" | "secondary" | "danger" | "ghost";
type Size = "sm" | "md";

const BASE =
  "inline-flex items-center justify-center gap-2 rounded-xl border font-semibold whitespace-nowrap transition-all duration-200 ease-out select-none active:translate-y-px disabled:opacity-50 disabled:pointer-events-none";

const VARIANTS: Record<Variant, string> = {
  primary:
    "bg-brand text-white border-brand shadow-glow hover:bg-brand-hover hover:border-brand-hover hover:shadow-glow-lg hover:-translate-y-0.5",
  secondary: "bg-white text-ink border-line hover:border-brand/40 hover:-translate-y-0.5 hover:shadow-card",
  danger: "bg-white text-danger border-danger/30 hover:bg-danger hover:text-white hover:border-danger",
  ghost: "bg-transparent text-ink border-transparent hover:bg-brand-tint",
};

const SIZES: Record<Size, string> = {
  sm: "h-10 px-4 text-[13.5px]",
  md: "h-12 px-6 text-[15px]",
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
}: {
  href: string;
  variant?: Variant;
  size?: Size;
  className?: string;
  children: ReactNode;
}) {
  return (
    <Link href={href} className={cn(BASE, VARIANTS[variant], SIZES[size], className)}>
      {children}
    </Link>
  );
}
