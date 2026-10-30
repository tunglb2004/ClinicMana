"use client";

import { ShieldCheck, Zap, HeartHandshake, Laptop, Activity, Users } from "lucide-react";

const features = [
    {
        icon: ShieldCheck,
        title: "Bảo mật tuyệt đối",
        description: "Hệ thống bảo mật đa lớp, mã hóa dữ liệu đầu cuối đảm bảo an toàn thông tin bệnh nhân.",
        wrapperClass: "bg-blue-50",
        iconClass: "text-blue-600"
    },
    {
        icon: Zap,
        title: "Tốc độ vượt trội",
        description: "Xử lý dữ liệu tức thì, tối ưu hóa hiệu suất làm việc cho đội ngũ y bác sĩ.",
        wrapperClass: "bg-blue-100",
        iconClass: "text-blue-700"
    },
    {
        icon: HeartHandshake,
        title: "Thân thiện người dùng",
        description: "Giao diện trực quan, dễ sử dụng, giảm thiểu thời gian đào tạo nhân sự.",
        wrapperClass: "bg-blue-50",
        iconClass: "text-blue-800"
    },
    {
        icon: Laptop,
        title: "Đa nền tảng",
        description: "Truy cập mọi lúc mọi nơi trên mọi thiết bị: Máy tính, máy tính bảng, điện thoại.",
        wrapperClass: "bg-blue-100",
        iconClass: "text-blue-600"
    },
    {
        icon: Activity,
        title: "Báo cáo thông minh",
        description: "Hệ thống báo cáo trực quan, giúp quản lý nắm bắt tình hình hoạt động chính xác.",
        wrapperClass: "bg-blue-50",
        iconClass: "text-blue-700"
    },
    {
        icon: Users,
        title: "Quản lý nhân sự",
        description: "Phân quyền chi tiết, theo dõi lịch làm việc và hiệu suất nhân viên dễ dàng.",
        wrapperClass: "bg-blue-100",
        iconClass: "text-blue-800"
    }
];

export function Features() {
    return (
        <section id="features" className="py-24 bg-white relative">
            <div className="max-w-7xl mx-auto px-6">
                <div className="text-center max-w-3xl mx-auto mb-20">
                    <h2 className="text-blue-600 font-bold tracking-wide uppercase text-sm mb-3">Tính năng nổi bật</h2>
                    <h3 className="text-4xl font-bold text-gray-900 mb-6">Mọi thứ bạn cần để vận hành phòng khám</h3>
                    <p className="text-xl text-gray-600">
                        Chúng tôi cung cấp bộ công cụ toàn diện giúp đơn giản hóa quy trình quản lý phức tạp.
                    </p>
                </div>

                <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
                    {features.map((feature, index) => (
                        <div key={index} className="group p-8 rounded-2xl bg-white border border-gray-100 shadow-lg shadow-gray-200/50 hover:shadow-2xl hover:shadow-blue-500/10 hover:-translate-y-1 transition-all duration-300">
                            <div className={`h-14 w-14 rounded-xl flex items-center justify-center mb-6 ${feature.wrapperClass} group-hover:scale-110 transition-transform duration-300`}>
                                <feature.icon className={`h-7 w-7 ${feature.iconClass}`} />
                            </div>
                            <h4 className="text-xl font-bold text-gray-900 mb-3">{feature.title}</h4>
                            <p className="text-gray-600 leading-relaxed">
                                {feature.description}
                            </p>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
}
