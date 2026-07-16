import type { InputHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

export default function Field({
  label,
  id,
  className,
  hint,
  ...rest
}: { label: string; hint?: string } & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label htmlFor={id} className="block">
      <span className="mb-1.5 block text-[13px] font-medium text-ink">{label}</span>
      <input
        id={id}
        className={cn(
          "h-11 w-full rounded-xl border border-line bg-white px-3.5 text-sm text-body outline-none transition-all placeholder:text-muted/60 focus:border-brand focus:ring-4 focus:ring-brand/10",
          className
        )}
        {...rest}
      />
      {hint && <span className="mt-1.5 block text-xs text-muted">{hint}</span>}
    </label>
  );
}
