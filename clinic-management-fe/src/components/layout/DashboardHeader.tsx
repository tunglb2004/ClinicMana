interface DashboardHeaderProps {
  title: string;
  subtitle: string;
}

export function DashboardHeader({ title, subtitle }: DashboardHeaderProps) {
  return (
    <div className="border-b border-slate-200/80 bg-white/75 backdrop-blur">
      <div className="mx-auto max-w-7xl px-6 py-6">
        <h1 className="text-2xl font-bold text-slate-950">{title}</h1>
        <p className="mt-1 text-sm text-slate-500">{subtitle}</p>
      </div>
    </div>
  );
}
