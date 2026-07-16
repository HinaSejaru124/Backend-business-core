import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

export default function Section({
  children,
  className,
  alt = false,
  id,
}: {
  children: ReactNode;
  className?: string;
  alt?: boolean;
  id?: string;
}) {
  return (
    <section id={id} className={cn("py-16 md:py-24", alt && "border-y border-line bg-white", className)}>
      {children}
    </section>
  );
}
