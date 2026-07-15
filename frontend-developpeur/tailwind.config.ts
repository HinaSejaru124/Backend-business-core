import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    // Angles À PEINE arrondis — on garde l'impression du carré, pas du "pilule".
    // full (9999px) reste réservé aux badges/avatars réellement circulaires.
    borderRadius: {
      none: "0",
      sm: "2px",
      DEFAULT: "3px",
      md: "4px",
      lg: "5px",
      xl: "6px",
      "2xl": "8px",
      "3xl": "10px",
      full: "9999px",
    },
    extend: {
      colors: {
        ink: "#0A1730", // navy TRÈS foncé — grands titres
        body: "#0F1B33", // texte principal (plus foncé qu'avant, mieux lisible)
        muted: "#48546B", // texte secondaire (assombri pour le contraste)
        line: "#E2E7F0", // bordure hairline
        subtle: "#F4F6FA", // fond de section alterné
        tint: "#ECF1FE", // fond bleuté discret
        brand: {
          DEFAULT: "#1B4DF5", // bleu primaire
          hover: "#163FCC",
        },
        // Vert d'accent (état actif / consommation) — reprend l'esprit de la maquette.
        ok: "#0E9F6E",
        "ok-strong": "#047857",
        "ok-tint": "#E6F6EF",
        danger: "#B42318",
      },
      fontFamily: {
        // display et sans partagent la même police ronde : cohérence totale de l'UI.
        display: ["var(--font-sans)", "ui-sans-serif", "system-ui", "sans-serif"],
        sans: ["var(--font-sans)", "ui-sans-serif", "system-ui", "sans-serif"],
        mono: ["var(--font-mono)", "ui-monospace", "SFMono-Regular", "monospace"],
      },
      maxWidth: {
        container: "1200px",
      },
      boxShadow: {
        card: "0 1px 2px rgba(11,27,58,.05)",
        pop: "0 8px 28px rgba(11,27,58,.10)",
        glow: "0 30px 70px -20px rgba(27,77,245,.35), 0 10px 30px -10px rgba(11,27,58,.25)",
        "glow-sm": "0 12px 30px -8px rgba(27,77,245,.28)",
      },
      keyframes: {
        "fade-up": {
          from: { opacity: "0", transform: "translateY(8px)" },
          to: { opacity: "1", transform: "translateY(0)" },
        },
        float: {
          "0%, 100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-10px)" },
        },
        "pulse-soft": {
          "0%, 100%": { opacity: "1" },
          "50%": { opacity: ".55" },
        },
      },
      animation: {
        "fade-up": "fade-up .5s ease-out both",
        float: "float 6s ease-in-out infinite",
        "pulse-soft": "pulse-soft 2.2s ease-in-out infinite",
      },
    },
  },
  plugins: [],
};

export default config;
