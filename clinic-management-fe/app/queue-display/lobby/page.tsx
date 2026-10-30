'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { Activity, ArrowUpRight, Clock3, Loader2, MapPin, MonitorSmartphone, Radio, Stethoscope, UserCheck } from 'lucide-react';
import { LobbyDashboardResponse, RoomOverview, queueDisplayService } from '@/services/queue-display.service';

const POLL_INTERVAL = 12000;

function formatTime(minutes: number) {
  if (minutes <= 0) return 'Sẵn sàng';
  if (minutes < 60) return `${minutes} phút`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m > 0 ? `${h}h ${m}p` : `${h}h`;
}

function formatClock(date: Date) {
  return date.toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

function formatDate(date: Date) {
  return date.toLocaleDateString('vi-VN', {
    weekday: 'long',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

function getRoomState(room: RoomOverview) {
  if (room.currentPatient) return 'busy' as const;
  if (room.currentQueueLength > 0) return 'waiting' as const;
  return 'idle' as const;
}

function RoomCard({ room }: { room: RoomOverview }) {
  const state = getRoomState(room);
  const palette = state === 'busy'
    ? {
        shell: 'border-blue-300/70 bg-gradient-to-br from-blue-700 via-blue-600 to-slate-900 text-white',
        badge: 'bg-white/18 text-blue-50 ring-1 ring-white/20',
        chip: 'bg-white/14 text-white/90',
        panel: 'bg-white/14 border-white/20',
        metric: 'bg-white/12 border-white/15',
        label: 'text-blue-100/80',
      }
    : state === 'waiting'
      ? {
          shell: 'border-blue-200/70 bg-gradient-to-br from-blue-50 via-white to-blue-100 text-slate-900',
          badge: 'bg-blue-600 text-white ring-1 ring-blue-400/40',
          chip: 'bg-white/80 text-blue-700',
          panel: 'bg-white/80 border-blue-100',
          metric: 'bg-white/80 border-blue-100',
          label: 'text-slate-500',
        }
      : {
          shell: 'border-blue-200/70 bg-gradient-to-br from-slate-50 via-blue-50 to-white text-slate-900',
          badge: 'bg-blue-500 text-white ring-1 ring-blue-300/40',
          chip: 'bg-white/80 text-blue-700',
          panel: 'bg-white/80 border-blue-100',
          metric: 'bg-white/80 border-blue-100',
          label: 'text-slate-500',
        };

  const headline = state === 'busy'
    ? 'Đang khám'
    : state === 'waiting'
      ? 'Sắp tới lượt'
      : 'Đang rảnh';

  return (
    <Link
      href={`/queue-display/room/${room.roomId}`}
      className={`group relative overflow-hidden rounded-[28px] border p-5 shadow-lg shadow-slate-900/5 transition duration-300 hover:-translate-y-1 hover:shadow-2xl ${palette.shell}`}
    >
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(255,255,255,0.28),transparent_32%)]" />

      <div className="relative flex h-full flex-col gap-4">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <div className={`inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.22em] ${palette.badge}`}>
              <span className={`h-2.5 w-2.5 rounded-full ${state === 'busy' ? 'bg-white animate-pulse' : state === 'waiting' ? 'bg-blue-300' : 'bg-blue-100'}`} />
              {headline}
            </div>
            <h3 className="mt-3 truncate text-2xl font-black tracking-[-0.03em]">{room.roomName}</h3>
            <p className={`mt-1 text-sm ${state === 'busy' ? 'text-blue-50/80' : 'text-slate-500'}`}>
              {room.departmentName ?? 'Chưa rõ khoa'}
            </p>
          </div>

          <div className={`inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs font-medium ${palette.chip}`}>
            <ArrowUpRight className="h-3.5 w-3.5 transition-transform duration-300 group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
            Mở bảng phòng
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3 text-sm">
          <span className={`inline-flex items-center gap-2 rounded-full px-3 py-1 ${palette.chip}`}>
            <UserCheck className="h-4 w-4" />
            {room.doctorName ?? 'Chưa có lịch trực'}
          </span>
          {room.location && (
            <span className={`inline-flex items-center gap-2 rounded-full px-3 py-1 ${palette.chip}`}>
              <MapPin className="h-4 w-4" />
              {room.location}
            </span>
          )}
        </div>

        <div className={`rounded-2xl border p-4 ${palette.panel}`}>
          <p className={`text-[11px] font-semibold uppercase tracking-[0.24em] ${palette.label}`}>
            {room.currentPatient ? 'Bệnh nhân đang khám' : room.currentQueueLength > 0 ? 'Người tiếp theo đang chờ' : 'Tình trạng phòng'}
          </p>
          {room.currentPatient ? (
            <>
              <p className="mt-2 text-lg font-bold">{room.currentPatient.patientName}</p>
              <p className={`text-sm ${state === 'busy' ? 'text-blue-50/80' : 'text-slate-500'}`}>Mã BN: {room.currentPatient.patientId}</p>
            </>
          ) : room.currentQueueLength > 0 ? (
            <p className="mt-2 text-lg font-bold">Có {room.currentQueueLength} bệnh nhân đang chờ vào phòng</p>
          ) : (
            <p className="mt-2 text-lg font-bold">Phòng sẵn sàng nhận bệnh nhân kế tiếp</p>
          )}
        </div>

        <div className="grid grid-cols-3 gap-3">
          <div className={`rounded-2xl border px-4 py-3 ${palette.metric}`}>
            <p className={`text-[11px] uppercase tracking-[0.24em] ${palette.label}`}>Đang chờ</p>
            <p className="mt-2 text-3xl font-black tracking-[-0.04em]">{room.currentQueueLength}</p>
          </div>
          <div className={`rounded-2xl border px-4 py-3 ${palette.metric}`}>
            <p className={`text-[11px] uppercase tracking-[0.24em] ${palette.label}`}>Ước tính</p>
            <p className="mt-2 text-xl font-black tracking-[-0.04em]">{formatTime(room.estimatedWaitMinutes)}</p>
          </div>
          <div className={`rounded-2xl border px-4 py-3 ${palette.metric}`}>
            <p className={`text-[11px] uppercase tracking-[0.24em] ${palette.label}`}>Chu kỳ</p>
            <p className="mt-2 text-xl font-black tracking-[-0.04em]">{room.baseAvgTime}p</p>
          </div>
        </div>
      </div>
    </Link>
  );
}

function RoomSection({
  title,
  description,
  rooms,
}: {
  title: string;
  description: string;
  rooms: RoomOverview[];
}) {
  if (rooms.length === 0) return null;

  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-2xl font-black tracking-[-0.03em] text-slate-900">{title}</h2>
          <p className="text-sm text-slate-500">{description}</p>
        </div>
        <div className="inline-flex w-fit items-center rounded-full bg-white/80 px-4 py-1.5 text-sm font-semibold text-slate-600 shadow-sm ring-1 ring-slate-200/70">
          {rooms.length} phòng
        </div>
      </div>

      <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
        {rooms.map((room) => (
          <RoomCard key={room.roomId} room={room} />
        ))}
      </div>
    </section>
  );
}

export default function LobbyDashboardPage() {
  const [data, setData] = useState<LobbyDashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [now, setNow] = useState(() => new Date());

  const fetchData = useCallback(async () => {
    try {
      const res = await queueDisplayService.getLobbyDashboard();
      setData(res);
      setError(null);
      setLastUpdated(new Date());
    } catch {
      setError('Không thể tải dữ liệu bảng đợi phòng khám');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, POLL_INTERVAL);
    return () => clearInterval(interval);
  }, [fetchData]);

  useEffect(() => {
    const interval = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(interval);
  }, []);

  const rooms = data?.rooms ?? [];
  const busyRooms = rooms.filter((room) => getRoomState(room) === 'busy');
  const waitingRooms = rooms.filter((room) => getRoomState(room) === 'waiting');
  const idleRooms = rooms.filter((room) => getRoomState(room) === 'idle');

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[linear-gradient(135deg,#f4f8ff_0%,#eef4ff_48%,#ffffff_100%)]">
        <div className="flex flex-col items-center gap-4 rounded-[28px] border border-white/60 bg-white/80 px-10 py-12 shadow-xl shadow-slate-900/5 backdrop-blur">
          <Loader2 className="h-10 w-10 animate-spin text-blue-600" />
          <p className="text-base font-medium text-slate-600">Đang tải bảng đợi phòng khám...</p>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[linear-gradient(135deg,#f4f8ff_0%,#eef4ff_48%,#ffffff_100%)]">
        <div className="rounded-[28px] border border-rose-200 bg-white px-10 py-12 text-center shadow-xl shadow-rose-100/60">
          <p className="text-xl font-bold text-rose-600">{error ?? 'Không tải được dữ liệu'}</p>
          <button
            onClick={fetchData}
            className="mt-5 rounded-full bg-slate-900 px-6 py-3 text-sm font-semibold text-white transition hover:bg-slate-700"
          >
            Tải lại
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[linear-gradient(135deg,#f4f8ff_0%,#eef4ff_42%,#ffffff_100%)] text-slate-900">
      <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <section className="relative overflow-hidden rounded-[36px] border border-white/60 bg-[linear-gradient(140deg,#0f172a_0%,#1d4ed8_42%,#1e3a8a_100%)] px-6 py-7 text-white shadow-[0_30px_80px_rgba(15,23,42,0.16)] sm:px-8 sm:py-9">
          <div className="pointer-events-none absolute inset-y-0 right-0 w-[42%] bg-[radial-gradient(circle_at_top_right,rgba(255,255,255,0.26),transparent_40%)]" />
          <div className="pointer-events-none absolute -left-10 bottom-0 h-44 w-44 rounded-full bg-white/10 blur-2xl" />

          <div className="relative grid gap-8 xl:grid-cols-[1.45fr_0.95fr] xl:items-end">
            <div>
              <div className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-2 text-xs font-semibold uppercase tracking-[0.24em] text-blue-100 ring-1 ring-white/10">
                <Radio className="h-4 w-4 animate-pulse" />
                Live Queue Display
              </div>
              <h1 className="mt-5 max-w-3xl text-4xl font-black tracking-[-0.05em] sm:text-5xl xl:text-6xl">
                Bảng đợi phòng khám
              </h1>
              <p className="mt-4 max-w-2xl text-sm leading-6 text-blue-50/78 sm:text-base">
                Theo dõi trạng thái từng phòng theo thời gian thực: phòng nào đang khám, phòng nào có người chờ, và phòng nào sẵn sàng gọi bệnh nhân tiếp theo.
              </p>

              <div className="mt-6 flex flex-wrap items-center gap-3 text-sm text-blue-50/80">
                <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-2 ring-1 ring-white/10">
                  <Clock3 className="h-4 w-4" />
                  {formatDate(now)}
                </span>
                <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-2 ring-1 ring-white/10">
                  <MonitorSmartphone className="h-4 w-4" />
                  Cập nhật mỗi {POLL_INTERVAL / 1000} giây
                </span>
                {lastUpdated && (
                  <span className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-2 ring-1 ring-white/10">
                    <Activity className="h-4 w-4" />
                    Đồng bộ lúc {formatClock(lastUpdated)}
                  </span>
                )}
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-2">
              <div className="rounded-[28px] border border-white/15 bg-white/10 p-5 backdrop-blur-sm">
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-blue-100/80">Bây giờ</p>
                <p className="mt-3 text-4xl font-black tracking-[-0.05em]">{formatClock(now)}</p>
                <p className="mt-2 text-sm text-blue-50/72">Giờ địa phương phòng khám</p>
              </div>
              <div className="rounded-[28px] border border-white/15 bg-white/10 p-5 backdrop-blur-sm">
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-blue-100/80">Phòng hoạt động</p>
                <p className="mt-3 text-4xl font-black tracking-[-0.05em]">{rooms.length}</p>
                <p className="mt-2 text-sm text-blue-50/72">Tổng số phòng đang mở</p>
              </div>
              <div className="rounded-[28px] border border-white/15 bg-white/10 p-5 backdrop-blur-sm">
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-blue-100/80">Đang chờ</p>
                <p className="mt-3 text-4xl font-black tracking-[-0.05em]">{data.totalWaiting}</p>
                <p className="mt-2 text-sm text-blue-50/72">Bệnh nhân trong hàng chờ</p>
              </div>
              <div className="rounded-[28px] border border-white/15 bg-white/10 p-5 backdrop-blur-sm">
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-blue-100/80">Đang khám</p>
                <p className="mt-3 text-4xl font-black tracking-[-0.05em]">{data.totalInProgress}</p>
                <p className="mt-2 text-sm text-blue-50/72">Phòng đang tiếp nhận</p>
              </div>
            </div>
          </div>
        </section>

        {rooms.length > 0 ? (
          <div className="mt-8 space-y-10">
            <RoomSection
              title="Đang khám"
              description="Các phòng đang có bệnh nhân trong trạng thái khám hoặc xử lý trực tiếp."
              rooms={busyRooms}
            />
            <RoomSection
              title="Sắp tới lượt"
              description="Các phòng hiện rảnh nhưng vẫn còn bệnh nhân chờ vào phòng."
              rooms={waitingRooms}
            />
            <RoomSection
              title="Đang rảnh"
              description="Các phòng đang hoạt động và sẵn sàng nhận bệnh nhân tiếp theo."
              rooms={idleRooms}
            />
          </div>
        ) : (
          <div className="mt-8 rounded-[32px] border border-dashed border-slate-300 bg-white/80 px-6 py-20 text-center shadow-sm">
            <Stethoscope className="mx-auto h-14 w-14 text-slate-300" />
            <h2 className="mt-4 text-2xl font-black tracking-[-0.03em] text-slate-700">Chưa có phòng hoạt động</h2>
            <p className="mt-2 text-sm text-slate-500">Khi có phòng khám mở và được đưa vào bảng đợi, dữ liệu sẽ xuất hiện tại đây.</p>
          </div>
        )}
      </div>
    </div>
  );
}
