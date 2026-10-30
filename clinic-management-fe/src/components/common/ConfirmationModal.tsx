import React from 'react';

interface ConfirmationModalProps {
    isOpen: boolean;
    onClose: () => void;
    onConfirm: () => void;
    title: string;
    message: string;
    confirmText?: string;
    cancelText?: string;
    variant?: 'pink' | 'red' | 'black' | 'blue';
}

export function ConfirmationModal({
    isOpen,
    onClose,
    onConfirm,
    title,
    message,
    confirmText = 'XÁC NHẬN',
    cancelText = 'HỦY',
    variant = 'pink'
}: ConfirmationModalProps) {
    if (!isOpen) return null;

    const getColors = () => {
        switch (variant) {
            case 'red':
                return { text: 'text-red-600', bg: 'bg-red-600', hover: 'hover:bg-red-700' };
            case 'black':
                return { text: 'text-black', bg: 'bg-black', hover: 'hover:bg-gray-800' };
            case 'blue':
                return { text: 'text-blue-600', bg: 'bg-blue-600', hover: 'hover:bg-blue-700' };
            default: // pink
                return { text: 'text-pink-500', bg: 'bg-pink-500', hover: 'hover:bg-pink-600' };
        }
    };

    const colors = getColors();

    return (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
            <div className="bg-white border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,0.5)] w-full max-w-sm p-6 text-center animate-in fade-in zoom-in duration-200 rounded-lg overflow-hidden">
                <div className="flex justify-center mb-4">
                    <div className="w-12 h-12 bg-black text-white rounded-none flex items-center justify-center">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-8 h-8">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
                        </svg>
                    </div>
                </div>

                <h3 className={`text-xl font-black uppercase mb-2 ${colors.text}`}>{title}</h3>
                <p className="text-gray-600 mb-6 font-medium">{message}</p>

                <div className="flex gap-3 justify-center">
                    <button
                        onClick={onClose}
                        className="flex-1 px-4 py-2 border-2 border-black font-bold hover:bg-gray-100 transition-colors uppercase"
                    >
                        {cancelText}
                    </button>
                    <button
                        onClick={onConfirm}
                        className={`flex-1 px-4 py-2 ${colors.bg} text-white border-2 border-black font-bold ${colors.hover} transition-colors uppercase`}
                    >
                        {confirmText}
                    </button>
                </div>
            </div>
        </div>
    );
}
