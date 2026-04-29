import type { NextConfig } from "next";

// PWA(serwist)는 Next.js 16 + Turbopack 안정 지원 시까지 보류 — manifest.ts와 메타만 노출.
const nextConfig: NextConfig = {
  reactStrictMode: true,
};

export default nextConfig;
