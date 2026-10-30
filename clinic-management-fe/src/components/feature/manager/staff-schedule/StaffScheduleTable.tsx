'use client';

import React from 'react';
import { StaffSchedule } from '@/types/staff-schedule.types';
import { Badge } from '@/components/ui/badge';
import { User, Calendar, Clock, FileText, Building2 } from 'lucide-react';

interface StaffScheduleTableProps {
    data: StaffSchedule[];
    isLoading: boolean;
}

const StaffScheduleTable: React.FC<StaffScheduleTableProps> = ({ data, isLoading }) => {
    if (isLoading) {
        return (
            <div className="w-full h-80 flex flex-col items-center justify-center space-y-4">
                <div className="relative">
                    <div className="h-16 w-16 rounded-full border-4 border-primary/20 border-t-primary animate-spin"></div>
                    <div className="absolute inset-0 flex items-center justify-center">
                        <div className="h-2 w-2 bg-primary rounded-full"></div>
                    </div>
                </div>
                <p className="text-muted-foreground font-medium animate-pulse">Đang tải dữ liệu...</p>
            </div>
        );
    }

    if (data.length === 0) {
        return (
            <div className="w-full py-20 text-center border rounded-2xl bg-card/30 backdrop-blur-sm border-dashed">
                <div className="inline-flex h-16 w-16 items-center justify-center rounded-full bg-muted/50 mb-4">
                    <Calendar className="h-8 w-8 text-muted-foreground/50" />
                </div>
                <h3 className="text-lg font-semibold text-foreground">Không tìm thấy lịch làm việc</h3>
                <p className="text-muted-foreground mt-1">Vui lòng thay đổi bộ lọc hoặc kiểm tra lại sau.</p>
            </div>
        );
    }

    return (
        <div className="w-full overflow-hidden rounded-2xl border border-border/50 bg-card/30 backdrop-blur-md shadow-sm">
            <div className="overflow-x-auto">
                <table className="w-full text-sm text-left">
                    <thead>
                        <tr className="bg-muted/30 text-muted-foreground font-semibold border-b border-border/50">
                            <th className="px-6 py-4 flex items-center gap-2">
                                <User className="h-4 w-4" /> Nhân viên
                            </th>
                            <th className="px-6 py-4">
                                <span className="flex items-center gap-2"><Building2 className="h-4 w-4" /> Phòng ban</span>
                            </th>
                            <th className="px-6 py-4">
                                <span className="flex items-center gap-2"><Calendar className="h-4 w-4" /> Ngày</span>
                            </th>
                            <th className="px-6 py-4">
                                <span className="flex items-center gap-2"><Clock className="h-4 w-4" /> Ca làm</span>
                            </th>
                            <th className="px-6 py-4">Trạng thái</th>
                            <th className="px-6 py-4">
                                <span className="flex items-center gap-2"><FileText className="h-4 w-4" /> Ghi chú</span>
                            </th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-border/40">
                        {data.map((item, index) => (
                            <tr
                                key={item.id}
                                className="group hover:bg-primary/5 transition-all duration-300"
                                style={{ animationDelay: `${index * 50}ms` }}
                            >
                                <td className="px-6 py-5">
                                    <div className="flex items-center gap-3">
                                        <div className="h-9 w-9 rounded-full bg-gradient-to-br from-primary/20 to-secondary/20 flex items-center justify-center text-primary font-bold shadow-inner">
                                            {item.staffName.charAt(0)}
                                        </div>
                                        <span className="font-semibold text-foreground group-hover:text-primary transition-colors">
                                            {item.staffName}
                                        </span>
                                    </div>
                                </td>
                                <td className="px-6 py-5">
                                    <span className="bg-muted/50 px-2 py-1 rounded-md text-xs font-medium border border-border/50">
                                        {item.departmentName}
                                    </span>
                                </td>
                                <td className="px-6 py-5 font-medium">
                                    {new Date(item.date).toLocaleDateString('vi-VN', {
                                        weekday: 'long',
                                        year: 'numeric',
                                        month: 'long',
                                        day: 'numeric'
                                    })}
                                </td>
                                <td className="px-6 py-5">
                                    <div className="flex items-center gap-1.5 py-1 px-2 rounded-lg bg-primary/5 w-fit border border-primary/10">
                                        <span className="font-bold text-primary">{item.startTime}</span>
                                        <span className="text-muted-foreground/50">→</span>
                                        <span className="font-bold text-primary">{item.endTime}</span>
                                    </div>
                                </td>
                                <td className="px-6 py-5">
                                    <StatusBadge status={item.status} />
                                </td>
                                <td className="px-6 py-5">
                                    <p className="text-muted-foreground italic text-xs max-w-[200px] line-clamp-2">
                                        {item.note || 'Không có ghi chú'}
                                    </p>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

const StatusBadge = ({ status }: { status: StaffSchedule['status'] }) => {
    const variants: Record<StaffSchedule['status'], { variant: any; label: string; icon: any }> = {
        PRESENT: { variant: 'success', label: 'Hiện diện', icon: <CheckCircle2 className="h-3 w-3" /> },
        ABSENT: { variant: 'destructive', label: 'Vắng mặt', icon: <AlertCircle className="h-3 w-3" /> },
        SCHEDULED: { variant: 'info', label: 'Đã lên lịch', icon: <Clock className="h-3 w-3" /> },
    };

    const { variant, label, icon } = variants[status];

    return (
        <Badge variant={variant} className="gap-1.5 px-3 py-1 shadow-sm">
            {icon}
            {label}
        </Badge>
    );
};

import { CheckCircle2, AlertCircle } from 'lucide-react';

export default StaffScheduleTable;
