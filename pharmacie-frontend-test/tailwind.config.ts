import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    // Radius très faible partout (2-4px) — jamais arrondi excessif, jamais 0 non plus
    // (cf. frontend-test.md §2.3 : différent du style "angles nets" à 0 du frontend développeur).
    borderRadius: {
      none: "0",
      sm: "2px",
      DEFAULT: "3px",
      md: "4px",
      lg: "4px",
      xl: "4px",
      "2xl": "4px",
      "3xl": "4px",
      full: "4px",
    },
    extend: {
      colors: {
        ink: "#0B2A1C",
        body: "#111827",
        muted: "#5B6B62",
        line: "#DCE7DF",
        subtle: "#F4F8F5",
        "brand-tint": "#ECFDF5",
        brand: {
          DEFAULT: "#16A34A",
          hover: "#128038",
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
        card: "0 1px 2px rgba(11,42,28,.06)",
        pop: "0 8px 24px rgba(11,42,28,.10)",
      },
      keyframes: {
        "fade-up": {
          from: { opacity: "0", transform: "translateY(8px)" },
          to: { opacity: "1", transform: "translateY(0)" },
        },
      },
      animation: {
        "fade-up": "fade-up .4s ease-out both",
      },
    },
  },
  plugins: [],
};

export default config;
