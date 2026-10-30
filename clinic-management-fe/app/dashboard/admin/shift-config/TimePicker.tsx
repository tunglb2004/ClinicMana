'use client';

import { useState, useRef, useEffect, useId } from 'react';
import { createPortal } from 'react-dom';
import { Clock } from 'lucide-react';

const HOURS = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'));
const MINUTES = Array.from({ length: 60 }, (_, i) => String(i).padStart(2, '0'));

interface TimePickerProps {
  value: string; // "HH:mm"
  onChange: (value: string) => void;
  placeholder?: string;
  className?: string;
  id?: string;
}

export function TimePicker({ value, onChange, placeholder = '00:00', className = '', id }: TimePickerProps) {
  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState({ top: 0, left: 0 });
  const generatedId = useId();
  const listboxId = `${id ?? generatedId}-listbox`;
  const triggerRef = useRef<HTMLDivElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  const isValueEmpty = value == null || String(value).trim() === '';
  const [h, m] = (value || '').split(':');
  const hour = h?.trim() && /^\d{1,2}$/.test(h) ? String(parseInt(h, 10)).padStart(2, '0') : '00';
  const minute = m?.trim() && /^\d{1,2}$/.test(m) ? String(parseInt(m, 10)).padStart(2, '0') : '00';
  const displayValue = isValueEmpty ? '' : `${hour}:${minute}`;

  useEffect(() => {
    if (open && triggerRef.current && typeof document !== 'undefined') {
      const rect = triggerRef.current.getBoundingClientRect();
      const panelHeight = 240;
      const spaceBelow = window.innerHeight - rect.bottom;
      const openDown = spaceBelow >= panelHeight || spaceBelow >= rect.top;
      setPosition({
        left: rect.left,
        top: openDown ? rect.bottom + 4 : rect.top - panelHeight - 4,
      });
    }
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const handleClickOutside = (e: MouseEvent) => {
      const target = e.target as Node;
      if (
        triggerRef.current?.contains(target) ||
        panelRef.current?.contains(target)
      ) {
        return;
      }
      setOpen(false);
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [open]);

  const handleSelectHour = (hr: string) => {
    onChange(`${hr}:${minute}`);
  };

  const handleSelectMinute = (min: string) => {
    onChange(`${hour}:${min}`);
  };

  const dropdown = open && typeof document !== 'undefined' && (
    createPortal(
      <div
        className="fixed inset-0 z-[9998] overflow-visible pointer-events-none"
        style={{ width: 0, height: 0 }}
        aria-hidden
      >
        <div
          id={listboxId}
          ref={panelRef}
          role="listbox"
          className="fixed z-[9999] min-w-[200px] bg-white border border-slate-200 rounded-lg shadow-xl overflow-hidden pointer-events-auto"
          style={{
            top: position.top,
            left: position.left,
          }}
        >
        <div className="flex gap-2 p-2 border-b border-slate-100 bg-slate-50">
          <span className="flex-1 py-1.5 rounded bg-blue-600 text-white text-sm font-medium text-center">
            {hour}
          </span>
          <span className="flex-1 py-1.5 rounded bg-blue-600 text-white text-sm font-medium text-center">
            {minute}
          </span>
        </div>
        <div className="flex max-h-48">
          <ul className="flex-1 overflow-y-auto py-1 text-sm text-slate-800" aria-label="Giờ">
            {HOURS.map((hr) => (
              <li key={hr}>
                <button
                  type="button"
                  onClick={() => handleSelectHour(hr)}
                  className={`w-full py-1.5 text-center hover:bg-slate-100 ${hr === hour ? 'bg-blue-50 text-blue-700 font-medium' : ''}`}
                >
                  {hr}
                </button>
              </li>
            ))}
          </ul>
          <ul
            className="flex-1 overflow-y-auto py-1 text-sm text-slate-800 border-l border-slate-100"
            aria-label="Phút"
          >
            {MINUTES.map((min) => (
              <li key={min}>
                <button
                  type="button"
                  onClick={() => handleSelectMinute(min)}
                  className={`w-full py-1.5 text-center hover:bg-slate-100 ${min === minute ? 'bg-blue-50 text-blue-700 font-medium' : ''}`}
                >
                  {min}
                </button>
              </li>
            ))}
          </ul>
        </div>
        </div>
      </div>,
      document.body
    )
  );

  return (
    <div className={className}>
      <div
        id={id}
        ref={triggerRef}
        role="combobox"
        aria-controls={listboxId}
        aria-expanded={open}
        aria-haspopup="listbox"
        tabIndex={0}
        onClick={() => setOpen((o) => !o)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            setOpen((o) => !o);
          }
        }}
        className="w-full border border-slate-200 rounded p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500 text-slate-800 bg-white flex items-center justify-between gap-2 cursor-pointer"
      >
        <span className={open ? 'bg-blue-100' : ''}>{displayValue || placeholder}</span>
        <Clock className="w-4 h-4 text-slate-500 shrink-0" aria-hidden />
      </div>
      {dropdown}
    </div>
  );
}
