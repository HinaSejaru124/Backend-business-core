import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    // Angles nets partout : on force TOUS les rayons à 0 (même rounded-full).
    borderRadius: {
      none: "0",
      sm: "0",
      DEFAULT: "0",
      md: "0",
      lg: "0",
      xl: "0",
      "2xl": "0",
      "3xl": "0",
      full: "0",
    },
    extend: {
      colors: {
        ink: "#0B1B3A", // navy — titres, nav foncée, blocs de code
        body: "#101828", // texte principal
        muted: "#5A6473", // texte secondaire
        line: "#E2E7F0", // bordure hairline
        subtle: "#F4F6FA", // fond de section alterné
        tint: "#ECF1FE", // fond bleuté discret
        brand: {
          DEFAULT: "#1B4DF5", // bleu primaire
          hover: "#163FCC",
        },
        ok: "#157347",
        danger: "#B42318",
      },
      fontFamily: {
        display: ["var(--font-display)", "ui-sans-serif", "system-ui", "sans-serif"],
        sans: ["var(--font-sans)", "ui-sans-serif", "system-ui", "sans-serif"],
        mono: ["var(--font-mono)", "ui-monospace", "SFMono-Regular", "monospace"],
      },
      maxWidth: {
        container: "1200px",
      },
      boxShadow: {
        card: "0 1px 2px rgba(11,27,58,.05)",
        pop: "0 8px 28px rgba(11,27,58,.10)",
      },
      keyframes: {
        "fade-up": {
          from: { opacity: "0", transform: "translateY(8px)" },
          to: { opacity: "1", transform: "translateY(0)" },
        },
      },
      animation: {
        "fade-up": "fade-up .5s ease-out both",
      },
    },
  },
  plugins: [],
};

export default config;
