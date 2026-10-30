export function Footer() {
    return (
        <footer className="bg-gray-900 text-white pt-20 pb-10">
            <div className="max-w-7xl mx-auto px-6">
                <div className="grid md:grid-cols-4 gap-12 mb-16">
                    <div className="col-span-1 md:col-span-2">
                        <div className="flex items-center gap-2 mb-6">
                            <div className="h-10 w-10 rounded-lg bg-blue-600 flex items-center justify-center font-bold text-xl">
                                CP
                            </div>
                            <span className="font-bold text-2xl">ClinicPro</span>
                        </div>
                        <p className="text-gray-400 max-w-md mb-8 leading-relaxed">
                            Giải pháp quản lý phòng khám toàn diện, mang lại hiệu quả vượt trội cho cơ sở y tế của bạn. Đồng hành cùng sự phát triển của nền y tế số.
                        </p>
                        <div className="flex gap-4">
                            {/* Social icons placeholders */}
                            <div className="h-10 w-10 rounded-full bg-gray-800 hover:bg-blue-600 flex items-center justify-center transition-colors cursor-pointer">
                                <span className="font-bold">fb</span>
                            </div>
                            <div className="h-10 w-10 rounded-full bg-gray-800 hover:bg-blue-500 flex items-center justify-center transition-colors cursor-pointer">
                                <span className="font-bold">tw</span>
                            </div>
                            <div className="h-10 w-10 rounded-full bg-gray-800 hover:bg-blue-700 flex items-center justify-center transition-colors cursor-pointer">
                                <span className="font-bold">in</span>
                            </div>
                        </div>
                    </div>

                    <div>
                        <h4 className="text-lg font-bold mb-6">Sản phẩm</h4>
                        <ul className="space-y-4 text-gray-400">
                            <li><a href="#" className="hover:text-white transition-colors">Tính năng</a></li>
                            <li><a href="#" className="hover:text-white transition-colors">Bảng giá</a></li>
                            <li><a href="#" className="hover:text-white transition-colors">Bảo mật</a></li>
                            <li><a href="#" className="hover:text-white transition-colors">Cập nhật</a></li>
                        </ul>
                    </div>

                    <div>
                        <h4 className="text-lg font-bold mb-6">Hỗ trợ</h4>
                        <ul className="space-y-4 text-gray-400">
                            <li><a href="#" className="hover:text-white transition-colors">Trung tâm trợ giúp</a></li>
                            <li><a href="#" className="hover:text-white transition-colors">Liên hệ</a></li>
                            <li><a href="#" className="hover:text-white transition-colors">Điều khoản</a></li>
                            <li><a href="#" className="hover:text-white transition-colors">Chính sách riêng tư</a></li>
                        </ul>
                    </div>
                </div>

                <div className="border-t border-gray-800 pt-8 flex flex-col md:flex-row items-center justify-between text-gray-500 text-sm">
                    <p>© 2024 ClinicPro. All rights reserved.</p>
                    <div className="flex gap-8 mt-4 md:mt-0">
                        <a href="#" className="hover:text-white transition-colors">Privacy Policy</a>
                        <a href="#" className="hover:text-white transition-colors">Terms of Service</a>
                    </div>
                </div>
            </div>
        </footer>
    );
}
