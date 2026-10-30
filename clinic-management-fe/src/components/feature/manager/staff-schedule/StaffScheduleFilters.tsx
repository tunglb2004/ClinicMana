import React from 'react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Search, Calendar, Filter, RotateCcw } from 'lucide-react';

interface StaffScheduleFiltersProps {
    onFilterChange: (filters: { date?: string; deptId?: string }) => void;
    isLoading: boolean;
}

const StaffScheduleFilters: React.FC<StaffScheduleFiltersProps> = ({ onFilterChange, isLoading }) => {
    const [date, setDate] = React.useState('');
    const [deptId, setDeptId] = React.useState('');

    const handleApplyFilters = () => {
        onFilterChange({ date, deptId });
    };

    const handleReset = () => {
        setDate('');
        setDeptId('');
        onFilterChange({ date: '', deptId: '' });
    };

    return (
        <div className="group relative mb-8">
            {/* Background Glow */}
            <div className="absolute -inset-1 bg-gradient-to-r from-primary/20 to-secondary/20 rounded-2xl blur opacity-25 group-hover:opacity-50 transition duration-1000 group-hover:duration-200"></div>

            <div className="relative flex flex-wrap gap-6 items-end bg-card/80 backdrop-blur-sm p-6 rounded-2xl border border-border/50 shadow-xl">
                <div className="flex-1 min-w-[240px] space-y-2">
                    <label className="text-xs font-bold uppercase tracking-widest text-muted-foreground ml-1">
                        Ngày làm việc
                    </label>
                    <div className="relative group/input">
                        <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 h-4.5 w-4.5 text-primary group-focus-within/input:text-primary transition-colors" />
                        <Input
                            type="date"
                            className="pl-11 h-12 bg-background/50 border-border/50 focus:border-primary/50 focus:ring-primary/20 rounded-xl transition-all"
                            value={date}
                            onChange={(e) => setDate(e.target.value)}
                            disabled={isLoading}
                        />
                    </div>
                </div>

                <div className="flex-1 min-w-[240px] space-y-2">
                    <label className="text-xs font-bold uppercase tracking-widest text-muted-foreground ml-1">
                        Phòng ban chuyên môn
                    </label>
                    <div className="relative group/input">
                        <Filter className="absolute left-3 top-1/2 -translate-y-1/2 h-4.5 w-4.5 text-primary group-focus-within/input:text-primary transition-colors" />
                        <select
                            className="w-full h-12 pl-11 pr-4 rounded-xl border border-border/50 bg-background/50 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/50 transition-all appearance-none cursor-pointer"
                            value={deptId}
                            onChange={(e) => setDeptId(e.target.value)}
                            disabled={isLoading}
                        >
                            <option value="">Tất cả phòng ban</option>
                            <option value="1">Khoa Nội</option>
                            <option value="2">Khoa Ngoại</option>
                            <option value="3">Khoa Nhi</option>
                            <option value="4">Khoa Sản</option>
                        </select>
                        <div className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none">
                            <Search className="h-4 w-4 text-muted-foreground/50" />
                        </div>
                    </div>
                </div>

                <div className="flex gap-3 ml-auto">
                    <Button
                        variant="outline"
                        onClick={handleReset}
                        disabled={isLoading}
                        className="h-12 px-6 rounded-xl border-border/50 hover:bg-muted/50 transition-all font-semibold text-muted-foreground"
                    >
                        <RotateCcw className="mr-2 h-4 w-4" />
                        Làm mới
                    </Button>
                    <Button
                        onClick={handleApplyFilters}
                        disabled={isLoading}
                        className="h-12 px-8 rounded-xl bg-gradient-to-r from-primary to-primary-dark hover:shadow-lg hover:shadow-primary/20 transition-all font-bold active:scale-[0.98]"
                    >
                        <Search className="mr-2 h-4.5 w-4.5" />
                        Truy vấn dữ liệu
                    </Button>
                </div>
            </div>
        </div>
    );
};

export default StaffScheduleFilters;
