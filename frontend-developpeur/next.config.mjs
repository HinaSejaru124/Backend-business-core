/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // Sortie autonome (serveur Node minimal) — requis pour l'image Docker multi-stage.
  output: "standalone",
};

export default nextConfig;
