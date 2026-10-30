'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { roomService, RoomResponse } from '@/services/room.service';
import { departmentService, DepartmentResponse } from '@/services/department.service';
import { staffScheduleService } from '@/services/staff-schedule.service';
import { User, userService } from '@/services/user.service';
import { StaffScheduleResponse } from '@/types/staff-schedule.types';
import { workingTimeSlotService, WorkingTimeSlotResponse } from '@/services/workingTimeSlot.service';

type DutyForm = {
  dutyDate: string;
  slotId: number | '';
  roomId: string;
  doctorId: number | '';
  nurseId: number | '';
};

type AssignmentRow = {
  id: number;
  doctorId: number;
  doctorCode: string;
  doctorName: string;
  nurseId: number | '';
};

type WeekDayInfo = {
  key: string;
  label: string;
};

type WeeklyShift = {
  slotId: number | null;
  label: string;
  startTime: string;
  endTime: string;
};

type RoomDoctorMember = {
  userId: number;
};

type WeeklyScheduleMap = Record<string, StaffScheduleResponse[]>;

type RoomStatusFilter = 'ALL' | 'HAS_SCHEDULE' | 'NO_SCHEDULE';

type AssignmentStatusFilter = 'ALL' | 'ASSIGNED' | 'UNASSIGNED';

const FALLBACK_SHIFTS: WeeklyShift[] = [
  { slotId: null, label: 'SÁNG', startTime: '07:00', endTime: '11:30' },
  { slotId: null, label: 'CHIỀU', startTime: '13:00', endTime: '17:30' },
];

const WEEKDAY_LABELS = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ nhật'];

const CLINICAL_DOCTOR_ROLES = ['ROLE_DOCTOR', 'ROLE_CLINICALDOCTOR', 'ROLE_CLINICAL_DOCTOR'];
const PARACLINICAL_DOCTOR_ROLES = ['ROLE_SPECIALISTDOCTOR', 'ROLE_SPECIALIST_DOCTOR'];

const hasAnyRole = (user: User, roleNames: string[]) =>
  user.roles.some((role) => roleNames.includes(role));

const isClinicalDoctor = (user: User) => hasAnyRole(user, CLINICAL_DOCTOR_ROLES);

const isParaclinicalDoctor = (user: User) =>
  hasAnyRole(user, PARACLINICAL_DOCTOR_ROLES) || !isClinicalDoctor(user);

const getDoctorCategoryLabel = (user: User) =>
  isClinicalDoctor(user) ? 'Lâm sàng' : 'Cận lâm sàng';

const formatStaffCode = (staffId?: string, index?: number) => {
  if (staffId) return staffId;
  return `D${String((index || 0) + 1)}`;
};

const toIsoDate = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const toHHmm = (timeValue: string) => {
  if (!timeValue) return '--:--';
  return String(timeValue).slice(0, 5);
};

const startOfWeekMonday = (date: Date) => {
  const next = new Date(date);
  next.setHours(0, 0, 0, 0);
  const day = (next.getDay() + 6) % 7;
  next.setDate(next.getDate() - day);
  return next;
};

const addDays = (date: Date, amount: number) => {
  const next = new Date(date);
  next.setDate(next.getDate() + amount);
  return next;
};

const formatDateShort = (date: Date) =>
  `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')}`;

const formatWeekLabel = (weekStart: Date) => {
  const weekEnd = addDays(weekStart, 6);
  return `Tuần ${formatDateShort(weekStart)} - ${formatDateShort(weekEnd)}/${weekEnd.getFullYear()}`;
};

const buildWeekDates = (weekStart: Date): WeekDayInfo[] =>
  WEEKDAY_LABELS.map((label, index) => ({
    key: toIsoDate(addDays(weekStart, index)),
    label,
  }));

const getShiftLabelFromName = (shiftName: string) => {
  const normalized = shiftName.toLowerCase();
  if (normalized.includes('sáng') || normalized.includes('sang')) return 'SÁNG';
  if (normalized.includes('chiều') || normalized.includes('chieu')) return 'CHIỀU';
  return shiftName.toUpperCase();
};

const parseErrorMessage = (error: unknown) => {
  const data = (error as { response?: { data?: { message?: string } } })?.response?.data;
  return data?.message || 'Không thể thực hiện thao tác. Vui lòng thử lại.';
};

const isGeneralRoom = (room?: RoomResponse) => {
  if (!room) return false;

  const roomType = (room.roomType || '').toUpperCase();
  const roomId = (room.roomId || '').toUpperCase();
  const roomName = (room.roomName || '').toLowerCase();

  return (
    roomType.includes('GENERAL') ||
    roomId.startsWith('DK') ||
    roomName.includes('đa khoa') ||
    roomName.includes('general')
  );
};

const isDoctorEligibleForRoom = (
  doctor: User,
  room?: RoomResponse,
  doctorDeptType?: string
) => {
  if (!room) return true;

  const generalRoom = isGeneralRoom(room);

  if (doctorDeptType === 'CLINICAL') return generalRoom;
  if (doctorDeptType === 'PARACLINICAL') return !generalRoom;

  return generalRoom ? isClinicalDoctor(doctor) : isParaclinicalDoctor(doctor);
};

const asRoomDoctorMembers = (value: unknown): RoomDoctorMember[] => {
  if (!Array.isArray(value)) return [];

  return value
    .map((item) => {
      const userId = Number((item as { userId?: unknown })?.userId);
      return { userId };
    })
    .filter((item) => Number.isFinite(item.userId));
};

function DutyModal({
  isOpen,
  title,
  form,
  rooms,
  shifts,
  doctors,
  nurses,
  getDoctorCategory,
  doctorUnavailableMessage,
  nurseUnavailableMessage,
  saving,
  saveDisabled,
  preserveStaffOnContextChange = false,
  onClose,
  onChange,
  onSave,
  saveLabel,
}: {
  isOpen: boolean;
  title: string;
  form: DutyForm;
  rooms: RoomResponse[];
  shifts: WeeklyShift[];
  doctors: User[];
  nurses: User[];
  getDoctorCategory: (doctor: User) => string;
  doctorUnavailableMessage?: string | null;
  nurseUnavailableMessage?: string | null;
  saving: boolean;
  saveDisabled?: boolean;
  preserveStaffOnContextChange?: boolean;
  onClose: () => void;
  onChange: (value: DutyForm) => void;
  onSave: () => void;
  saveLabel: string;
}) {
  if (!isOpen) return null;

  const selectableShifts = shifts.filter((shift) => shift.slotId !== null);

  return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4">
      <div className="w-full max-w-md border border-slate-200 bg-white shadow-xl shadow-slate-900/10">
        <div className="flex items-center justify-between bg-blue-600 px-3 py-2 text-xs font-bold uppercase text-white">
          <span>{title}</span>
          <button onClick={onClose}>X</button>
        </div>

        <div className="space-y-2 p-3 text-xs">
          <div>
            <label className="mb-1 block font-bold">Ngày trực</label>
            <input
              type="date"
              className="h-9 w-full border border-slate-200 px-2"
              value={form.dutyDate}
              onChange={(event) =>
                onChange({
                  ...form,
                  dutyDate: event.target.value,
                  doctorId: preserveStaffOnContextChange ? form.doctorId : '',
                  nurseId: preserveStaffOnContextChange ? form.nurseId : '',
                })
              }
            />
          </div>

          <div>
            <label className="mb-1 block font-bold">Ca làm việc</label>
            <select
              className="h-9 w-full border border-slate-200 px-2"
              value={form.slotId}
              onChange={(event) =>
                onChange({
                  ...form,
                  slotId: Number(event.target.value) || '',
                  doctorId: preserveStaffOnContextChange ? form.doctorId : '',
                  nurseId: preserveStaffOnContextChange ? form.nurseId : '',
                })
              }
            >
              <option value="">-- Chọn ca --</option>
              {selectableShifts.map((shift) => (
                <option key={shift.slotId} value={shift.slotId ?? ''}>
                  {shift.label} ({shift.startTime} - {shift.endTime})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-1 block font-bold">Phòng khám</label>
            <select
              className="h-9 w-full border border-slate-200 px-2"
              value={form.roomId}
              onChange={(event) =>
                onChange({
                  ...form,
                  roomId: event.target.value,
                  doctorId: preserveStaffOnContextChange ? form.doctorId : '',
                  nurseId: preserveStaffOnContextChange ? form.nurseId : '',
                })
              }
            >
              {rooms.map((room) => (
                <option key={room.roomId} value={room.roomId}>
                  {room.roomName}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2 border border-dashed border-slate-300 p-2">
            <p className="font-bold uppercase">Nhân sự phụ trách (ghép cặp)</p>

            <div>
              <label className="mb-1 block font-semibold">Bác sĩ chính [*]</label>
              <select
                className="h-9 w-full border border-slate-200 px-2"
                value={form.doctorId}
                onChange={(event) => onChange({ ...form, doctorId: Number(event.target.value) || '' })}
                disabled={saving || Boolean(doctorUnavailableMessage)}
              >
                <option value="">-- Chọn bác sĩ --</option>
                {doctors.map((doctor) => (
                  <option key={doctor.userId} value={doctor.userId}>
                    {`${doctor.fullName} (${getDoctorCategory(doctor)})`}
                  </option>
                ))}
              </select>
              <p className={`mt-1 text-[10px] ${doctorUnavailableMessage ? 'font-semibold text-red-600' : 'text-slate-500'}`}>
                {doctorUnavailableMessage ||
                  'Chỉ hiển thị bác sĩ phù hợp và chưa có lịch trong ca đã chọn.'}
              </p>
            </div>

            <div>
              <label className="mb-1 block font-semibold">Y tá phụ trách</label>
              <select
                className="h-9 w-full border border-slate-200 px-2"
                value={form.nurseId}
                onChange={(event) => onChange({ ...form, nurseId: Number(event.target.value) || '' })}
                disabled={saving || Boolean(nurseUnavailableMessage)}
              >
                <option value="">-- Chọn y tá (tùy chọn) --</option>
                {nurses.map((nurse) => (
                  <option key={nurse.userId} value={nurse.userId}>
                    {nurse.fullName}
                  </option>
                ))}
              </select>
              {nurseUnavailableMessage ? (
                <p className="mt-1 text-[10px] font-semibold text-red-600">{nurseUnavailableMessage}</p>
              ) : (
                <p className="mt-1 text-[10px] text-slate-500">
                  Chỉ hiển thị y tá chưa có lịch trong ca đã chọn.
                </p>
              )}
            </div>
          </div>
        </div>

        <div className="border-t border-slate-200 p-3">
          <button
            onClick={onSave}
            disabled={saving || saveDisabled}
              className="h-9 w-full border border-blue-600 bg-blue-600 text-xs font-bold uppercase text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {saving ? 'Đang lưu...' : saveLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

type EditingScheduleState = {
  doctorScheduleId: number;
  nurseScheduleId: number | null;
  originalDoctorId: number;
  originalNurseId: number | '';
  originalDate: string;
  originalSlotId: number;
  originalRoomId: string;
};

export default function StaffSchedulesPage() {
  const searchParams = useSearchParams();

  const [baseLoading, setBaseLoading] = useState(true);
  const [tableLoading, setTableLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [staff, setStaff] = useState<User[]>([]);
  const [rooms, setRooms] = useState<RoomResponse[]>([]);
  const [activeSlots, setActiveSlots] = useState<WorkingTimeSlotResponse[]>([]);
  const [doctorRoomByUserId, setDoctorRoomByUserId] = useState<Record<number, string>>({});
  const [doctorDeptIdByUserId, setDoctorDeptIdByUserId] = useState<Record<number, number>>({});
  const [doctorDeptTypeByUserId, setDoctorDeptTypeByUserId] = useState<Record<number, string>>({});

  const [searchAssign, setSearchAssign] = useState('');
  const [searchStock, setSearchStock] = useState('');
  const [roomStatusFilter, setRoomStatusFilter] = useState<RoomStatusFilter>('ALL');
  const [assignmentStatusFilter, setAssignmentStatusFilter] =
    useState<AssignmentStatusFilter>('ALL');
  const [selectedDeptId, setSelectedDeptId] = useState<number | 'ALL'>('ALL');
  const [weekStart, setWeekStart] = useState<Date>(() => startOfWeekMonday(new Date()));
  const [schedulesByDate, setSchedulesByDate] = useState<WeeklyScheduleMap>({});

  const [assignments, setAssignments] = useState<AssignmentRow[]>([]);

  const [modalOpen, setModalOpen] = useState(false);
  const [modalTitle, setModalTitle] = useState('Xếp ca trực mới');
  const [editingSchedule, setEditingSchedule] = useState<EditingScheduleState | null>(null);

  const [dutyForm, setDutyForm] = useState<DutyForm>({
    dutyDate: toIsoDate(new Date()),
    slotId: '',
    roomId: '',
    doctorId: '',
    nurseId: '',
  });

  const selectedDoctorIds = useMemo(() => {
    const raw = searchParams.get('doctorIds');
    if (!raw) return [];
    return raw
      .split(',')
      .map((value) => Number(value.trim()))
      .filter((value) => Number.isFinite(value));
  }, [searchParams]);

  const doctors = useMemo(
    () =>
      staff.filter(
        (item) =>
          hasAnyRole(item, [...CLINICAL_DOCTOR_ROLES, ...PARACLINICAL_DOCTOR_ROLES]) &&
          item.isActive
      ),
    [staff]
  );

  const getDoctorCategoryText = useCallback(
    (doctor: User) => {
      const deptType = doctorDeptTypeByUserId[doctor.userId];
      if (deptType === 'CLINICAL') return 'Lâm sàng';
      if (deptType === 'PARACLINICAL') return 'Cận lâm sàng';
      return getDoctorCategoryLabel(doctor);
    },
    [doctorDeptTypeByUserId]
  );

  const isDoctorEligibleByRoomRule = useCallback(
    (doctor: User, room?: RoomResponse) =>
      isDoctorEligibleForRoom(doctor, room, doctorDeptTypeByUserId[doctor.userId]),
    [doctorDeptTypeByUserId]
  );

  const nurses = useMemo(
    () => staff.filter((item) => hasAnyRole(item, ['ROLE_NURSE']) && item.isActive),
    [staff]
  );

  const displayShifts = useMemo(() => {
    const sorted = [...activeSlots].sort((a, b) => toHHmm(a.startTime).localeCompare(toHHmm(b.startTime)));
    const mapped = sorted.slice(0, 2).map((slot) => ({
      slotId: slot.slotId,
      label: getShiftLabelFromName(slot.shiftName),
      startTime: toHHmm(slot.startTime),
      endTime: toHHmm(slot.endTime),
    }));

    if (mapped.length === 2) return mapped;
    if (mapped.length === 1) return [mapped[0], FALLBACK_SHIFTS[1]];
    return FALLBACK_SHIFTS;
  }, [activeSlots]);

  const weekDates = useMemo(() => buildWeekDates(weekStart), [weekStart]);
  const selectedWeekLabel = useMemo(() => formatWeekLabel(weekStart), [weekStart]);

  const departmentOptions = useMemo(() => {
    const deptMap = new Map<number, string>();

    rooms.forEach((room) => {
      if (room.deptId != null && room.deptName) {
        deptMap.set(room.deptId, room.deptName);
      }
    });

    return Array.from(deptMap.entries())
      .map(([deptId, deptName]) => ({ deptId, deptName }))
      .sort((a, b) => a.deptName.localeCompare(b.deptName, 'vi'));
  }, [rooms]);

  const visibleRooms = useMemo(() => {
    if (selectedDeptId === 'ALL') return rooms;
    return rooms.filter((room) => room.deptId === selectedDeptId);
  }, [rooms, selectedDeptId]);

  const roomNameById = useMemo(() => {
    const map: Record<string, string> = {};
    rooms.forEach((room) => {
      map[room.roomId] = room.roomName;
    });
    return map;
  }, [rooms]);

  const roomIdByName = useMemo(() => {
    const map: Record<string, string> = {};
    rooms.forEach((room) => {
      map[room.roomName.trim().toLowerCase()] = room.roomId;
    });
    return map;
  }, [rooms]);

  const getScheduleRoomId = useCallback(
    (schedule: StaffScheduleResponse): string | undefined => {
      // Ưu tiên roomId từ response (cột room_id trong DB)
      if (schedule.roomId) return schedule.roomId;
      // Fallback: parse note cho bản ghi cũ chưa migrate
      const note = (schedule.note || '').trim();
      const prefixed = note.match(/^Phòng:\s*(.+)$/i);
      if (prefixed && prefixed[1]) {
        const noteRoomName = prefixed[1].trim().toLowerCase();
        const byName = roomIdByName[noteRoomName];
        if (byName) return byName;
      }
      return doctorRoomByUserId[schedule.userId];
    },
    [doctorRoomByUserId, roomIdByName]
  );

  const selectedRoom = useMemo(
    () => rooms.find((room) => room.roomId === dutyForm.roomId),
    [dutyForm.roomId, rooms]
  );

  const doctorsInSelectedRoom = useMemo(() => {
    if (!dutyForm.roomId) return doctors;

    const roomDeptId = selectedRoom?.deptId;
    const doctorsInSameDepartment =
      typeof roomDeptId === 'number'
        ? doctors.filter((doctor) => doctorDeptIdByUserId[doctor.userId] === roomDeptId)
        : doctors;

    const eligibleDoctors = doctorsInSameDepartment.filter((doctor) =>
      isDoctorEligibleByRoomRule(doctor, selectedRoom)
    );

    // Không ép cứng phải có mapping phòng, vì dữ liệu cũ có thể chưa map đầy đủ.
    // Nếu có bác sĩ đã map đúng phòng thì ưu tiên hiển thị trước, sau đó vẫn giữ bác sĩ hợp lệ chưa map.
    const mappedDoctors = eligibleDoctors.filter(
      (doctor) => doctorRoomByUserId[doctor.userId] === dutyForm.roomId
    );
    const unmappedEligibleDoctors = eligibleDoctors.filter(
      (doctor) => !doctorRoomByUserId[doctor.userId]
    );

    if (mappedDoctors.length > 0) {
      return [...mappedDoctors, ...unmappedEligibleDoctors];
    }

    return eligibleDoctors;
  }, [
    doctors,
    doctorDeptIdByUserId,
    doctorRoomByUserId,
    dutyForm.roomId,
    isDoctorEligibleByRoomRule,
    selectedRoom,
  ]);

  const editingScheduleIds = useMemo(
    () =>
      editingSchedule
        ? [
            editingSchedule.doctorScheduleId,
            ...(editingSchedule.nurseScheduleId ? [editingSchedule.nurseScheduleId] : []),
          ]
        : [],
    [editingSchedule]
  );

  const scheduledUserIdsForSelectedDuty = useMemo(() => {
    const slotId = typeof dutyForm.slotId === 'number' ? dutyForm.slotId : null;
    if (!slotId || !dutyForm.dutyDate) return new Set<number>();

    return new Set(
      (schedulesByDate[dutyForm.dutyDate] || [])
        .filter((schedule) => schedule.slotId === slotId)
        .filter((schedule) => !editingScheduleIds.includes(schedule.scheduleId))
        .map((schedule) => schedule.userId)
    );
  }, [dutyForm.dutyDate, dutyForm.slotId, editingScheduleIds, schedulesByDate]);

  const selectedRoomSchedulesForDuty = useMemo(() => {
    const slotId = typeof dutyForm.slotId === 'number' ? dutyForm.slotId : null;
    if (!slotId || !dutyForm.dutyDate || !dutyForm.roomId) return [];

    return (schedulesByDate[dutyForm.dutyDate] || []).filter(
      (schedule) => schedule.slotId === slotId && getScheduleRoomId(schedule) === dutyForm.roomId
    );
  }, [dutyForm.dutyDate, dutyForm.roomId, dutyForm.slotId, getScheduleRoomId, schedulesByDate]);

  const selectedRoomDoctorConflict = useMemo(
    () =>
      selectedRoomSchedulesForDuty.find(
        (schedule) =>
          schedule.role !== 'NURSE' && schedule.scheduleId !== editingSchedule?.doctorScheduleId
      ),
    [editingSchedule?.doctorScheduleId, selectedRoomSchedulesForDuty]
  );

  const selectedRoomNurseConflict = useMemo(
    () =>
      selectedRoomSchedulesForDuty.find(
        (schedule) =>
          schedule.role === 'NURSE' && schedule.scheduleId !== editingSchedule?.nurseScheduleId
      ),
    [editingSchedule?.nurseScheduleId, selectedRoomSchedulesForDuty]
  );

  const availableDoctorsForDuty = useMemo(
    () =>
      doctorsInSelectedRoom.filter(
        (doctor) =>
          !scheduledUserIdsForSelectedDuty.has(doctor.userId) ||
          doctor.userId === editingSchedule?.originalDoctorId
      ),
    [doctorsInSelectedRoom, editingSchedule?.originalDoctorId, scheduledUserIdsForSelectedDuty]
  );

  const availableNursesForDuty = useMemo(
    () =>
      nurses.filter(
        (nurse) =>
          !scheduledUserIdsForSelectedDuty.has(nurse.userId) ||
          nurse.userId === editingSchedule?.originalNurseId
      ),
    [editingSchedule?.originalNurseId, nurses, scheduledUserIdsForSelectedDuty]
  );

  const roomDoctorConflictMessage = selectedRoomDoctorConflict
    ? `Phòng/ca này đã có bác sĩ ${selectedRoomDoctorConflict.fullName}. Bấm vào lịch hiện có để cập nhật hoặc gỡ trước.`
    : null;

  const roomNurseConflictMessage = selectedRoomNurseConflict
    ? `Phòng/ca này đã có y tá ${selectedRoomNurseConflict.fullName}. Bấm vào lịch hiện có để cập nhật hoặc gỡ trước.`
    : null;

  const assignableDoctorsForDuty = useMemo(
    () => (selectedRoomDoctorConflict ? [] : availableDoctorsForDuty),
    [availableDoctorsForDuty, selectedRoomDoctorConflict]
  );
  const assignableNursesForDuty = useMemo(
    () => (selectedRoomNurseConflict ? [] : availableNursesForDuty),
    [availableNursesForDuty, selectedRoomNurseConflict]
  );
  const modalDoctorOptions = assignableDoctorsForDuty;
  const modalNurseOptions = assignableNursesForDuty;
  const modalSaveDisabled =
    Boolean(selectedRoomDoctorConflict) || Boolean(dutyForm.nurseId && selectedRoomNurseConflict);

  const filteredAssignments = useMemo(() => {
    const keyword = searchAssign.trim().toLowerCase();

    return assignments.filter((row) => {
      const byKeyword =
        !keyword ||
        [row.doctorCode, row.doctorName].some((value) => value.toLowerCase().includes(keyword));

      const byStatus =
        assignmentStatusFilter === 'ALL'
          ? true
          : assignmentStatusFilter === 'ASSIGNED'
            ? row.nurseId !== ''
            : row.nurseId === '';

      return byKeyword && byStatus;
    });
  }, [assignments, assignmentStatusFilter, searchAssign]);

  const filteredDoctorsStock = useMemo(() => {
    const keyword = searchStock.trim().toLowerCase();
    if (!keyword) return assignableDoctorsForDuty;

    return assignableDoctorsForDuty.filter((doctor) =>
      [doctor.staffId || '', doctor.fullName].some((value) => value.toLowerCase().includes(keyword))
    );
  }, [assignableDoctorsForDuty, searchStock]);

  const isAssignmentMode = selectedDoctorIds.length > 0;
  const tableColSpan = weekDates.length + 2;

  const roomHasScheduleById = useMemo(() => {
    const map: Record<string, boolean> = {};

    visibleRooms.forEach((room) => {
      const hasSchedule = weekDates.some((day) =>
        displayShifts.some((shift) => {
          if (shift.slotId === null) return false;

          return (schedulesByDate[day.key] || []).some(
            (schedule) =>
              schedule.slotId === shift.slotId && getScheduleRoomId(schedule) === room.roomId
          );
        })
      );

      map[room.roomId] = hasSchedule;
    });

    return map;
  }, [displayShifts, getScheduleRoomId, schedulesByDate, visibleRooms, weekDates]);

  const statusFilteredRooms = useMemo(() => {
    if (roomStatusFilter === 'ALL') return visibleRooms;

    return visibleRooms.filter((room) => {
      const hasSchedule = roomHasScheduleById[room.roomId] || false;
      return roomStatusFilter === 'HAS_SCHEDULE' ? hasSchedule : !hasSchedule;
    });
  }, [roomHasScheduleById, roomStatusFilter, visibleRooms]);

  useEffect(() => {
    let cancelled = false;

    const fetchAllPages = async <T,>(
      fetcher: (page: number) => Promise<{ items: T[]; totalPages: number }>
    ): Promise<T[]> => {
      const all: T[] = [];
      let page = 0;
      while (true) {
        const { items, totalPages } = await fetcher(page);
        all.push(...items);
        if (totalPages === 0 || page >= totalPages - 1) break;
        page += 1;
      }
      return all;
    };

    const fetchInitialData = async () => {
      setBaseLoading(true);
      try {
        // ── Phase 1: Song song 4 API list độc lập ──
        const [allUsers, slotResponse, loadedRooms, loadedDepartments] = await Promise.all([
          fetchAllPages<User>(async (p) => {
            const r = await userService.getUsers(p, 200, '', 'ALL');
            return { items: r.users || [], totalPages: r.totalPages ?? 0 };
          }),
          workingTimeSlotService.getActive(),
          fetchAllPages<RoomResponse>(async (p) => {
            const r = await roomService.getList(undefined, '', p, 200);
            return { items: r.rooms || [], totalPages: r.totalPages ?? 0 };
          }),
          fetchAllPages<DepartmentResponse>(async (p) => {
            const r = await departmentService.getList('', p, 200);
            return { items: r.departments || [], totalPages: r.totalPages ?? 0 };
          }),
        ]);

        if (cancelled) return;

        const uniqueUsers = Array.from(
          new Map(allUsers.map((item) => [item.userId, item])).values()
        );
        const list = uniqueUsers.filter(
          (item) => !hasAnyRole(item, ['ROLE_SYSTEM_ADMIN', 'ROLE_ADMIN', 'ROLE_PATIENT'])
        );

        const doctorDeptIdMap: Record<number, number> = {};
        const doctorDeptTypeMap: Record<number, string> = {};

        list.forEach((item) => {
          if (typeof item.deptId === 'number') {
            doctorDeptIdMap[item.userId] = item.deptId;
          }

          const normalizedDeptType = (item.deptType || '').toUpperCase();
          if (normalizedDeptType) {
            doctorDeptTypeMap[item.userId] = normalizedDeptType;
          }
        });

        setStaff(list);
        setActiveSlots(slotResponse || []);
        setRooms(loadedRooms);

        const sortedSlots = [...(slotResponse || [])].sort((a, b) =>
          toHHmm(a.startTime).localeCompare(toHHmm(b.startTime))
        );
        setDutyForm((prev) => ({
          ...prev,
          slotId: prev.slotId || sortedSlots[0]?.slotId || '',
          roomId: prev.roomId || loadedRooms[0]?.roomId || '',
        }));

        // ── Phase 2: Room detail (song song) + derive deptType từ list (0 API) ──
        const deptTypeByDeptId: Record<number, string> = {};
        loadedDepartments.forEach((dept) => {
          deptTypeByDeptId[dept.deptId] = (dept.deptType || '').toUpperCase();
        });

        const roomMap: Record<number, string> = {};

        await Promise.all(
          loadedRooms.map(async (room) => {
            try {
              const detail = await roomService.getDetail(room.roomId);
              const doctorsInRoom = asRoomDoctorMembers(detail.doctors);
              doctorsInRoom.forEach((doctor) => {
                roomMap[doctor.userId] = room.roomId;
                if (!(doctor.userId in doctorDeptIdMap) && room.deptId != null) {
                  doctorDeptIdMap[doctor.userId] = room.deptId;
                }
                if (!doctorDeptTypeMap[doctor.userId] && room.deptId != null && deptTypeByDeptId[room.deptId]) {
                  doctorDeptTypeMap[doctor.userId] = deptTypeByDeptId[room.deptId];
                }
              });
            } catch (error) {
              console.error(`Không thể tải chi tiết phòng ${room.roomId}:`, error);
            }
          })
        );

        if (cancelled) return;

        setDoctorRoomByUserId(roomMap);
        setDoctorDeptIdByUserId(doctorDeptIdMap);
        setDoctorDeptTypeByUserId(doctorDeptTypeMap);
      } catch (error) {
        console.error('Không thể tải dữ liệu lịch làm việc:', error);
        if (cancelled) return;
        setStaff([]);
        setRooms([]);
        setDoctorRoomByUserId({});
        setDoctorDeptIdByUserId({});
        setDoctorDeptTypeByUserId({});
      } finally {
        if (!cancelled) {
          setBaseLoading(false);
        }
      }
    };

    fetchInitialData();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (baseLoading) return;

    const sourceCandidates =
      selectedDoctorIds.length > 0
        ? assignableDoctorsForDuty.filter((doctor) => selectedDoctorIds.includes(doctor.userId))
        : assignableDoctorsForDuty;
    const source = sourceCandidates.slice(0, 1);

    const mapped: AssignmentRow[] = source.map((doctor, index) => ({
      id: index + 1,
      doctorId: doctor.userId,
      doctorCode: formatStaffCode(doctor.staffId, index),
      doctorName: doctor.fullName,
      nurseId: '',
    }));

    setAssignments(mapped);
  }, [assignableDoctorsForDuty, baseLoading, selectedDoctorIds]);

  useEffect(() => {
    if (visibleRooms.length === 0) return;

    const stillExists = visibleRooms.some((room) => room.roomId === dutyForm.roomId);
    if (!stillExists) {
      setDutyForm((prev) => ({ ...prev, roomId: visibleRooms[0].roomId, doctorId: '', nurseId: '' }));
    }
  }, [dutyForm.roomId, visibleRooms]);

  useEffect(() => {
    const selectableSlots = displayShifts
      .map((shift) => shift.slotId)
      .filter((slotId): slotId is number => slotId !== null);

    if (selectableSlots.length === 0) return;

    if (typeof dutyForm.slotId !== 'number' || !selectableSlots.includes(dutyForm.slotId)) {
      setDutyForm((prev) => ({ ...prev, slotId: selectableSlots[0] }));
    }
  }, [displayShifts, dutyForm.slotId]);

  const fetchSchedulesByDate = useCallback(async (date: string) => {
    let page = 0;
    const size = 200;
    const collected: StaffScheduleResponse[] = [];

    while (true) {
      const response = await staffScheduleService.getStaffSchedules({
        date,
        page,
        size,
      });

      collected.push(...(response.schedules || []));

      if ((response.totalPages ?? 0) === 0 || page >= response.totalPages - 1) {
        break;
      }

      page += 1;
    }

    return collected;
  }, []);

  const syncSchedulesForDate = useCallback(
    async (date: string) => {
      const nextSchedules = await fetchSchedulesByDate(date);
      setSchedulesByDate((prev) => ({
        ...prev,
        [date]: nextSchedules,
      }));
      return nextSchedules;
    },
    [fetchSchedulesByDate]
  );

  const syncMultipleScheduleDates = useCallback(
    async (dates: string[]) => {
      const uniqueDates = Array.from(new Set(dates.filter(Boolean)));
      await Promise.all(uniqueDates.map((date) => syncSchedulesForDate(date)));
    },
    [syncSchedulesForDate]
  );

  const findConflictInSchedules = useCallback(
    (
      schedules: StaffScheduleResponse[],
      userId: number,
      slotId: number,
      workDate: string,
      ignoredScheduleIds: number[] = []
    ): StaffScheduleResponse | undefined =>
      schedules.find(
        (schedule) =>
          schedule.userId === userId &&
          schedule.slotId === slotId &&
          schedule.workDate === workDate &&
          !ignoredScheduleIds.includes(schedule.scheduleId)
      ),
    []
  );

  const findRoomRoleConflictInSchedules = useCallback(
    (
      schedules: StaffScheduleResponse[],
      roomId: string,
      slotId: number,
      workDate: string,
      role: 'DOCTOR' | 'NURSE',
      ignoredScheduleIds: number[] = []
    ): StaffScheduleResponse | undefined =>
      schedules.find((schedule) => {
        const scheduleIsRequestedRole =
          role === 'NURSE' ? schedule.role === 'NURSE' : schedule.role !== 'NURSE';

        return (
          scheduleIsRequestedRole &&
          schedule.slotId === slotId &&
          schedule.workDate === workDate &&
          getScheduleRoomId(schedule) === roomId &&
          !ignoredScheduleIds.includes(schedule.scheduleId)
        );
      }),
    [getScheduleRoomId]
  );

  const extractSupportingDoctorName = useCallback((schedule: StaffScheduleResponse) => {
    const note = (schedule.note || '').trim();
    const match = note.match(/^(?:Hỗ trợ bác sĩ|Ho tro bac si)\s+(.+)$/i);
    return match?.[1]?.trim();
  }, []);

  const getSchedulesForSlot = useCallback(
    (dateKey: string, slotId: number | null) => {
      if (slotId === null) return [];
      return (schedulesByDate[dateKey] || []).filter((schedule) => schedule.slotId === slotId);
    },
    [schedulesByDate]
  );

  const getLinkedNurseSchedulesForDoctor = useCallback(
    (doctorSchedule: StaffScheduleResponse, daySchedules: StaffScheduleResponse[]) =>
      daySchedules.filter(
        (schedule) =>
          schedule.role === 'NURSE' &&
          schedule.scheduleId !== doctorSchedule.scheduleId &&
          extractSupportingDoctorName(schedule) === doctorSchedule.fullName
      ),
    [extractSupportingDoctorName]
  );

  const getLinkedDoctorScheduleForNurse = useCallback(
    (
      nurseSchedule: StaffScheduleResponse,
      daySchedules: StaffScheduleResponse[],
      roomId?: string
    ) => {
      const doctorName = extractSupportingDoctorName(nurseSchedule);
      if (!doctorName) {
        return undefined;
      }

      return daySchedules.find(
        (schedule) =>
          schedule.role !== 'NURSE' &&
          schedule.fullName === doctorName &&
          (!roomId || getScheduleRoomId(schedule) === roomId)
      );
    },
    [extractSupportingDoctorName, getScheduleRoomId]
  );

  const describeScheduleLocation = useCallback(
    (schedule: StaffScheduleResponse) => {
      const scheduleRoomId = getScheduleRoomId(schedule);
      const scheduleRoomName =
        schedule.roomName || (scheduleRoomId ? roomNameById[scheduleRoomId] : undefined);

      if (scheduleRoomName) {
        return `phòng "${scheduleRoomName}"`;
      }

      if (scheduleRoomId) {
        return `phòng "${scheduleRoomId}"`;
      }

      if (schedule.note) {
        return `ghi chú "${schedule.note}"`;
      }

      return 'một phòng khác';
    },
    [getScheduleRoomId, roomNameById]
  );

  const canReuseNurseSchedule = useCallback(
    (schedule: StaffScheduleResponse) =>
      schedule.role === 'NURSE' &&
      !getScheduleRoomId(schedule) &&
      !extractSupportingDoctorName(schedule),
    [extractSupportingDoctorName, getScheduleRoomId]
  );

  const loadWeekSchedules = useCallback(async (targetWeekStart: Date) => {
    const week = buildWeekDates(targetWeekStart);
    const results = await Promise.all(
      week.map(async (day) => ({
        key: day.key,
        schedules: await fetchSchedulesByDate(day.key),
      }))
    );
    const next: WeeklyScheduleMap = {};
    results.forEach((day) => {
      next[day.key] = day.schedules;
    });

    return next;
  }, [fetchSchedulesByDate]);

  useEffect(() => {
    if (baseLoading) return;

    let cancelled = false;

    const fetchWeeklySchedules = async () => {
      setTableLoading(true);
      try {
        const next = await loadWeekSchedules(weekStart);
        if (!cancelled) {
          setSchedulesByDate(next);
        }
      } catch (error) {
        console.error('Không thể tải lịch tuần:', error);
        if (!cancelled) {
          setSchedulesByDate({});
        }
      } finally {
        if (!cancelled) {
          setTableLoading(false);
        }
      }
    };

    fetchWeeklySchedules();

    return () => {
      cancelled = true;
    };
  }, [baseLoading, loadWeekSchedules, weekStart]);

  const getCellSchedules = (roomId: string, dateKey: string, slotId: number | null) => {
    if (slotId === null) return [];

    const daySchedules = getSchedulesForSlot(dateKey, slotId);

    // Doctors/staff assigned directly to this room
    const directMatches = daySchedules.filter((s) => getScheduleRoomId(s) === roomId);
    const doctorsInRoom = directMatches.filter((schedule) => schedule.role !== 'NURSE');
    const supportingNurses = doctorsInRoom.flatMap((doctorSchedule) =>
      getLinkedNurseSchedulesForDoctor(doctorSchedule, daySchedules)
    );

    return [...directMatches, ...supportingNurses].filter(
      (schedule, index, allSchedules) =>
        allSchedules.findIndex((candidate) => candidate.scheduleId === schedule.scheduleId) === index
    );
  };

  const closeDutyModal = useCallback(() => {
    setModalOpen(false);
    setEditingSchedule(null);
  }, []);

  const openCreateModal = () => {
    setModalTitle('Xếp ca trực mới');
    setEditingSchedule(null);
    setDutyForm((prev) => ({ ...prev, doctorId: '', nurseId: '' }));
    setModalOpen(true);
  };

  const openEditModal = (row: AssignmentRow) => {
    setModalTitle('Chi tiết phân công');
    setEditingSchedule(null);
    setDutyForm((prev) => ({
      ...prev,
      doctorId: row.doctorId,
      nurseId: row.nurseId,
      roomId: doctorRoomByUserId[row.doctorId] || prev.roomId,
    }));
    setModalOpen(true);
  };

  const openExistingScheduleModal = useCallback(
    (
      doctorSchedule: StaffScheduleResponse,
      linkedNurseSchedule: StaffScheduleResponse | undefined,
      roomId: string
    ) => {
      setModalTitle('Cập nhật lịch trực');
      setEditingSchedule({
        doctorScheduleId: doctorSchedule.scheduleId,
        nurseScheduleId: linkedNurseSchedule?.scheduleId ?? null,
        originalDoctorId: doctorSchedule.userId,
        originalNurseId: linkedNurseSchedule?.userId ?? '',
        originalDate: doctorSchedule.workDate,
        originalSlotId: doctorSchedule.slotId,
        originalRoomId: roomId,
      });
      setDutyForm({
        dutyDate: doctorSchedule.workDate,
        slotId: doctorSchedule.slotId,
        roomId,
        doctorId: doctorSchedule.userId,
        nurseId: linkedNurseSchedule?.userId ?? '',
      });
      setModalOpen(true);
    },
    []
  );

  const handleDeleteSchedule = useCallback(
    async (schedule: StaffScheduleResponse, roomId?: string) => {
      const daySchedules = getSchedulesForSlot(schedule.workDate, schedule.slotId);
      const doctorSchedule =
        schedule.role === 'NURSE'
          ? getLinkedDoctorScheduleForNurse(schedule, daySchedules, roomId)
          : schedule;
      const linkedNurseSchedules =
        schedule.role === 'NURSE' || !doctorSchedule
          ? []
          : getLinkedNurseSchedulesForDoctor(doctorSchedule, daySchedules);
      const isDoctorSchedule = schedule.role !== 'NURSE';
      const confirmMessage = isDoctorSchedule
        ? linkedNurseSchedules.length > 0
          ? `Gỡ bác sĩ ${schedule.fullName} và ${linkedNurseSchedules.length} y tá hỗ trợ khỏi lịch này?`
          : `Gỡ bác sĩ ${schedule.fullName} khỏi lịch này?`
        : `Gỡ y tá ${schedule.fullName} khỏi lịch này?`;

      if (!window.confirm(confirmMessage)) {
        return;
      }

      setSaving(true);
      try {
        const removedScheduleIds = [
          schedule.scheduleId,
          ...linkedNurseSchedules.map((nurseSchedule) => nurseSchedule.scheduleId),
        ];
        if (linkedNurseSchedules.length > 0) {
          for (const nurseSchedule of linkedNurseSchedules) {
            await staffScheduleService.deleteSchedule(nurseSchedule.scheduleId);
          }
        }

        await staffScheduleService.deleteSchedule(schedule.scheduleId);
        setSchedulesByDate((prev) => ({
          ...prev,
          [schedule.workDate]: (prev[schedule.workDate] || []).filter(
            (item) => !removedScheduleIds.includes(item.scheduleId)
          ),
        }));
        await syncMultipleScheduleDates([schedule.workDate]);
        setEditingSchedule(null);
        const deletedRoomId = roomId || getScheduleRoomId(schedule);
        setDutyForm((prev) =>
          prev.dutyDate === schedule.workDate &&
          prev.slotId === schedule.slotId &&
          (!deletedRoomId || prev.roomId === deletedRoomId)
            ? { ...prev, doctorId: '', nurseId: '' }
            : prev
        );
        window.alert('Đã gỡ nhân sự khỏi lịch.');
      } catch (error) {
        await syncMultipleScheduleDates([schedule.workDate]);
        window.alert(parseErrorMessage(error));
      } finally {
        setSaving(false);
      }
    },
    [
      getLinkedDoctorScheduleForNurse,
      getLinkedNurseSchedulesForDoctor,
      getScheduleRoomId,
      getSchedulesForSlot,
      syncMultipleScheduleDates,
    ]
  );

  const handleDeleteRow = (rowId: number) => {
    setAssignments((prev) => prev.filter((row) => row.id !== rowId));
  };

  const handleSaveDuty = async () => {
    const slotId = typeof dutyForm.slotId === 'number' ? dutyForm.slotId : null;
    if (!slotId) {
      window.alert('Vui lòng chọn ca làm việc hợp lệ.');
      return;
    }

    if (!dutyForm.doctorId) {
      window.alert('Vui lòng chọn bác sĩ.');
      return;
    }

    if (!dutyForm.roomId) {
      window.alert('Vui lòng chọn phòng.');
      return;
    }

    const selectedDoctor = doctors.find((item) => item.userId === dutyForm.doctorId);
    if (!selectedDoctor) {
      window.alert('Không tìm thấy bác sĩ đã chọn. Vui lòng thử lại.');
      return;
    }

    if (
      selectedRoom?.deptId != null &&
      doctorDeptIdByUserId[selectedDoctor.userId] !== selectedRoom.deptId
    ) {
      window.alert('Bác sĩ chỉ được xếp vào phòng thuộc đúng khoa của mình.');
      return;
    }

    if (!isDoctorEligibleByRoomRule(selectedDoctor, selectedRoom)) {
      window.alert(
        isGeneralRoom(selectedRoom)
          ? 'Phòng GENERAL chỉ cho phép chọn bác sĩ lâm sàng.'
          : 'Phòng không GENERAL chỉ cho phép chọn bác sĩ cận lâm sàng.'
      );
      return;
    }

    const doctor = selectedDoctor;
    const roomName = roomNameById[dutyForm.roomId];
    let reusableNurseSchedule: StaffScheduleResponse | undefined;
    const ignoredScheduleIds = editingSchedule
      ? [
          editingSchedule.doctorScheduleId,
          ...(editingSchedule.nurseScheduleId ? [editingSchedule.nurseScheduleId] : []),
        ]
      : [];
    const datesToSync = editingSchedule
      ? [editingSchedule.originalDate, dutyForm.dutyDate]
      : [dutyForm.dutyDate];

    setSaving(true);

    try {
      const latestDaySchedules = await syncSchedulesForDate(dutyForm.dutyDate);
      const roomDoctorConflict = findRoomRoleConflictInSchedules(
        latestDaySchedules,
        dutyForm.roomId,
        slotId,
        dutyForm.dutyDate,
        'DOCTOR',
        ignoredScheduleIds
      );
      if (roomDoctorConflict) {
        window.alert(
          `Phòng "${roomName || dutyForm.roomId}" đã có bác sĩ ${roomDoctorConflict.fullName} trong ca "${roomDoctorConflict.shiftName}" ngày ${dutyForm.dutyDate}. Hãy bấm vào lịch hiện có để sửa hoặc gỡ bác sĩ đó trước.`
        );
        return;
      }

      const doctorConflict = findConflictInSchedules(
        latestDaySchedules,
        Number(dutyForm.doctorId),
        slotId,
        dutyForm.dutyDate,
        ignoredScheduleIds
      );
      if (doctorConflict) {
        window.alert(
          `Bác sĩ ${doctor.fullName} đã được gán vào ca "${doctorConflict.shiftName}" ngày ${dutyForm.dutyDate}, hiện đang ở ${describeScheduleLocation(doctorConflict)}. Muốn chuyển phòng thì hãy mở lịch hiện có để cập nhật.`
        );
        return;
      }

      if (dutyForm.nurseId) {
        const roomNurseConflict = findRoomRoleConflictInSchedules(
          latestDaySchedules,
          dutyForm.roomId,
          slotId,
          dutyForm.dutyDate,
          'NURSE',
          ignoredScheduleIds
        );
        if (roomNurseConflict) {
          window.alert(
            `Phòng "${roomName || dutyForm.roomId}" đã có y tá ${roomNurseConflict.fullName} trong ca "${roomNurseConflict.shiftName}" ngày ${dutyForm.dutyDate}. Hãy bấm vào lịch hiện có để sửa hoặc gỡ y tá đó trước.`
          );
          return;
        }

        const nurseConflict = findConflictInSchedules(
          latestDaySchedules,
          Number(dutyForm.nurseId),
          slotId,
          dutyForm.dutyDate,
          ignoredScheduleIds
        );
        if (nurseConflict) {
          if (canReuseNurseSchedule(nurseConflict)) {
            reusableNurseSchedule = nurseConflict;
          } else {
            const nurse = nurses.find((n) => n.userId === dutyForm.nurseId);
            window.alert(
              `Y tá ${nurse?.fullName || ''} đã được gán vào ca "${nurseConflict.shiftName}" ngày ${dutyForm.dutyDate} rồi, hiện đang ở ${describeScheduleLocation(nurseConflict)}.`
            );
            return;
          }
        }
      }

      if (editingSchedule) {
        const doctorChanged = editingSchedule.originalDoctorId !== Number(dutyForm.doctorId);
        const hadLinkedNurse = editingSchedule.nurseScheduleId != null;
        const nextNurseId = dutyForm.nurseId ? Number(dutyForm.nurseId) : null;
        const previousNurseId =
          typeof editingSchedule.originalNurseId === 'number'
            ? editingSchedule.originalNurseId
            : null;

        if (doctorChanged) {
          await staffScheduleService.updateSchedule(editingSchedule.doctorScheduleId, {
            userId: Number(dutyForm.doctorId),
            slotId,
            workDate: dutyForm.dutyDate,
            roomId: dutyForm.roomId,
            note: roomName ? `Phòng: ${roomName}` : undefined,
          });
        } else {
          await staffScheduleService.updateSchedule(editingSchedule.doctorScheduleId, {
            slotId,
            workDate: dutyForm.dutyDate,
            roomId: dutyForm.roomId,
            note: roomName ? `Phòng: ${roomName}` : undefined,
          });
        }

        if (hadLinkedNurse && !nextNurseId && editingSchedule.nurseScheduleId) {
          await staffScheduleService.deleteSchedule(editingSchedule.nurseScheduleId);
        } else if (!hadLinkedNurse && nextNurseId) {
          if (reusableNurseSchedule) {
            await staffScheduleService.updateSchedule(reusableNurseSchedule.scheduleId, {
              slotId,
              workDate: dutyForm.dutyDate,
              roomId: dutyForm.roomId,
              note: `Hỗ trợ bác sĩ ${doctor.fullName}`,
            });
          } else {
            await staffScheduleService.createSchedule({
              userId: nextNurseId,
              slotId,
              workDate: dutyForm.dutyDate,
              roomId: dutyForm.roomId,
              note: `Hỗ trợ bác sĩ ${doctor.fullName}`,
            });
          }
        } else if (hadLinkedNurse && nextNurseId && editingSchedule.nurseScheduleId) {
          if (previousNurseId === nextNurseId) {
            await staffScheduleService.updateSchedule(editingSchedule.nurseScheduleId, {
              slotId,
              workDate: dutyForm.dutyDate,
              roomId: dutyForm.roomId,
              note: `Hỗ trợ bác sĩ ${doctor.fullName}`,
            });
          } else {
            if (reusableNurseSchedule) {
              await staffScheduleService.updateSchedule(reusableNurseSchedule.scheduleId, {
                slotId,
                workDate: dutyForm.dutyDate,
                roomId: dutyForm.roomId,
                note: `Hỗ trợ bác sĩ ${doctor.fullName}`,
              });
            } else {
              await staffScheduleService.createSchedule({
                userId: nextNurseId,
                slotId,
                workDate: dutyForm.dutyDate,
                roomId: dutyForm.roomId,
                note: `Hỗ trợ bác sĩ ${doctor.fullName}`,
              });
            }
            await staffScheduleService.deleteSchedule(editingSchedule.nurseScheduleId);
          }
        }

        await syncMultipleScheduleDates(datesToSync);
        closeDutyModal();
        window.alert('Đã cập nhật lịch trực thành công.');
        return;
      }

      let doctorScheduleCreated = false;

      await staffScheduleService.createSchedule({
        userId: Number(dutyForm.doctorId),
        slotId,
        workDate: dutyForm.dutyDate,
        roomId: dutyForm.roomId || undefined,
        note: roomName ? `Phòng: ${roomName}` : undefined,
      });
      doctorScheduleCreated = true;

      if (dutyForm.nurseId) {
        try {
          if (reusableNurseSchedule) {
            await staffScheduleService.updateSchedule(reusableNurseSchedule.scheduleId, {
              slotId,
              workDate: dutyForm.dutyDate,
              roomId: dutyForm.roomId,
              note: doctor ? `Hỗ trợ bác sĩ ${doctor.fullName}` : undefined,
            });
          } else {
            await staffScheduleService.createSchedule({
              userId: Number(dutyForm.nurseId),
              slotId,
              workDate: dutyForm.dutyDate,
              roomId: dutyForm.roomId,
              note: doctor ? `Hỗ trợ bác sĩ ${doctor.fullName}` : undefined,
            });
          }
        } catch (error) {
          await syncMultipleScheduleDates(datesToSync);
          if (doctorScheduleCreated) {
            closeDutyModal();
            window.alert(
              `Đã lưu lịch cho bác sĩ ${doctor.fullName}, nhưng không thể gán y tá: ${parseErrorMessage(error)}`
            );
            return;
          }
          throw error;
        }
      }

      await syncMultipleScheduleDates(datesToSync);
      closeDutyModal();
      window.alert('Đã lưu lịch trực thành công.');
    } catch (error) {
      await syncMultipleScheduleDates(datesToSync);
      window.alert(parseErrorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  const handleSaveAssignments = async () => {
    const slotId = typeof dutyForm.slotId === 'number' ? dutyForm.slotId : null;
    if (!slotId) {
      window.alert('Vui lòng chọn ca làm việc hợp lệ trước khi lưu.');
      return;
    }

    if (assignments.length === 0) {
      window.alert('Không có bác sĩ nào trong danh sách phân công để lưu.');
      return;
    }

    setSaving(true);

    let latestDaySchedules = await syncSchedulesForDate(dutyForm.dutyDate);

    let successCount = 0;
    const failures: string[] = [];

    for (const row of assignments) {
      let reusableRowNurseSchedule: StaffScheduleResponse | undefined;
      const doctorOfRow = doctors.find((item) => item.userId === row.doctorId);
      if (!doctorOfRow) {
        failures.push(`${row.doctorName}: không tìm thấy thông tin bác sĩ.`);
        continue;
      }

      if (!isDoctorEligibleByRoomRule(doctorOfRow, selectedRoom)) {
        failures.push(
          isGeneralRoom(selectedRoom)
            ? `${row.doctorName}: chỉ bác sĩ lâm sàng mới được xếp phòng GENERAL.`
            : `${row.doctorName}: phòng này chỉ nhận bác sĩ cận lâm sàng.`
        );
        continue;
      }

      const doctorConflict = findConflictInSchedules(
        latestDaySchedules,
        row.doctorId,
        slotId,
        dutyForm.dutyDate
      );
      if (doctorConflict) {
        failures.push(
          `${row.doctorName}: đã được gán vào ca "${doctorConflict.shiftName}" ngày ${dutyForm.dutyDate}, hiện đang ở ${describeScheduleLocation(doctorConflict)}.`
        );
        continue;
      }

      if (row.nurseId) {
        const nurseConflict = findConflictInSchedules(
          latestDaySchedules,
          Number(row.nurseId),
          slotId,
          dutyForm.dutyDate
        );
        if (nurseConflict) {
          if (canReuseNurseSchedule(nurseConflict)) {
            reusableRowNurseSchedule = nurseConflict;
          } else {
            failures.push(
              `Y tá của ${row.doctorName}: đã được gán vào ca "${nurseConflict.shiftName}" ngày ${dutyForm.dutyDate}, hiện đang ở ${describeScheduleLocation(nurseConflict)}.`
            );
            continue;
          }
        }
      }

      const doctorRoomId = doctorRoomByUserId[row.doctorId];
      const roomIdForDoctor = dutyForm.roomId || doctorRoomId;
      const rowRoomName = roomNameById[roomIdForDoctor] || roomIdForDoctor;
      const roomDoctorConflict = roomIdForDoctor
        ? findRoomRoleConflictInSchedules(
            latestDaySchedules,
            roomIdForDoctor,
            slotId,
            dutyForm.dutyDate,
            'DOCTOR'
          )
        : undefined;
      if (roomDoctorConflict) {
        failures.push(
          `${row.doctorName}: phòng "${rowRoomName}" đã có bác sĩ ${roomDoctorConflict.fullName} trong ca này.`
        );
        continue;
      }

      if (row.nurseId && roomIdForDoctor) {
        const roomNurseConflict = findRoomRoleConflictInSchedules(
          latestDaySchedules,
          roomIdForDoctor,
          slotId,
          dutyForm.dutyDate,
          'NURSE'
        );
        if (roomNurseConflict) {
          failures.push(
            `Y tá của ${row.doctorName}: phòng "${rowRoomName}" đã có y tá ${roomNurseConflict.fullName} trong ca này.`
          );
          continue;
        }
      }

      try {
        const docSch = await staffScheduleService.createSchedule({
          userId: row.doctorId,
          slotId,
          workDate: dutyForm.dutyDate,
          roomId: roomIdForDoctor || undefined,
          note: roomNameById[roomIdForDoctor] ? `Phòng: ${roomNameById[roomIdForDoctor]}` : undefined,
        });
        latestDaySchedules = [...latestDaySchedules, docSch];
        successCount += 1;
      } catch (error) {
        failures.push(`${row.doctorName}: ${parseErrorMessage(error)}`);
      }

      if (row.nurseId) {
        try {
          const nurseSch = reusableRowNurseSchedule
            ? await staffScheduleService.updateSchedule(reusableRowNurseSchedule.scheduleId, {
                slotId,
                workDate: dutyForm.dutyDate,
                roomId: roomIdForDoctor,
                note: `Hỗ trợ bác sĩ ${row.doctorName}`,
              })
            : await staffScheduleService.createSchedule({
                userId: Number(row.nurseId),
                slotId,
                workDate: dutyForm.dutyDate,
                roomId: roomIdForDoctor,
                note: `Hỗ trợ bác sĩ ${row.doctorName}`,
              });
          latestDaySchedules = latestDaySchedules
            .filter((schedule) => schedule.scheduleId !== nurseSch.scheduleId)
            .concat(nurseSch);
          successCount += 1;
        } catch (error) {
          failures.push(`Y tá của ${row.doctorName}: ${parseErrorMessage(error)}`);
        }
      }
    }

    await syncSchedulesForDate(dutyForm.dutyDate);

    setSaving(false);

    if (failures.length > 0) {
      const preview = failures.slice(0, 6).join('\n- ');
      window.alert(`Đã lưu ${successCount} lịch trực. Có ${failures.length} lỗi:\n- ${preview}`);
      return;
    }

    window.alert(`Đã lưu thành công ${successCount} lịch trực.`);
  };

  const getAvailableNursesForAssignmentRow = useCallback(
    (row: AssignmentRow) => {
      const selectedNurseIds = new Set(
        assignments
          .filter((item) => item.id !== row.id && item.nurseId)
          .map((item) => Number(item.nurseId))
      );

      return assignableNursesForDuty.filter(
        (nurse) => nurse.userId === row.nurseId || !selectedNurseIds.has(nurse.userId)
      );
    },
    [assignments, assignableNursesForDuty]
  );

  return (
    <div className="min-h-[calc(100vh-4rem)] p-6 text-slate-950">
      <div className="space-y-3 border border-slate-200 bg-white p-3">
        {isAssignmentMode ? (
          <>
            <div className="grid grid-cols-1 gap-2 md:grid-cols-3">
              <div className="border border-slate-200 bg-white p-2 text-[11px]">
                <label className="mb-1 block font-bold uppercase">Ngày trực</label>
                <input
                  type="date"
                  className="h-8 w-full border border-slate-200 px-2"
                  value={dutyForm.dutyDate}
                  onChange={(event) => setDutyForm((prev) => ({ ...prev, dutyDate: event.target.value }))}
                />
              </div>

              <div className="border border-slate-200 bg-white p-2 text-[11px]">
                <label className="mb-1 block font-bold uppercase">Ca làm việc</label>
                <select
                  className="h-8 w-full border border-slate-200 px-2"
                  value={dutyForm.slotId}
                  onChange={(event) =>
                    setDutyForm((prev) => ({ ...prev, slotId: Number(event.target.value) || '' }))
                  }
                >
                  <option value="">-- Chọn ca --</option>
                  {displayShifts
                    .filter((shift) => shift.slotId !== null)
                    .map((shift) => (
                      <option key={shift.slotId} value={shift.slotId ?? ''}>
                        {shift.label} ({shift.startTime} - {shift.endTime})
                      </option>
                    ))}
                </select>
              </div>

              <div className="border border-slate-200 bg-white p-2 text-[11px]">
                <label className="mb-1 block font-bold uppercase">Phòng</label>
                <select
                  className="h-8 w-full border border-slate-200 px-2"
                  value={dutyForm.roomId}
                  onChange={(event) =>
                    setDutyForm((prev) => ({ ...prev, roomId: event.target.value, doctorId: '' }))
                  }
                >
                  {rooms.filter((r) => !r.status || r.status === 'ACTIVE').map((room) => (
                    <option key={room.roomId} value={room.roomId}>
                      {room.roomName}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {(roomDoctorConflictMessage || roomNurseConflictMessage) && (
              <div className="border border-red-200 bg-red-50 px-3 py-2 text-xs font-semibold text-red-700">
                {roomDoctorConflictMessage || roomNurseConflictMessage}
              </div>
            )}

            <div className="grid grid-cols-1 gap-3 lg:grid-cols-[1fr_2fr]">
              <div className="border border-slate-200 bg-white">
                <div className="border-b border-slate-200 bg-blue-600 px-3 py-2 text-xs font-bold uppercase text-white">
                  Kho nhân sự
                </div>
                <div className="border-b border-slate-200 px-2 py-2">
                  <input
                    className="h-8 w-full border border-slate-200 px-2 text-xs"
                    placeholder="Tìm kiếm nhân sự..."
                    value={searchStock}
                    onChange={(event) => setSearchStock(event.target.value)}
                  />
                </div>
                <div className="max-h-[520px] overflow-auto p-2">
                  <table className="w-full text-xs">
                    <thead className="border-b border-slate-200">
                      <tr className="text-left uppercase">
                        <th className="w-8 py-2">STT</th>
                        <th className="py-2">Mã nhân viên</th>
                        <th className="py-2">Họ tên</th>
                        <th className="py-2">Nhóm</th>
                      </tr>
                    </thead>
                    <tbody>
                      {baseLoading ? (
                        <tr>
                          <td colSpan={4} className="py-4 text-center">
                            Đang tải...
                          </td>
                        </tr>
                      ) : (
                        filteredDoctorsStock.slice(0, 8).map((doctor, index) => (
                          <tr key={doctor.userId} className="border-b border-slate-200/10">
                            <td className="py-2">{index + 1}</td>
                            <td className="py-2">{formatStaffCode(doctor.staffId, index)}</td>
                            <td className="py-2">{doctor.fullName}</td>
                            <td className="py-2 font-semibold">{getDoctorCategoryText(doctor)}</td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              <div className="border border-slate-200 bg-white">
                <div className="border-b border-slate-200 px-3 py-2">
                  <p className="text-xs font-bold uppercase">Danh sách phân công</p>
                  <div className="mt-2 grid grid-cols-1 gap-2 md:grid-cols-[1fr_170px]">
                    <input
                      className="h-8 w-full border border-slate-200 px-2 text-xs"
                      placeholder="Tìm kiếm bác sĩ trong danh sách phân công..."
                      value={searchAssign}
                      onChange={(event) => setSearchAssign(event.target.value)}
                    />
                    <select
                      className="h-8 w-full border border-slate-200 px-2 text-xs"
                      value={assignmentStatusFilter}
                      onChange={(event) =>
                        setAssignmentStatusFilter(event.target.value as AssignmentStatusFilter)
                      }
                    >
                      <option value="ALL">Tất cả trạng thái</option>
                      <option value="ASSIGNED">Đã ghép y tá</option>
                      <option value="UNASSIGNED">Chưa ghép y tá</option>
                    </select>
                  </div>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full min-w-[700px] text-xs">
                    <thead className="bg-blue-600 text-white">
                      <tr className="uppercase">
                        <th className="w-12 px-2 py-2 text-left">STT</th>
                        <th className="px-2 py-2 text-left">Mã nhân viên</th>
                        <th className="px-2 py-2 text-left">Họ tên</th>
                        <th className="px-2 py-2 text-left">Phân công</th>
                        <th className="px-2 py-2 text-right">Thao tác</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredAssignments.length === 0 ? (
                        <tr>
                          <td colSpan={5} className="px-2 py-6 text-center">
                            Không có bác sĩ trong danh sách phân công.
                          </td>
                        </tr>
                      ) : (
                        filteredAssignments.map((row, index) => (
                          <tr key={row.id} className="border-t border-slate-200/20">
                            <td className="px-2 py-2">{index + 1}</td>
                            <td className="px-2 py-2">{row.doctorCode}</td>
                            <td className="px-2 py-2 font-semibold">{row.doctorName}</td>
                            <td className="px-2 py-2">
                              <select
                                className="h-8 w-full border border-slate-200 px-2"
                                value={row.nurseId}
                                onChange={(event) => {
                                  const nurseId = Number(event.target.value) || '';
                                  setAssignments((prev) =>
                                    prev.map((item) => (item.id === row.id ? { ...item, nurseId } : item))
                                  );
                                }}
                              >
                                <option value="">-- Chọn y tá --</option>
                                {getAvailableNursesForAssignmentRow(row).map((nurse) => (
                                  <option key={nurse.userId} value={nurse.userId}>
                                    {nurse.fullName}
                                  </option>
                                ))}
                              </select>
                            </td>
                            <td className="px-2 py-2">
                              <div className="flex justify-end gap-1">
                                <button
                                  onClick={() => openEditModal(row)}
                                  className="h-7 border border-slate-200 bg-white px-3 font-bold uppercase"
                                >
                                  Sửa
                                </button>
                                <button
                                  onClick={() => handleDeleteRow(row.id)}
                                  className="h-7 border border-slate-200 bg-blue-600 px-3 font-bold uppercase text-white"
                                >
                                  Xóa
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>

                <div className="flex justify-end border-t border-slate-200 p-3">
                  <button
                    onClick={handleSaveAssignments}
                    disabled={saving || assignments.length === 0}
                    className="h-9 border border-slate-200 bg-blue-600 px-6 text-xs font-bold uppercase text-white disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {saving ? 'Đang lưu...' : 'Lưu lịch trực'}
                  </button>
                </div>
              </div>
            </div>
          </>
        ) : (
          <>
            <div className="flex flex-wrap items-center gap-2">
              <button
                onClick={() => setWeekStart((prev) => addDays(prev, -7))}
                className="h-8 border border-slate-200 bg-white px-3 text-xs font-semibold"
              >
                {'<'}
              </button>

              <div className="h-8 border border-slate-200 bg-white px-3 text-xs font-semibold leading-8">
                {selectedWeekLabel}
              </div>

              <button
                onClick={() => setWeekStart((prev) => addDays(prev, 7))}
                className="h-8 border border-slate-200 bg-white px-3 text-xs font-semibold"
              >
                {'>'}
              </button>

              <select
                className="h-8 border border-slate-200 bg-white px-3 text-xs"
                value={selectedDeptId}
                onChange={(event) => {
                  const value = event.target.value;
                  setSelectedDeptId(value === 'ALL' ? 'ALL' : Number(value));
                }}
              >
                <option value="ALL">Tất cả khoa</option>
                {departmentOptions.map((dept) => (
                  <option key={dept.deptId} value={dept.deptId}>
                    {dept.deptName}
                  </option>
                ))}
              </select>

              <select
                className="h-8 border border-slate-200 bg-white px-3 text-xs"
                value={roomStatusFilter}
                onChange={(event) => setRoomStatusFilter(event.target.value as RoomStatusFilter)}
              >
                <option value="ALL">Tất cả trạng thái</option>
                <option value="HAS_SCHEDULE">Phòng đã có lịch</option>
                <option value="NO_SCHEDULE">Phòng đang trống</option>
              </select>

              <button
                onClick={openCreateModal}
                className="ml-auto h-8 border border-slate-200 bg-blue-600 px-3 text-xs font-bold uppercase text-white"
              >
                + Thêm nhân sự vào ca
              </button>
            </div>

            <div className="overflow-x-auto border border-slate-200 bg-white">
              <table className="w-full min-w-[980px] text-xs">
                <thead className="bg-slate-50">
                  <tr className="border-b border-slate-200 uppercase">
                    <th className="w-32 px-2 py-2 text-left">Phòng</th>
                    <th className="w-36 px-2 py-2 text-left">Ca</th>
                    {weekDates.map((day) => (
                      <th key={day.key} className="px-2 py-2 text-center">
                        {day.label}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {tableLoading ? (
                    <tr>
                      <td colSpan={tableColSpan} className="px-3 py-8 text-center text-sm">
                        Đang tải lịch làm việc...
                      </td>
                    </tr>
                  ) : statusFilteredRooms.length === 0 ? (
                    <tr>
                      <td colSpan={tableColSpan} className="px-3 py-8 text-center text-sm">
                        {visibleRooms.length === 0
                          ? 'Không có phòng để hiển thị.'
                          : 'Không có phòng phù hợp trạng thái lọc.'}
                      </td>
                    </tr>
                  ) : (
                    statusFilteredRooms.map((room) => {
                      const isRoomActive = !room.status || room.status === 'ACTIVE';
                      const roomStatusVi: Record<string, string> = {
                        ACTIVE: 'Đang hoạt động',
                        INACTIVE: 'Ngừng hoạt động',
                        MAINTENANCE: 'Đang bảo trì',
                        CLOSED: 'Đã đóng',
                      };
                      return displayShifts.map((shift, shiftIndex) => (
                        <tr key={`${room.roomId}-${shift.slotId ?? shiftIndex}`} className="border-t border-slate-200/20 align-top">
                          {shiftIndex === 0 ? (
                            <td rowSpan={2} className={`border-r border-slate-200 p-2 font-bold align-middle ${!isRoomActive ? 'bg-gray-200' : ''}`}>
                              {room.roomName}
                              {!isRoomActive && (
                                <div className="mt-1 text-[10px] font-bold uppercase text-red-600">
                                  {roomStatusVi[room.status!] || 'Trạng thái khác'}
                                </div>
                              )}
                              <div className="mt-1 text-[10px] font-semibold uppercase text-slate-500">
                                {roomHasScheduleById[room.roomId] ? 'Đã có lịch' : 'Đang trống'}
                              </div>
                            </td>
                          ) : null}

                          <td className="border-r border-slate-200 p-2 font-bold">
                            {shift.label}
                            <br />
                            <span className="font-semibold">
                              ({shift.startTime} - {shift.endTime})
                            </span>
                          </td>

                          {weekDates.map((day) => {
                            const cellSchedules = getCellSchedules(room.roomId, day.key, shift.slotId);
                            const daySchedules = getSchedulesForSlot(day.key, shift.slotId);
                            return (
                              <td
                                key={`${room.roomId}-${shift.slotId ?? shiftIndex}-${day.key}`}
                                className={`border-r border-slate-200/20 p-2 ${shiftIndex === 0 ? 'h-28' : 'h-20'} last:border-r-0 ${!isRoomActive ? 'bg-gray-100' : ''}`}
                              >
                                {cellSchedules.length > 0 ? (
                                  <div className="space-y-1">
                                    {cellSchedules.map((schedule) => {
                                      const linkedDoctorSchedule =
                                        schedule.role === 'NURSE'
                                          ? getLinkedDoctorScheduleForNurse(
                                              schedule,
                                              daySchedules,
                                              room.roomId
                                            )
                                          : schedule;
                                      const linkedNurseSchedule =
                                        schedule.role === 'NURSE'
                                          ? schedule
                                          : getLinkedNurseSchedulesForDoctor(schedule, daySchedules)[0];
                                      const canEdit = Boolean(linkedDoctorSchedule);

                                      return (
                                        <div
                                          key={schedule.scheduleId}
                                          role={canEdit ? 'button' : undefined}
                                          tabIndex={canEdit ? 0 : undefined}
                                          onClick={
                                            canEdit
                                              ? () =>
                                                  openExistingScheduleModal(
                                                    linkedDoctorSchedule!,
                                                    linkedNurseSchedule,
                                                    room.roomId
                                                  )
                                              : undefined
                                          }
                                          onKeyDown={
                                            canEdit
                                              ? (event) => {
                                                  if (event.key === 'Enter' || event.key === ' ') {
                                                    event.preventDefault();
                                                    openExistingScheduleModal(
                                                      linkedDoctorSchedule!,
                                                      linkedNurseSchedule,
                                                      room.roomId
                                                    );
                                                  }
                                                }
                                              : undefined
                                          }
                                          className={`relative px-2 py-1 pr-6 text-[10px] font-semibold ${
                                            schedule.role === 'NURSE'
                                              ? 'border border-dashed border-gray-500 bg-gray-50 text-gray-700'
                                              : 'border border-slate-200'
                                          } ${canEdit ? 'cursor-pointer hover:bg-white focus:outline-none focus:ring-1 focus:ring-blue-500' : ''}`}
                                          title={schedule.note || ''}
                                        >
                                          <div>
                                            {`${schedule.role === 'NURSE' ? 'YT.' : 'BS.'} ${(schedule.fullName || 'CHƯA CÓ TÊN').toUpperCase()}`}
                                          </div>
                                          <button
                                            type="button"
                                            aria-label={`Gỡ ${schedule.fullName}`}
                                            disabled={saving}
                                            onClick={(event) => {
                                              event.stopPropagation();
                                              handleDeleteSchedule(schedule, room.roomId);
                                            }}
                                                        className="absolute right-1 top-1 flex h-4 w-4 items-center justify-center border border-blue-600 bg-blue-600 text-[9px] font-bold leading-none text-white disabled:cursor-not-allowed disabled:opacity-60"
                                          >
                                            x
                                          </button>
                                        </div>
                                      );
                                    })}
                                  </div>
                                ) : null}
                              </td>
                            );
                          })}
                        </tr>
                      ));
                    })
                  )}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>

      <DutyModal
        isOpen={modalOpen}
        title={modalTitle}
        form={dutyForm}
        rooms={rooms.filter((r) => !r.status || r.status === 'ACTIVE')}
        shifts={displayShifts}
        doctors={modalDoctorOptions}
        nurses={modalNurseOptions}
        getDoctorCategory={getDoctorCategoryText}
        doctorUnavailableMessage={roomDoctorConflictMessage}
        nurseUnavailableMessage={roomNurseConflictMessage}
        saving={saving}
        saveDisabled={modalSaveDisabled}
        preserveStaffOnContextChange={Boolean(editingSchedule)}
        onClose={closeDutyModal}
        onChange={setDutyForm}
        onSave={handleSaveDuty}
        saveLabel={editingSchedule ? 'Lưu thay đổi' : 'Lưu lịch trực'}
      />
    </div>
  );
}

