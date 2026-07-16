import type { SelectHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

export default function Select({
  label,
  id,
  className,
  children,
  ...rest
}: { label: string } & SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <label htmlFor={id} className="block">
      <span className="mb-1.5 block text-[13px] font-medium text-ink">{label}</span>
      <select
        id={id}
        className={cn(
          "h-11 w-full rounded-xl border border-line bg-white px-3 text-sm text-body outline-none transition-all focus:border-brand focus:ring-4 focus:ring-brand/10",
          className
        )}
        {...rest}
      >
        {children}
      </select>
    </label>
  );
}
