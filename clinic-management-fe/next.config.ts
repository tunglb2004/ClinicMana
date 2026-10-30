import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  devIndicators: false, // Tắt dev indicator để tránh nextjs-portal 0x0 trong DOM
};

export default nextConfig;
