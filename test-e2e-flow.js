const fs = require('fs');
const login = async (role, id, pwd) => {
  const res = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({identifier: id, password: pwd})
  });
  if (!res.ok) throw new Error(role + ' login failed: ' + await res.text());
  const data = await res.json();
  return data.accessToken;
};

const req = async (path, method, token, body) => {
    const res = await fetch('http://localhost:8080' + path, {
        method,
        headers: {
            'Content-Type': 'application/json',
            ...(token ? {'Authorization': 'Bearer ' + token} : {})
        },
        ...(body ? {body: JSON.stringify(body)} : {})
    });
    const txt = await res.text();
    if (!res.ok) throw new Error(`${method} ${path} failed: ${txt}`);
    try { return JSON.parse(txt); } catch(e) { return txt; }
};

const run = async () => {
    try {
        console.log("=== BẮT ĐẦU E2E TEST: LUỒNG KHÁM BỆNH CHÍNH ===");
        
        // 1. Logins
        console.log("1. Đang đăng nhập các tài khoản...");
        const patientToken = await login('Patient', '0981730367@patient.clinic', '123456');
        const recepToken = await login('Receptionist', 'letan@gmail.com', '123456@');
        const cashierToken = await login('Cashier', 'cashier01', '123456');
        const doctorToken = await login('Doctor', 'namnhhe173376@fpt.edu.vn', '123456@');
        console.log("-> Đăng nhập thành công!");

        // 2. Lấy danh sách ca khám (Slot)
        console.log("\n2. Bệnh nhân lấy danh sách ca khám hôm nay...");
        const date = new Date().toISOString().split('T')[0];
        const slotsRes = await req(`/api/appointments/available-slots?date=${date}`, 'GET', patientToken);
        const slotId = slotsRes.data[0].slotId;
        console.log(`-> Lấy thành công Slot ID: ${slotId}`);

        // 3. Bệnh nhân Đặt khám
        console.log("\n3. Bệnh nhân tiến hành đặt khám...");
        const bookPayload = {
            slotId: slotId,
            date: date,
            reason: "Khám tổng quát, đau dạ dày"
        };
        const bookRes = await req('/api/appointments/my/book', 'POST', patientToken, bookPayload);
        const appointmentId = bookRes.data.appointmentId;
        console.log(`-> Đặt khám thành công! Mã C.Hẹn (AppointmentId): ${appointmentId}`);

        // 4. Lễ tân Check-in
        console.log("\n4. Bệnh nhân đến viện, Lễ tân thực hiện Check-in...");
        await req(`/api/appointments/${appointmentId}/checkin`, 'PATCH', recepToken);
        console.log("-> Check-in hoàn tất. Bệnh nhân chuyển sang trạng thái CHỜ KHÁM (WAITING).");

        // 5. Bác sĩ gọi bệnh nhân
        console.log("\n5. Bác sĩ lâm sàng gọi bệnh nhân tiếp theo trong hàng đợi...");
        const callRes = await req('/api/appointments/my-patients/call-next', 'POST', doctorToken);
        console.log(`-> Đã gọi bệnh nhân: ${callRes.data.patientName}. Trạng thái: IN_PROGRESS.`);

        // 6. Bác sĩ Hoàn tất khám
        console.log("\n6. Bác sĩ kết luận lâm sàng và kết thúc khám...");
        const completePayload = {
            diagnosis: "Viêm loét dạ dày - tá tràng",
            notes: "Cần ăn uống đúng giờ, tránh đồ cay nóng",
            services: [] 
        };
        await req(`/api/appointments/${appointmentId}/complete`, 'PATCH', doctorToken, completePayload);
        console.log("-> Khám hoàn tất. Trạng thái bệnh án: COMPLETED.");

        console.log("\n=== ✅ TEST THÀNH CÔNG: QUY TRÌNH HOÀT ĐỘNG HOÀN HẢO ===");
    } catch (e) { 
        console.error("❌ LỖI TRONG QUÁ TRÌNH TEST:\n", e.message || e); 
    }
};
run();
