import { Navbar } from "@/components/landing/Navbar";
import { Hero } from "@/components/landing/Hero";
import { Features } from "@/components/landing/Features";
import { Footer } from "@/components/landing/Footer";

export default function Home() {
  return (
    <main className="min-h-screen bg-white font-sans selection:bg-blue-100 selection:text-blue-900">
      <Navbar />
      <Hero />
      <Features />
      <div id="testimonials" className="py-20 bg-gray-50 text-center">
        <h2 className="text-3xl font-bold text-gray-900 mb-4">Được tin dùng bởi hơn 500+ phòng khám</h2>
        <p className="text-gray-600 mb-12">Giải pháp của chúng tôi đã giúp hàng ngàn bác sĩ nâng cao hiệu quả công việc</p>
        <div className="flex flex-wrap justify-center gap-12 opacity-50 grayscale hover:grayscale-0 transition-all duration-500">
          {/* Placeholder Logos */}
          <div className="text-2xl font-bold font-serif">MediCare</div>
          <div className="text-2xl font-bold font-mono">HealthPlus</div>
          <div className="text-2xl font-bold font-sans">Dr.Smith</div>
          <div className="text-2xl font-bold">FamilyClinic</div>
          <div className="text-2xl font-bold font-serif">DentalProp</div>
        </div>
      </div>
      <div id="contact">
        <Footer />
      </div>
    </main>
  );
}
