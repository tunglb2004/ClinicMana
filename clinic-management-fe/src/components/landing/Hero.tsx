"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { ArrowRight, CheckCircle2 } from "lucide-react";

export function Hero() {
    return (
        <section className="relative pt-32 pb-20 lg:pt-48 lg:pb-32 overflow-hidden bg-gradient-to-b from-blue-50/70 via-white to-blue-100/50">
            {/* Background blobs */}
            <div className="absolute top-20 right-0 -mr-20 -mt-20 w-96 h-96 rounded-full bg-blue-100/50 blur-3xl opacity-50 animate-pulse" />
            <div className="absolute bottom-0 left-0 -ml-20 -mb-20 w-80 h-80 rounded-full bg-blue-200/50 blur-3xl opacity-50" />

            <div className="max-w-7xl mx-auto px-6 relative z-10">
                <div className="grid lg:grid-cols-2 gap-16 items-center">
                    <div className="max-w-2xl">
                        <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-blue-50 text-blue-700 font-semibold text-sm mb-6 border border-blue-100">
                            <span className="relative flex h-2 w-2">
                                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
                                <span className="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
                            </span>
                            Giải pháp quản lý phòng khám 4.0
                        </div>

                        <h1 className="text-5xl lg:text-7xl font-bold text-gray-900 leading-[1.1] mb-8 tracking-tight">
                            Quản lý thông minh <br />
                            <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-blue-800">
                                Hiệu quả vượt trội
                            </span>
                        </h1>

                        <p className="text-xl text-gray-600 mb-10 leading-relaxed">
                            Hệ thống toàn diện giúp tối ưu hóa quy trình khám chữa bệnh,
                            quản lý hồ sơ bệnh nhân và nâng cao chất lượng dịch vụ y tế của bạn.
                        </p>

                        <div className="flex flex-col sm:flex-row gap-4 mb-12">
                            <Link href="/register">
                                <Button size="lg" className="w-full sm:w-auto rounded-full text-lg h-14 px-8 bg-gradient-to-r from-blue-600 to-blue-800 shadow-xl shadow-blue-500/20 hover:shadow-blue-500/30 transition-all hover:-translate-y-1 hover:from-blue-700 hover:to-blue-900">
                                    Bắt đầu ngay
                                    <ArrowRight className="ml-2 h-5 w-5" />
                                </Button>
                            </Link>
                            <Link href="#features">
                                <Button variant="outline" size="lg" className="w-full sm:w-auto rounded-full text-lg h-14 px-8 border-gray-300 hover:bg-gray-50 hover:text-gray-900 transition-all hover:-translate-y-1">
                                    Tìm hiểu thêm
                                </Button>
                            </Link>
                        </div>

                        <div className="flex gap-8 text-sm text-gray-600 font-medium">
                            <div className="flex items-center gap-2">
                                <CheckCircle2 className="h-5 w-5 text-blue-600" />
                                <span>Thiết lập nhanh chóng</span>
                            </div>
                            <div className="flex items-center gap-2">
                                <CheckCircle2 className="h-5 w-5 text-blue-600" />
                                <span>Bảo mật tuyệt đối</span>
                            </div>
                            <div className="flex items-center gap-2">
                                <CheckCircle2 className="h-5 w-5 text-blue-600" />
                                <span>Hỗ trợ 24/7</span>
                            </div>
                        </div>
                    </div>

                    <div className="relative lg:h-[600px] w-full hidden lg:block">
                        {/* Abstract Graphic / Placeholder for UI mockup */}
                        <div className="absolute inset-0 bg-gradient-to-tr from-blue-100 to-blue-50 rounded-3xl transform rotate-3 shadow-2xl border border-white/50 backdrop-blur-sm overflow-hidden flex items-center justify-center">
                            <div className="text-center p-12">
                                <div className="w-24 h-24 bg-white rounded-2xl shadow-xl mx-auto mb-6 flex items-center justify-center">
                                    <svg className="w-12 h-12 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"></path></svg>
                                </div>
                                <div className="space-y-4">
                                    <div className="h-4 bg-white/60 rounded-full w-48 mx-auto"></div>
                                    <div className="h-4 bg-white/60 rounded-full w-32 mx-auto"></div>
                                </div>
                            </div>
                            {/* Floating cards */}
                            <div className="absolute top-12 left-12 bg-white p-4 rounded-xl shadow-lg border border-gray-100 transform -rotate-6 animate-bounce duration-[3000ms]">
                                <div className="flex items-center gap-3">
                                    <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center text-blue-700">
                                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                                    </div>
                                    <div>
                                        <p className="text-xs text-gray-500">Trạng thái</p>
                                        <p className="font-bold text-gray-900">Hoạt động tốt</p>
                                    </div>
                                </div>
                            </div>
                            <div className="absolute bottom-20 right-12 bg-white p-4 rounded-xl shadow-lg border border-gray-100 transform rotate-6 animate-pulse">
                                <div className="flex items-center gap-3">
                                    <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center text-blue-600">
                                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>
                                    </div>
                                    <div>
                                        <p className="text-xs text-gray-500">Bệnh nhân mới</p>
                                        <p className="font-bold text-gray-900">+128 Tuần này</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className="absolute inset-0 bg-blue-600/5 rounded-3xl transform -rotate-3 z-[-1]"></div>
                    </div>
                </div>
            </div>
        </section>
    );
}
