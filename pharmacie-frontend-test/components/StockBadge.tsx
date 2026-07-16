import { cn } from "@/lib/cn";

/** Vert = OK, orange = seuil bas, rouge = rupture (cf. frontend-test.md §2.4). */
export default function StockBadge({ stock, seuil }: { stock: number; seuil: number }) {
  const rupture = stock <= 0;
  const bas = !rupture && stock <= seuil;
  const style = rupture
    ? "text-danger border-danger/30 bg-danger/5"
    : bas
      ? "text-warning border-warning/30 bg-warning/5"
      : "text-ok border-ok/30 bg-ok/5";
  const label = rupture ? "Rupture" : bas ? "Stock bas" : "Stock OK";
  return (
    <span className={cn("inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 font-mono text-[11px] font-semibold", style)}>
      {stock} <span className="opacity-70">· {label}</span>
    </span>
  );
}
