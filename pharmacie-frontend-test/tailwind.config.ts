import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    // Système "carte élevée douce" — arrondis généreux mais mesurés, cohérents à toutes les échelles
    // (refonte visuelle, cf. AUDIT-PHARMACORE.md décision produit : abandon du style "angles nets").
    borderRadius: {
      none: "0",
      sm: "8px",
      DEFAULT: "10px",
      md: "12px",
      lg: "14px",
      xl: "18px",
      "2xl": "22px",
      "3xl": "28px",
      full: "9999px",
    },
    extend: {
      colors: {
        ink: "#0B2A1C",
        "ink-soft": "#12351F",
        body: "#111827",
        muted: "#5B6B62",
        line: "#DCE7DF",
        "line-soft": "#E9F1EC",
        subtle: "#F3F8F5",
        canvas: "#EFF6F1",
        "brand-tint": "#ECFDF5",
        brand: {
          DEFAULT: "#16A34A",
          hover: "#128038",
          deep: "#0E6B33",
          light: "#34D399",
        },
        ok: "#157347",
        danger: "#B42318",
        warning: "#B45309",
      },
      fontFamily: {
        display: ["var(--font-display)", "ui-sans-serif", "system-ui", "sans-serif"],
        sans: ["var(--font-sans)", "ui-sans-serif", "system-ui", "sans-serif"],
        mono: ["var(--font-mono)", "ui-monospace", "SFMono-Regular", "monospace"],
      },
      boxShadow: {
        // Ombres douces, teintées vert très légèrement (jamais du gris neutre) — profondeur "premium".
        card: "0 1px 2px rgba(11,42,28,.04), 0 1px 1px rgba(11,42,28,.03)",
        "card-hover": "0 12px 28px -10px rgba(11,42,28,.18), 0 4px 10px -4px rgba(11,42,28,.08)",
        pop: "0 20px 44px -12px rgba(11,42,28,.22), 0 8px 20px -8px rgba(11,42,28,.12)",
        glow: "0 8px 20px -6px rgba(22,163,74,.45)",
        "glow-lg": "0 16px 40px -12px rgba(22,163,74,.5)",
        inset: "inset 0 1px 2px rgba(11,42,28,.05)",
      },
      backgroundImage: {
        "brand-gradient": "linear-gradient(135deg, #16A34A 0%, #0E6B33 100%)",
        "ink-gradient": "linear-gradient(160deg, #0B2A1C 0%, #133924 55%, #0B2A1C 100%)",
      },
      keyframes: {
        "fade-up": {
          from: { opacity: "0", transform: "translateY(8px)" },
          to: { opacity: "1", transform: "translateY(0)" },
        },
        "scale-in": {
          from: { opacity: "0", transform: "scale(.97)" },
          to: { opacity: "1", transform: "scale(1)" },
        },
      },
      animation: {
        "fade-up": "fade-up .4s cubic-bezier(.22,1,.36,1) both",
        "scale-in": "scale-in .2s cubic-bezier(.22,1,.36,1) both",
      },
    },
  },
  plugins: [],
};

export default config;
