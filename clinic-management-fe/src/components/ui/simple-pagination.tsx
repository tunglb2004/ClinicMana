'use client';

import React from 'react';
import { Button } from '@/components/ui/button';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface PaginationProps {
    currentPage: number;
    totalPages: number;
    onPageChange: (page: number) => void;
    isLoading: boolean;
}

const SimplePagination: React.FC<PaginationProps> = ({
    currentPage,
    totalPages,
    onPageChange,
    isLoading
}) => {
    if (totalPages <= 1) return null;

    return (
        <div className="flex items-center justify-center gap-4 mt-6">
            <Button
                variant="outline"
                size="sm"
                onClick={() => onPageChange(currentPage - 1)}
                disabled={currentPage === 0 || isLoading}
            >
                <ChevronLeft className="h-4 w-4 mr-1" />
                Trang trước
            </Button>

            <span className="text-sm font-medium">
                Trang {currentPage + 1} / {totalPages}
            </span>

            <Button
                variant="outline"
                size="sm"
                onClick={() => onPageChange(currentPage + 1)}
                disabled={currentPage === totalPages - 1 || isLoading}
            >
                Trang sau
                <ChevronRight className="h-4 w-4 ml-1" />
            </Button>
        </div>
    );
};

export default SimplePagination;
