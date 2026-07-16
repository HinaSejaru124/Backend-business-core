import { cn } from "@/lib/cn";

/** Statuts couverts : ACTIF/RETIRE, VALIDE/UTILISEE/EXPIREE, BROUILLON/ENVOYEE/RECUE/ANNULEE,
 * et statut de vente COMPLETEE/EN_COURS/COMPENSEE (cf. frontend-test.md §2.4). */
const STYLES: Record<string, string> = {
  ACTIF: "text-ok border-ok/30 bg-ok/5",
  VALIDE: "text-ok border-ok/30 bg-ok/5",
  RECUE: "text-ok border-ok/30 bg-ok/5",
  COMPLETEE: "text-ok border-ok/30 bg-ok/5",
  RETIRE: "text-muted border-line bg-subtle",
  ANNULEE: "text-muted border-line bg-subtle",
  UTILISEE: "text-muted border-line bg-subtle",
  EXPIREE: "text-danger border-danger/30 bg-danger/5",
  COMPENSEE: "text-danger border-danger/30 bg-danger/5",
  BROUILLON: "text-brand border-brand/30 bg-brand-tint",
  ENVOYEE: "text-brand border-brand/30 bg-brand-tint",
  EN_COURS: "text-brand border-brand/30 bg-brand-tint",
};

export default function Badge({ value }: { value: string }) {
  const style = STYLES[value] ?? "text-muted border-line bg-subtle";
  return (
    <span className={cn("inline-block rounded-full border px-2.5 py-0.5 font-mono text-[11px] font-semibold", style)}>
      {value}
    </span>
  );
}
