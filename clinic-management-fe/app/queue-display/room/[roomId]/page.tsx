'use client';

import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { Armchair, ClipboardCheck, Clock3, Info, Loader2, MapPin, Radio, Stethoscope, Users, Volume2 } from 'lucide-react';
import { QueueEntry, RoomQueueBoardResponse, queueDisplayService } from '@/services/queue-display.service';

const POLL_INTERVAL = 8000;
const MAX_WAITING_ROWS = 6;

function formatClock(date: Date) {
  return date.toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

function formatWaitingSince(iso: string | null) {
  if (!iso) return '--:--';
  const d = new Date(iso);
  return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
}

function statusText(currentPatient: QueueEntry | null, nextPatient: QueueEntry | null) {
  if (currentPatient) {
    return {
      label: 'Đang khám',
      title: 'Bệnh nhân đang khám',
      helper: 'Vui lòng chờ đến lượt gọi tiếp theo',
      shell: 'border-[rgb(37_99_235_/_0.24)] bg-[linear-gradient(135deg,#1d4ed8_0%,#2563eb_58%,#0f766e_100%)] text-white',
      badge: 'bg-white text-[var(--primary-dark)]',
      metric: 'border-white/15 bg-white/10 text-white',
    };
  }

  if (nextPatient) {
    return {
      label: 'Mời vào',
      title: 'Mời bệnh nhân vào phòng',
      helper: 'Phòng đã sẵn sàng tiếp nhận',
      shell: 'border-[rgb(15_118_110_/_0.24)] bg-[linear-gradient(135deg,#0f766e_0%,#14b8a6_62%,#2563eb_100%)] text-white',
      badge: 'bg-white text-[var(--secondary-dark)]',
      metric: 'border-white/15 bg-white/12 text-white',
    };
  }

  return {
    label: 'Sẵn sàng',
    title: 'Phòng đang rảnh',
    helper: 'Chưa có bệnh nhân trong hàng chờ',
    shell: 'border-[var(--app-border)] bg-[var(--app-surface)] text-[var(--app-text)]',
    badge: 'bg-[var(--primary)] text-white',
    metric: 'border-[var(--app-border)] bg-[var(--muted)] text-[var(--app-text)]',
  };
}

function WaitingRow({ entry, highlight }: { entry: QueueEntry; highlight: boolean }) {
  return (
    <div
      className={`grid min-h-[72px] grid-cols-[56px_minmax(0,1fr)_70px] items-center gap-3 rounded-xl border px-3 py-2 ${
        highlight ? 'border-[rgb(15_118_110_/_0.28)] bg-[linear-gradient(135deg,#ffffff_0%,var(--app-teal-soft)_100%)]' : 'border-[var(--app-border)] bg-[var(--app-surface)]'
      }`}
    >
      <div
        className={`flex h-12 w-12 items-center justify-center rounded-lg text-2xl font-black ${
          highlight ? 'bg-[var(--secondary)] text-white' : 'bg-[var(--muted)] text-[var(--app-text)]'
        }`}
      >
        {entry.queuePosition}
      </div>

      <div className="min-w-0">
        <p className="break-words text-xl font-black leading-tight text-[var(--app-text)] xl:text-[1.35rem]">{entry.patientName}</p>
        <p className="mt-0.5 truncate text-xs font-semibold text-[var(--app-muted)] xl:text-sm">Mã BN: {entry.patientId}</p>
      </div>

      <div className="text-right">
        <p className="text-[11px] font-semibold uppercase text-[var(--app-muted)]">Chờ từ</p>
        <p className="mt-1 text-base font-black text-[var(--app-text)]">{formatWaitingSince(entry.waitingSince)}</p>
      </div>
    </div>
  );
}

function GuidanceCard() {
  const items = [
    {
      icon: Volume2,
      title: 'Lắng nghe',
      text: 'Khi có thông báo, vui lòng chú ý lắng nghe.',
    },
    {
      icon: Armchair,
      title: 'Chờ đến lượt',
      text: 'Vui lòng chờ đến lượt và chuẩn bị giấy tờ.',
    },
    {
      icon: ClipboardCheck,
      title: 'Cảm ơn bạn',
      text: 'Cảm ơn bạn đã hợp tác và chúc bạn sức khỏe.',
    },
  ];

  return (
    <section className="rounded-2xl border border-[var(--app-border)] bg-[var(--app-surface)] px-5 py-4 shadow-sm">
      <div className="flex items-center gap-3 text-[var(--primary-dark)]">
        <div className="flex h-9 w-9 items-center justify-center rounded-full bg-[var(--primary)] text-white">
          <Info className="h-5 w-5" />
        </div>
        <h2 className="text-xl font-black">Hướng dẫn</h2>
      </div>

      <div className="mt-4 grid grid-cols-3 divide-x divide-[var(--app-border)]">
        {items.map((item) => {
          const Icon = item.icon;
          return (
            <div key={item.title} className="flex gap-3 px-4 first:pl-0 last:pr-0">
              <Icon className="mt-1 h-8 w-8 shrink-0 text-[var(--primary-dark)]" />
              <div>
                <p className="text-lg font-black text-[var(--app-text)]">{item.title}</p>
                <p className="mt-1 text-sm font-semibold leading-5 text-[var(--app-muted)]">{item.text}</p>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}

function QueueDisplayShell({
  data,
  now,
  lastUpdated,
}: {
  data: RoomQueueBoardResponse;
  now: Date;
  lastUpdated: Date | null;
}) {
  const nextPatient = data.currentPatient ? null : data.waitingList[0] ?? null;
  const featuredPatient = data.currentPatient ?? nextPatient;
  const status = statusText(data.currentPatient, nextPatient);

  const waitingQueue = data.currentPatient ? data.waitingList : data.waitingList.slice(nextPatient ? 1 : 0);
  const visibleWaitingQueue = waitingQueue.slice(0, MAX_WAITING_ROWS);
  const hiddenWaitingCount = Math.max(waitingQueue.length - visibleWaitingQueue.length, 0);

  return (
    <div className="h-screen overflow-hidden bg-[var(--app-bg)] p-3 text-[var(--app-text)]">
      <div className="grid h-full grid-rows-[auto_minmax(0,1fr)] gap-3">
        <header className="rounded-2xl border border-[var(--app-border)] bg-[var(--app-surface)] px-5 py-4 shadow-sm">
          <div className="flex min-w-0 items-center justify-between gap-5">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-3 text-sm font-semibold text-[var(--app-muted)]">
                <span className="inline-flex items-center gap-2 text-[var(--secondary)]">
                  <Radio className="h-4 w-4 animate-pulse" />
                  Đang hiển thị
                </span>
                {data.departmentName && (
                  <span className="inline-flex items-center gap-2">
                    <Stethoscope className="h-4 w-4" />
                    {data.departmentName}
                  </span>
                )}
                {data.location && (
                  <span className="inline-flex items-center gap-2">
                    <MapPin className="h-4 w-4" />
                    {data.location}
                  </span>
                )}
              </div>
              <h1 className="mt-1 truncate text-4xl font-black text-[var(--app-text)] xl:text-5xl">{data.roomName}</h1>
            </div>

            <div className="grid shrink-0 grid-cols-[210px_170px_150px] gap-3">
              <div className="rounded-xl bg-[var(--primary-dark)] px-4 py-3 text-white">
                <p className="inline-flex items-center gap-2 text-xs font-semibold uppercase text-blue-100">
                  <Clock3 className="h-4 w-4" />
                  Bây giờ
                </p>
                <p className="mt-1 text-3xl font-black">{formatClock(now)}</p>
              </div>
              <div className="rounded-xl bg-[var(--muted)] px-4 py-3">
                <p className="text-xs font-semibold uppercase text-[var(--app-muted)]">Đang chờ</p>
                <p className="mt-1 text-3xl font-black">{data.totalWaiting}</p>
              </div>
              <div className="rounded-xl bg-[var(--muted)] px-4 py-3">
                <p className="text-xs font-semibold uppercase text-[var(--app-muted)]">Cập nhật</p>
                <p className="mt-1 text-2xl font-black">{lastUpdated ? formatClock(lastUpdated) : '--:--'}</p>
              </div>
            </div>
          </div>
        </header>

        <main className="grid min-h-0 grid-cols-[minmax(0,1.35fr)_minmax(360px,0.65fr)] gap-3">
          <div className="flex min-h-0 flex-col gap-3 overflow-hidden">
            <section className={`w-full overflow-hidden rounded-2xl border p-4 shadow-sm xl:p-5 ${status.shell}`}>
              <div className="overflow-hidden">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <span className={`inline-flex rounded-full px-4 py-1.5 text-xl font-black uppercase xl:text-2xl ${status.badge}`}>
                      {status.label}
                    </span>
                    <p className="mt-3 text-2xl font-black xl:text-3xl">{status.title}</p>
                    <p className="mt-1 text-base font-semibold opacity-80 xl:text-lg">{status.helper}</p>
                  </div>
                  <div className="hidden text-right xl:block">
                    <p className="text-sm font-semibold uppercase opacity-70">Bác sĩ phụ trách</p>
                    <p className="mt-1 max-w-[360px] truncate text-2xl font-black">
                      {data.doctorName ?? 'Chưa có lịch trực'}
                    </p>
                  </div>
                </div>

                {featuredPatient ? (
                  <div className="mt-4 min-w-0 overflow-hidden">
                    <p className="break-words py-1 text-[clamp(3rem,8vh,5.25rem)] font-black leading-[1.12]">
                      {featuredPatient.patientName}
                    </p>
                    <div className="mt-4 max-w-md">
                      <div className={`min-h-[86px] rounded-xl border px-4 py-3 ${status.metric}`}>
                        <p className="text-xs font-semibold uppercase opacity-70">Mã bệnh nhân</p>
                        <p className="mt-1 truncate text-[clamp(1.5rem,5vh,2rem)] font-black leading-none">
                          {featuredPatient.patientId}
                        </p>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="mt-6">
                    <p className="text-[clamp(3rem,11vh,6rem)] font-black leading-none">Chưa có bệnh nhân</p>
                    <p className="mt-4 text-2xl font-semibold text-slate-500">Phòng có thể nhận lượt tiếp theo</p>
                  </div>
                )}
              </div>
            </section>

            <GuidanceCard />
          </div>

          <aside className="flex min-h-0 flex-col rounded-2xl border border-[var(--app-border)] bg-[var(--app-surface)] p-4 shadow-sm">
            <div className="flex items-end justify-between gap-3 border-b border-[var(--app-border)] pb-3">
              <div>
                <h2 className="text-2xl font-black text-[var(--app-text)] xl:text-3xl">
                  {data.currentPatient ? 'Chờ tiếp theo' : 'Hàng chờ còn lại'}
                </h2>
                <p className="mt-0.5 text-sm font-semibold text-[var(--app-muted)]">
                  Hiển thị tối đa {MAX_WAITING_ROWS} lượt gần nhất
                </p>
              </div>
              <div className="rounded-xl bg-[var(--primary-dark)] px-3 py-2 text-center text-white">
                <p className="text-xs font-semibold uppercase text-blue-100">Còn</p>
                <p className="text-2xl font-black">{waitingQueue.length}</p>
              </div>
            </div>

            {visibleWaitingQueue.length > 0 ? (
              <div className="mt-3 flex min-h-0 flex-1 flex-col gap-2">
                {visibleWaitingQueue.map((entry, index) => (
                  <WaitingRow key={entry.appointmentId} entry={entry} highlight={index === 0} />
                ))}
              </div>
            ) : (
              <div className="flex min-h-0 flex-1 flex-col items-center justify-center rounded-xl border border-dashed border-[var(--app-border)] bg-[var(--muted)] px-6 text-center">
                <Users className="h-12 w-12 text-[var(--app-muted)]" />
                <p className="mt-4 text-2xl font-black text-[var(--app-text)]">Không còn bệnh nhân chờ</p>
              </div>
            )}

            {hiddenWaitingCount > 0 && (
              <div className="mt-3 rounded-xl bg-[var(--warning-soft)] px-4 py-2 text-center text-base font-black text-[var(--warning-foreground)]">
                Còn {hiddenWaitingCount} bệnh nhân chưa hiển thị
              </div>
            )}
          </aside>
        </main>
      </div>
    </div>
  );
}

export default function RoomQueueBoardPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const [data, setData] = useState<RoomQueueBoardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [now, setNow] = useState(() => new Date());

  const fetchData = useCallback(async () => {
    if (!roomId) return;

    try {
      const res = await queueDisplayService.getRoomQueueBoard(roomId);
      setData(res);
      setError(null);
      setLastUpdated(new Date());
    } catch {
      setError('Không thể tải bảng đợi của phòng');
    } finally {
      setLoading(false);
    }
  }, [roomId]);

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, POLL_INTERVAL);
    return () => clearInterval(interval);
  }, [fetchData]);

  useEffect(() => {
    const interval = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-[var(--app-bg)] text-[var(--app-text)]">
        <div className="flex flex-col items-center gap-4 rounded-2xl border border-[var(--app-border)] bg-[var(--app-surface)] px-10 py-12 shadow-sm">
          <Loader2 className="h-10 w-10 animate-spin text-[var(--primary)]" />
          <p className="text-xl font-black">Đang tải bảng đợi của phòng...</p>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="flex h-screen items-center justify-center bg-[var(--app-bg)] text-[var(--app-text)]">
        <div className="rounded-2xl border border-[var(--error)] bg-[var(--app-surface)] px-10 py-12 text-center shadow-sm">
          <p className="text-2xl font-black text-[var(--error)]">{error ?? 'Không tải được dữ liệu'}</p>
          <button
            onClick={fetchData}
            className="mt-6 rounded-xl bg-[var(--primary)] px-6 py-3 text-lg font-black text-white transition hover:bg-[var(--primary-dark)]"
          >
            Tải lại
          </button>
        </div>
      </div>
    );
  }

  return <QueueDisplayShell data={data} now={now} lastUpdated={lastUpdated} />;
}
