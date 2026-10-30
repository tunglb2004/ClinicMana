'use client';

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';

interface SidebarContextType {
    isSidebarOpen: boolean;
    toggleSidebar: () => void;
}

const SidebarContext = createContext<SidebarContextType | undefined>(undefined);

export function SidebarProvider({ children }: { children: ReactNode }) {
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);

    // Khi vào dashboard (bất kỳ role nào), luôn mở left menu sẵn
    useEffect(() => {
        setIsSidebarOpen(true);
    }, []);

    const toggleSidebar = () => {
        setIsSidebarOpen(prev => !prev);
    };

    return (
        <SidebarContext.Provider value={{ isSidebarOpen, toggleSidebar }}>
            {children}
        </SidebarContext.Provider>
    );
}

export function useSidebar() {
    const context = useContext(SidebarContext);
    if (context === undefined) {
        throw new Error('useSidebar must be used within a SidebarProvider');
    }
    return context;
}
