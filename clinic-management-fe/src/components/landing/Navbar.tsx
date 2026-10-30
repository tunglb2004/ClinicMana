"use client";

import Link from "next/link";
import { useState, useEffect } from "react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

export function Navbar() {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20);
    };
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <nav
      className={cn(
        "fixed top-0 left-0 right-0 z-50 transition-all duration-300",
        scrolled
          ? "bg-white/90 backdrop-blur-md shadow-sm border-b border-gray-100 py-3"
          : "bg-transparent py-5"
      )}
    >
      <div className="max-w-7xl mx-auto px-6 flex items-center justify-between">
        <Link href="/" className="flex items-center gap-2 group">
          <div className="h-10 w-10 rounded-xl bg-gradient-to-br from-blue-600 to-blue-800 flex items-center justify-center text-white font-bold text-xl shadow-lg shadow-blue-500/30 group-hover:scale-105 transition-transform duration-300">
            CP
          </div>
          <span className={cn("font-bold text-xl", scrolled ? "text-gray-900" : "text-gray-900")}>
            ClinicPro
          </span>
        </Link>

        <div className="hidden md:flex items-center gap-8">
          <Link href="#features" className="text-gray-600 hover:text-blue-600 font-medium transition-colors">
            Tính năng
          </Link>
          <Link href="#services" className="text-gray-600 hover:text-blue-600 font-medium transition-colors">
            Dịch vụ
          </Link>
          <Link href="#testimonials" className="text-gray-600 hover:text-blue-600 font-medium transition-colors">
            Khách hàng
          </Link>
          <Link href="#contact" className="text-gray-600 hover:text-blue-600 font-medium transition-colors">
            Liên hệ
          </Link>
        </div>

        <div className="flex items-center gap-4">
          <Link href="/login" className="hidden md:inline-block font-semibold text-gray-700 hover:text-blue-600 transition-colors">
            Đăng nhập
          </Link>
          <Link href="/register">
             <Button className="rounded-full bg-blue-600 hover:bg-blue-700 text-white px-6 shadow-lg shadow-blue-500/30 hover:shadow-blue-500/40 transition-all hover:-translate-y-0.5">
               Dùng thử miễn phí
             </Button>
          </Link>
        </div>
      </div>
    </nav>
  );
}
