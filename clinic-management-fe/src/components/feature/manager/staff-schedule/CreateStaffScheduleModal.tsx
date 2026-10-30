'use client';

import React, { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'react-hot-toast';
import { X, Calendar, Clock, User, Building2, AlertCircle, Save } from 'lucide-react';
import { staffScheduleSchema, StaffScheduleInput } from '@/lib/validations/staff-schedule.schema';
import { staffScheduleService } from '@/services/staff-schedule.service';
import { managementService, Department, Staff } from '@/services/management.service';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

interface CreateStaffScheduleModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}

const CreateStaffScheduleModal: React.FC<CreateStaffScheduleModalProps> = ({ isOpen, onClose, onSuccess }) => {
    const [departments, setDepartments] = useState<Department[]>([]);
    const [staffList, setStaffList] = useState<Staff[]>([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isLoadingData, setIsLoadingData] = useState(false);

    const {
        register,
        handleSubmit,
        reset,
        formState: { errors },
    } = useForm<StaffScheduleInput>({
        resolver: zodResolver(staffScheduleSchema),
        defaultValues: {
            status: 'SCHEDULED',
            date: new Date().toISOString().split('T')[0],
            startTime: '08:00',
            endTime: '17:00'
        }
    });

    useEffect(() => {
        if (isOpen) {
            const fetchData = async () => {
                setIsLoadingData(true);
                try {
                    const [depts, staff] = await Promise.all([
                        managementService.getDepartments(),
                        managementService.getStaff()
                    ]);
                    setDepartments(depts);
                    setStaffList(staff);
                } catch (error) {
                    console.error('Failed to fetch modal data:', error);
                    toast.error('Không thể tải danh sách nhân viên hoặc phòng ban');
                } finally {
                    setIsLoadingData(false);
                }
            };
            fetchData();
        }
    }, [isOpen]);

    const onSubmit = async (data: StaffScheduleInput) => {
        setIsSubmitting(true);
        try {
            await staffScheduleService.createSchedule({
                ...data,
                userId: Number(data.userId),
                deptId: Number(data.deptId)
            });
            toast.success('Thêm lịch làm việc thành công');
            onSuccess();
            onClose();
            reset();
        } catch (error: any) {
            console.error('Failed to create schedule:', error);
            toast.error(error.response?.data?.message || 'Đã có lỗi xảy ra khi thêm lịch làm việc');
        } finally {
            setIsSubmitting(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto">
            {/* Overlay */}
            <div
                className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm transition-opacity animate-in fade-in duration-300"
                onClick={onClose}
            ></div>

            {/* Modal Content */}
            <div className="relative w-full max-w-2xl bg-white dark:bg-slate-900 rounded-[2rem] shadow-2xl border border-border/50 overflow-hidden animate-in zoom-in-95 slide-in-from-bottom-4 duration-300">
                <div className="absolute top-0 right-0 p-6 z-10">
                    <button
                        onClick={onClose}
                        className="h-10 w-10 flex items-center justify-center rounded-full bg-muted/30 hover:bg-muted/50 text-muted-foreground transition-all"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                <div className="p-8 sm:p-10">
                    <div className="mb-8">
                        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-bold uppercase tracking-wider border border-primary/20 mb-4">
                            <Calendar className="h-3.5 w-3.5" />
                            Gán lịch làm việc
                        </div>
                        <h2 className="text-3xl font-black text-foreground">Thêm lịch mới</h2>
                        <p className="text-muted-foreground mt-2 font-medium">Thiết lập thời gian trực cho nhân viên hệ thống.</p>
                    </div>

                    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            {/* Staff Selection */}
                            <div className="space-y-2">
                                <Label htmlFor="userId" className="text-sm font-bold ml-1 flex items-center gap-2">
                                    <User className="h-4 w-4 text-primary" /> Nhân viên
                                </Label>
                                <div className="relative">
                                    <select
                                        id="userId"
                                        className={`w-full h-12 pl-4 pr-10 rounded-xl border ${errors.userId ? 'border-destructive' : 'border-border/50'} bg-background focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-all appearance-none cursor-pointer text-sm font-medium`}
                                        {...register('userId')}
                                        disabled={isLoadingData || isSubmitting}
                                    >
                                        <option value="">Chọn nhân viên...</option>
                                        {staffList.map((staff) => (
                                            <option key={staff.userId} value={staff.userId.toString()}>
                                                {staff.fullName} ({staff.username})
                                            </option>
                                        ))}
                                    </select>
                                    {errors.userId && (
                                        <p className="mt-1.5 text-xs font-bold text-destructive flex items-center gap-1 ml-1 leading-none">
                                            <AlertCircle className="h-3 w-3" /> {errors.userId.message}
                                        </p>
                                    )}
                                </div>
                            </div>

                            {/* Department Selection */}
                            <div className="space-y-2">
                                <Label htmlFor="deptId" className="text-sm font-bold ml-1 flex items-center gap-2">
                                    <Building2 className="h-4 w-4 text-primary" /> Phòng ban
                                </Label>
                                <div className="relative">
                                    <select
                                        id="deptId"
                                        className={`w-full h-12 pl-4 pr-10 rounded-xl border ${errors.deptId ? 'border-destructive' : 'border-border/50'} bg-background focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-all appearance-none cursor-pointer text-sm font-medium`}
                                        {...register('deptId')}
                                        disabled={isLoadingData || isSubmitting}
                                    >
                                        <option value="">Chọn phòng ban...</option>
                                        {departments.map((dept) => (
                                            <option key={dept.deptId} value={dept.deptId.toString()}>
                                                {dept.deptName}
                                            </option>
                                        ))}
                                    </select>
                                    {errors.deptId && (
                                        <p className="mt-1.5 text-xs font-bold text-destructive flex items-center gap-1 ml-1 leading-none">
                                            <AlertCircle className="h-3 w-3" /> {errors.deptId.message}
                                        </p>
                                    )}
                                </div>
                            </div>

                            {/* Date */}
                            <div className="space-y-2">
                                <Label htmlFor="date" className="text-sm font-bold ml-1 flex items-center gap-2">
                                    <Calendar className="h-4 w-4 text-primary" /> Ngày làm việc
                                </Label>
                                <Input
                                    id="date"
                                    type="date"
                                    className={`h-12 rounded-xl bg-background ${errors.date ? 'border-destructive' : 'border-border/50'}`}
                                    {...register('date')}
                                    disabled={isSubmitting}
                                />
                                {errors.date && (
                                    <p className="mt-1.5 text-xs font-bold text-destructive flex items-center gap-1 ml-1 leading-none">
                                        <AlertCircle className="h-3 w-3" /> {errors.date.message}
                                    </p>
                                )}
                            </div>

                            {/* Status */}
                            <div className="space-y-2">
                                <Label htmlFor="status" className="text-sm font-bold ml-1 flex items-center gap-2">
                                    Trạng thái ban đầu
                                </Label>
                                <select
                                    id="status"
                                    className="w-full h-12 px-4 rounded-xl border border-border/50 bg-background focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-all text-sm font-medium"
                                    {...register('status')}
                                    disabled={isSubmitting}
                                >
                                    <option value="SCHEDULED">Đã lên lịch</option>
                                    <option value="PRESENT">Hiện diện (Present)</option>
                                    <option value="ABSENT">Vắng mặt (Absent)</option>
                                </select>
                            </div>

                            {/* Start Time */}
                            <div className="space-y-2">
                                <Label htmlFor="startTime" className="text-sm font-bold ml-1 flex items-center gap-2">
                                    <Clock className="h-4 w-4 text-primary" /> Giờ bắt đầu
                                </Label>
                                <Input
                                    id="startTime"
                                    type="time"
                                    className={`h-12 rounded-xl bg-background ${errors.startTime ? 'border-destructive' : 'border-border/50'}`}
                                    {...register('startTime')}
                                    disabled={isSubmitting}
                                />
                                {errors.startTime && (
                                    <p className="mt-1.5 text-xs font-bold text-destructive flex items-center gap-1 ml-1 leading-none">
                                        <AlertCircle className="h-3 w-3" /> {errors.startTime.message}
                                    </p>
                                )}
                            </div>

                            {/* End Time */}
                            <div className="space-y-2">
                                <Label htmlFor="endTime" className="text-sm font-bold ml-1 flex items-center gap-2">
                                    <Clock className="h-4 w-4 text-primary" /> Giờ kết thúc
                                </Label>
                                <Input
                                    id="endTime"
                                    type="time"
                                    className={`h-12 rounded-xl bg-background ${errors.endTime ? 'border-destructive' : 'border-border/50'}`}
                                    {...register('endTime')}
                                    disabled={isSubmitting}
                                />
                                {errors.endTime && (
                                    <p className="mt-1.5 text-xs font-bold text-destructive flex items-center gap-1 ml-1 leading-none">
                                        <AlertCircle className="h-3 w-3" /> {errors.endTime.message}
                                    </p>
                                )}
                            </div>
                        </div>

                        {/* Note */}
                        <div className="space-y-2">
                            <Label htmlFor="note" className="text-sm font-bold ml-1">Ghi chú (Tùy chọn)</Label>
                            <textarea
                                id="note"
                                rows={3}
                                className="w-full px-4 py-3 rounded-xl border border-border/50 bg-background focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-all text-sm font-medium resize-none shadow-inner"
                                placeholder="Nhập ghi chú hoặc hướng dẫn công việc..."
                                {...register('note')}
                                disabled={isSubmitting}
                            ></textarea>
                        </div>

                        {/* Actions */}
                        <div className="pt-6 flex gap-4">
                            <Button
                                type="button"
                                variant="outline"
                                onClick={onClose}
                                disabled={isSubmitting}
                                className="flex-1 h-14 rounded-2xl border-border/50 font-bold text-muted-foreground hover:bg-muted/30 transition-all active:scale-95"
                            >
                                Hủy bỏ
                            </Button>
                            <Button
                                type="submit"
                                disabled={isSubmitting}
                                className="flex-[2] h-14 rounded-2xl bg-gradient-to-r from-primary to-primary-dark hover:shadow-lg hover:shadow-primary/20 transition-all font-bold active:scale-95 flex items-center justify-center gap-2 group"
                            >
                                {isSubmitting ? (
                                    <div className="h-6 w-6 border-3 border-white/20 border-t-white rounded-full animate-spin"></div>
                                ) : (
                                    <>
                                        <Save className="h-5 w-5 transition-transform group-hover:scale-110" />
                                        Thực hiện lưu lịch
                                    </>
                                )}
                            </Button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default CreateStaffScheduleModal;
