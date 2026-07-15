"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";
import { cn } from "@/lib/cn";
import { IconCopy, IconCheck } from "./icons";

export default function CodeWindow({
  filename,
  lang,
  copyText,
  children,
  className,
  typeText,
  typeSpeed = 14,
}: {
  filename?: string;
  lang?: string;
  copyText?: string;
  children?: ReactNode;
  className?: string;
  typeText?: string;
  typeSpeed?: number;
}) {
  const typing = typeof typeText === "string";
  const [shown, setShown] = useState("");
  const [done, setDone] = useState(!typing);
  const [copied, setCopied] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const started = useRef(false);

  useEffect(() => {
    if (!typing) return;
    const el = ref.current;
    if (!el) return;
    const io = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && !started.current) {
          started.current = true;
          let i = 0;
          const id = window.setInterval(() => {
            i += 1;
            setShown(typeText!.slice(0, i));
            if (i >= typeText!.length) {
              window.clearInterval(id);
              setDone(true);
            }
          }, typeSpeed);
        }
      },
      { threshold: 0.3 }
    );
    io.observe(el);
    return () => io.disconnect();
  }, [typing, typeText, typeSpeed]);

  const text = copyText ?? (typing ? typeText : undefined);

  async function copy() {
    if (!text) return;
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 1400);
    } catch {
      /* ignore */
    }
  }

  return (
    <div
      ref={ref}
      className={cn(
        "group overflow-hidden rounded-2xl border border-ink/20 bg-ink text-slate-200 shadow-glow transition-all duration-300 ease-out hover:-translate-y-1.5 hover:border-brand/50 hover:shadow-[0_40px_80px_-20px_rgba(27,77,245,.45)]",
        className
      )}
    >
      <div className="flex items-center justify-between border-b border-white/10 px-4 py-2.5">
        <div className="flex items-center gap-3 font-mono text-[12px] text-slate-400">
          <span className="flex gap-1.5" aria-hidden>
            <span className="h-2.5 w-2.5 rounded-full bg-[#FF5F57]/70 transition-colors group-hover:bg-[#FF5F57]" />
            <span className="h-2.5 w-2.5 rounded-full bg-[#FEBC2E]/70 transition-colors group-hover:bg-[#FEBC2E]" />
            <span className="h-2.5 w-2.5 rounded-full bg-[#28C840]/70 transition-colors group-hover:bg-[#28C840]" />
          </span>
          {filename && <span>{filename}</span>}
        </div>
        <div className="flex items-center gap-3">
          {lang && (
            <span className="font-mono text-[11px] uppercase tracking-wider text-slate-500">{lang}</span>
          )}
          {text && (
            <button
              onClick={copy}
              className="inline-flex items-center gap-1.5 font-mono text-[11px] text-slate-400 transition-colors hover:text-white"
              aria-label="Copier le code"
            >
              {copied ? <IconCheck className="h-3.5 w-3.5 text-brand" /> : <IconCopy className="h-3.5 w-3.5" />}
              {copied ? "Copié" : "Copier"}
            </button>
          )}
        </div>
      </div>
      <div className="no-scrollbar overflow-x-auto p-4">
        <pre className="whitespace-pre font-mono text-[13px] leading-relaxed">
          {typing ? shown : children}
          {typing && !done && <span className="caret" aria-hidden />}
        </pre>
      </div>
    </div>
  );
}
