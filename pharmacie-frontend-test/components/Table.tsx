import type { ReactNode } from "react";

/** Wrapper de style commun ; chaque page écrit son propre thead/tbody. */
export default function Table({ children }: { children: ReactNode }) {
  return (
    <div className="overflow-x-auto rounded-2xl border border-line bg-white shadow-card">
      <table className="w-full min-w-[640px] border-collapse text-sm">{children}</table>
    </div>
  );
}

export function Th({ children }: { children?: ReactNode }) {
  return (
    <th className="border-b border-line bg-subtle px-5 py-3.5 text-left font-mono text-[11.5px] font-semibold uppercase tracking-wider text-ink/70">
      {children}
    </th>
  );
}

export function Td({ children, className }: { children: ReactNode; className?: string }) {
  return <td className={`px-5 py-4 text-[15px] text-body ${className ?? ""}`}>{children}</td>;
}

export function EmptyRow({ colSpan, children }: { colSpan: number; children: ReactNode }) {
  return (
    <tr>
      <td colSpan={colSpan} className="px-4 py-8 text-center text-sm text-muted">
        {children}
      </td>
    </tr>
  );
}
