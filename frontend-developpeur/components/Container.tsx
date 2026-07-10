import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

export default function Container({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return <div className={cn("mx-auto w-full max-w-container px-6 md:px-8", className)}>{children}</div>;
}
