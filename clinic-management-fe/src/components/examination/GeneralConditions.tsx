'use client';

import { Activity } from 'lucide-react';

interface GeneralConditionsProps {
  conditions: string[];
}

export function GeneralConditions({ conditions }: GeneralConditionsProps) {
  if (conditions.length === 0) {
    return null;
  }

  const conditionColors = {
    'Tỉnh táo': 'bg-yellow-100 text-yellow-800 border-yellow-300',
    'Bình thường': 'bg-green-100 text-green-800 border-green-300',
    'Mệt mệt': 'bg-red-100 text-red-800 border-red-300',
    'Đau nhức': 'bg-purple-100 text-purple-800 border-purple-300',
  };

  const defaultColor = 'bg-blue-100 text-blue-800 border-blue-300';

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 mb-6">
      <h3 className="text-base font-bold text-gray-800 mb-4 flex items-center gap-2">
        <Activity className="w-5 h-5 text-[#8B5CF6]" />
        Tình trạng chung
      </h3>

      <div className="flex flex-wrap gap-2">
        {conditions.map((condition, index) => (
          <span
            key={index}
            className={`inline-flex items-center gap-1.5 px-4 py-2 rounded-full text-sm font-semibold transition-transform hover:scale-105 ${
              conditionColors[condition as keyof typeof conditionColors] || defaultColor
            }`}
          >
            {condition}
          </span>
        ))}
      </div>
    </div>
  );
}
