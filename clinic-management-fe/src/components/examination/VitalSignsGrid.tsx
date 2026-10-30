'use client';

import { Heart, Thermometer, Activity, Scale, Ruler, type LucideIcon } from 'lucide-react';

interface VitalsDisplayProps {
  label: string;
  value: string | number;
  unit?: string;
  icon: LucideIcon;
}

export function VitalsDisplay({ label, value, unit, icon: Icon }: VitalsDisplayProps) {
  return (
    <div className="flex items-center gap-3 bg-white rounded-lg p-4 border border-gray-200">
      <div className="flex-shrink-0 bg-gradient-to-br from-[#8B5CF6] to-[#7C3AED] rounded-full p-3">
        <Icon className="w-6 h-6 text-white" />
      </div>
      <div className="flex-1">
        <p className="text-xs text-gray-500 uppercase tracking-wide font-semibold mb-1">{label}</p>
        <p className="text-lg font-bold text-gray-900">
          {value}
          {unit && <span className="text-sm text-gray-600 font-normal ml-1">{unit}</span>}
        </p>
      </div>
    </div>
  );
}

interface VitalSignsGridProps {
  vitals: {
    bloodPressure?: string;
    heartRate?: number;
    temperature?: number;
    weight?: number;
    height?: number;
  };
}

export function VitalSignsGrid({ vitals }: VitalSignsGridProps) {
  const vitalCards = [
    {
      label: 'Huyết áp',
      value: vitals.bloodPressure || '--',
      unit: 'mmHg',
      icon: Heart,
    },
    {
      label: 'Nhịp tim',
      value: vitals.heartRate || '--',
      unit: 'lần/phút',
      icon: Activity,
    },
    {
      label: 'Nhiệt độ',
      value: vitals.temperature || '--',
      unit: '°C',
      icon: Thermometer,
    },
    {
      label: 'Cân nặng',
      value: vitals.weight || '--',
      unit: 'kg',
      icon: Scale,
    },
    {
      label: 'Chiều cao',
      value: vitals.height || '--',
      unit: 'cm',
      icon: Ruler,
    },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
      {vitalCards.map((card, index) => (
        <VitalsDisplay
          key={index}
          label={card.label}
          value={card.value}
          unit={card.unit}
          icon={card.icon}
        />
      ))}
    </div>
  );
}