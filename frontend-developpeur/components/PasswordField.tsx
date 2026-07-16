"use client";

import { useState, type InputHTMLAttributes } from "react";
import { cn } from "@/lib/cn";
import { IconEye, IconEyeOff } from "./icons";

export default function PasswordField({
  label,
  id,
  className,
  ...rest
}: { label: string } & InputHTMLAttributes<HTMLInputElement>) {
  const [show, setShow] = useState(false);
  return (
    <label htmlFor={id} className="block">
      <span className="mb-1.5 block text-[13px] font-medium text-ink">{label}</span>
      <div className="relative">
        <input
          id={id}
          type={show ? "text" : "password"}
          className={cn(
            "h-11 w-full border border-line bg-white pl-3.5 pr-11 text-sm text-body outline-none transition-colors placeholder:text-muted/60 focus:border-brand",
            className
          )}
          {...rest}
        />
        <button
          type="button"
          onClick={() => setShow((s) => !s)}
          className="absolute right-0 top-0 grid h-11 w-11 place-items-center text-muted transition-colors hover:text-ink"
          aria-label={show ? "Masquer le mot de passe" : "Afficher le mot de passe"}
        >
          {show ? <IconEyeOff className="h-4 w-4" /> : <IconEye className="h-4 w-4" />}
        </button>
      </div>
    </label>
  );
}
