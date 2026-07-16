import { cn } from "@/lib/cn";
import type { HttpMethod } from "@/lib/endpoints";

const MAP: Record<HttpMethod, string> = {
  GET: "text-brand border-brand/30 bg-brand/5",
  POST: "text-ink border-ink/25 bg-ink/5",
  PUT: "text-muted border-line bg-white",
  PATCH: "text-muted border-line bg-white",
  DELETE: "text-danger border-danger/30 bg-danger/5",
};

export default function HttpBadge({ method }: { method: HttpMethod }) {
  return (
    <span
      className={cn(
        "inline-block min-w-[54px] border px-1.5 py-0.5 text-center font-mono text-[11px] font-medium",
        MAP[method]
      )}
    >
      {method}
    </span>
  );
}
