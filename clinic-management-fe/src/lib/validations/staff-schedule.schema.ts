import { z } from 'zod';

export const staffScheduleSchema = z.object({
    userId: z.string().min(1, { message: "Vui lòng chọn nhân viên" }),
    deptId: z.string().min(1, { message: "Vui lòng chọn phòng ban" }),
    date: z.string().min(1, { message: "Vui lòng chọn ngày" }),
    startTime: z.string().min(1, { message: "Vui lòng chọn thời gian bắt đầu" }),
    endTime: z.string().min(1, { message: "Vui lòng chọn thời gian kết thúc" }),
    status: z.enum(['PRESENT', 'ABSENT', 'SCHEDULED']),
    note: z.string().optional(),
});

export type StaffScheduleInput = z.infer<typeof staffScheduleSchema>;
