'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Building2,
  DoorOpen,
  Edit,
  Loader2,
  Lock,
  Plus,
  Search,
  Trash2,
  Unlock,
} from 'lucide-react';
import { departmentService, type DepartmentResponse } from '@/services/department.service';
import { roomService, type RoomResponse } from '@/services/room.service';
import { ErrorCode, ERROR_CODE_MESSAGES } from '@/types/api.types';
import { useToast } from '@/hooks/use-toast';
import { useDebounce } from '@/hooks/useDebounce';
import { DepartmentModal } from './DepartmentModal';
import { DeleteDepartmentModal } from './DeleteDepartmentModal';
import { RoomModal } from './RoomModal';
import { DeleteRoomModal } from './DeleteRoomModal';

const ALL_DEPARTMENTS_ID = '__ALL__';
const SEARCH_DEBOUNCE_MS = 400;

function StatusBadge({ active, activeLabel, inactiveLabel }: {
  active: boolean;
  activeLabel: string;
  inactiveLabel: string;
}) {
  return active ? (
    <span className="inline-flex items-center rounded-full bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-700">
      <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-blue-600" />
      {activeLabel}
    </span>
  ) : (
    <span className="inline-flex items-center rounded-full bg-blue-100 px-2.5 py-1 text-xs font-semibold text-blue-800">
      <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-blue-600" />
      {inactiveLabel}
    </span>
  );
}

export default function ClinicStructurePage() {
  const { toast } = useToast();
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [rooms, setRooms] = useState<RoomResponse[]>([]);
  const [loadingDepartments, setLoadingDepartments] = useState(false);
  const [loadingRooms, setLoadingRooms] = useState(false);
  const [processingDeptId, setProcessingDeptId] = useState<number | null>(null);
  const [processingRoomId, setProcessingRoomId] = useState<string | null>(null);

  const [selectedDeptId, setSelectedDeptId] = useState<string>(ALL_DEPARTMENTS_ID);
  const [deptSearch, setDeptSearch] = useState('');
  const [roomSearch, setRoomSearch] = useState('');
  const debouncedDeptSearch = useDebounce(deptSearch, SEARCH_DEBOUNCE_MS);
  const debouncedRoomSearch = useDebounce(roomSearch, SEARCH_DEBOUNCE_MS);

  const [isDeptModalOpen, setIsDeptModalOpen] = useState(false);
  const [editDeptId, setEditDeptId] = useState<number | null>(null);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deleteDept, setDeleteDept] = useState<DepartmentResponse | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  const [isRoomModalOpen, setIsRoomModalOpen] = useState(false);
  const [editRoomId, setEditRoomId] = useState<string | null>(null);
  const [isDeleteRoomModalOpen, setIsDeleteRoomModalOpen] = useState(false);
  const [deleteRoom, setDeleteRoom] = useState<RoomResponse | null>(null);
  const [deleteRoomLoading, setDeleteRoomLoading] = useState(false);

  const selectedDeptIdNum = selectedDeptId === ALL_DEPARTMENTS_ID ? null : Number(selectedDeptId);
  const selectedDepartment = useMemo(
    () => departments.find((department) => department.deptId === selectedDeptIdNum) ?? null,
    [departments, selectedDeptIdNum]
  );

  const fetchDepartments = useCallback(async () => {
    setLoadingDepartments(true);
    try {
      const response = await departmentService.getList(
        debouncedDeptSearch.trim() || undefined,
        0,
        100,
        true
      );
      setDepartments(response.departments ?? []);
    } catch (error) {
      console.error('Failed to fetch departments:', error);
      setDepartments([]);
    } finally {
      setLoadingDepartments(false);
    }
  }, [debouncedDeptSearch]);

  const fetchRooms = useCallback(async () => {
    setLoadingRooms(true);
    try {
      const response = await roomService.getList(
        selectedDeptIdNum,
        debouncedRoomSearch.trim() || undefined,
        0,
        100,
        true
      );
      setRooms(response.rooms ?? []);
    } catch (error) {
      console.error('Failed to fetch rooms:', error);
      setRooms([]);
    } finally {
      setLoadingRooms(false);
    }
  }, [debouncedRoomSearch, selectedDeptIdNum]);

  useEffect(() => {
    void fetchDepartments();
  }, [fetchDepartments]);

  useEffect(() => {
    void fetchRooms();
  }, [fetchRooms]);

  useEffect(() => {
    if (selectedDeptId === ALL_DEPARTMENTS_ID) {
      return;
    }

    const stillExists = departments.some((department) => String(department.deptId) === selectedDeptId);
    if (!stillExists) {
      setSelectedDeptId(ALL_DEPARTMENTS_ID);
    }
  }, [departments, selectedDeptId]);

  const canAddDepartment = selectedDeptId === ALL_DEPARTMENTS_ID;
  const canAddRoom = !!selectedDepartment && selectedDepartment.isActive;

  const refreshClinicStructure = async () => {
    await Promise.all([fetchDepartments(), fetchRooms()]);
  };

  const handleDepartmentSuccess = async () => {
    await refreshClinicStructure();
  };

  const handleRoomSuccess = async () => {
    await fetchRooms();
  };

  const handleToggleDepartmentStatus = async (department: DepartmentResponse, nextIsActive: boolean) => {
    setProcessingDeptId(department.deptId);
    try {
      await departmentService.updateStatus(department.deptId, nextIsActive);
      toast({
        description: nextIsActive ? 'Đã mở khóa khoa thành công.' : 'Đã khóa khoa thành công.',
        variant: 'success',
      });
      await refreshClinicStructure();
    } catch (error: any) {
      console.error(error);
      toast({
        description: error?.response?.data?.message || 'Không thể cập nhật trạng thái khoa.',
        variant: 'error',
      });
    } finally {
      setProcessingDeptId(null);
    }
  };

  const handleToggleRoomStatus = async (room: RoomResponse, nextStatus: 'ACTIVE' | 'INACTIVE') => {
    setProcessingRoomId(room.roomId);
    try {
      await roomService.updateStatus(room.roomId, nextStatus);
      toast({
        description: nextStatus === 'ACTIVE' ? 'Đã mở khóa phòng thành công.' : 'Đã khóa phòng thành công.',
        variant: 'success',
      });
      await fetchRooms();
    } catch (error: any) {
      console.error(error);
      toast({
        description: error?.response?.data?.message || 'Không thể cập nhật trạng thái phòng.',
        variant: 'error',
      });
    } finally {
      setProcessingRoomId(null);
    }
  };

  const handleConfirmDeleteDepartment = async () => {
    if (!deleteDept) return;
    setDeleteLoading(true);
    try {
      await departmentService.delete(deleteDept.deptId);
      if (selectedDeptId === String(deleteDept.deptId)) {
        setSelectedDeptId(ALL_DEPARTMENTS_ID);
      }
      setIsDeleteModalOpen(false);
      setDeleteDept(null);
      toast({
        description: ERROR_CODE_MESSAGES[ErrorCode.DELETE_DEPT_SUCCESS] ?? 'Xóa mềm khoa thành công.',
        variant: 'success',
      });
      await refreshClinicStructure();
    } catch (error: any) {
      console.error(error);
      toast({
        description: error?.response?.data?.message || 'Không thể xóa khoa.',
        variant: 'error',
      });
    } finally {
      setDeleteLoading(false);
    }
  };

  const handleConfirmDeleteRoom = async () => {
    if (!deleteRoom) return;
    setDeleteRoomLoading(true);
    try {
      await roomService.delete(deleteRoom.roomId);
      setIsDeleteRoomModalOpen(false);
      setDeleteRoom(null);
      toast({ description: 'Xóa mềm phòng thành công.', variant: 'success' });
      await fetchRooms();
    } catch (error: any) {
      console.error(error);
      toast({
        description: error?.response?.data?.message || 'Không thể xóa phòng.',
        variant: 'error',
      });
    } finally {
      setDeleteRoomLoading(false);
    }
  };

  return (
    <div className="ls-page min-h-full rounded-[var(--ls-radius-lg)] p-6">
      <div className="mx-auto max-w-7xl space-y-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="space-y-1">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-blue-50 text-[var(--ls-primary)]">
                <Building2 className="h-6 w-6" />
              </div>
              <div>
                <h1 className="ls-page-title">Cơ cấu phòng khám</h1>
                <p className="ls-text-muted ls-text-muted-14">
                  Khoa và phòng đã xóa mềm sẽ biến mất khỏi danh sách. Bản khóa vẫn được giữ lại để mở lại sau này.
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.08fr)_minmax(0,1fr)]">
          <div className="page-card !p-0 overflow-hidden">
            <div className="flex items-center justify-between border-b border-[var(--ls-border)] bg-[var(--ls-bg-table-header)] px-6 py-4">
              <div>
                <div className="ls-section-title flex items-center gap-2">
                  <Building2 className="h-5 w-5 text-[var(--ls-primary)]" />
                  Danh sách khoa
                </div>
                <p className="ls-text-muted ls-text-muted-12 mt-1">
                  Chọn một khoa để lọc phòng. Khoa đang khóa vẫn hiện để có thể mở lại.
                </p>
              </div>

              <button
                type="button"
                disabled={!canAddDepartment}
                onClick={() => {
                  setEditDeptId(null);
                  setIsDeptModalOpen(true);
                }}
                className="btn btn-primary gap-2"
              >
                <Plus className="h-4 w-4" />
                Thêm khoa
              </button>
            </div>

            <div className="space-y-4 border-b border-[var(--ls-border)] p-4">
              <div className="form-control-with-icon">
                <Search className="form-control-leading-icon" />
                <input
                  type="text"
                  placeholder="Tìm theo tên khoa..."
                  value={deptSearch}
                  onChange={(event) => setDeptSearch(event.target.value)}
                  className="form-control"
                />
              </div>

              <button
                type="button"
                onClick={() => setSelectedDeptId(ALL_DEPARTMENTS_ID)}
                className={`w-full rounded-[var(--ls-radius-sm)] border px-4 py-3 text-left transition-colors ${
                  selectedDeptId === ALL_DEPARTMENTS_ID
                    ? 'border-[var(--ls-primary)] bg-blue-50 text-[var(--ls-primary)]'
                    : 'border-[var(--ls-border)] bg-white text-[var(--ls-title-page-color)] hover:bg-[var(--ls-bg-table-header)]'
                }`}
              >
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <div className="font-semibold">Tất cả khoa</div>
                    <div className="ls-text-muted ls-text-muted-12">
                      {departments.length} khoa chưa bị xóa mềm
                    </div>
                  </div>
                </div>
              </button>
            </div>

            <div className="overflow-x-auto">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Khoa</th>
                    <th>Loại</th>
                    <th className="text-center">Trạng thái</th>
                    <th className="text-right !w-[168px]">Xử lý</th>
                  </tr>
                </thead>
                <tbody>
                  {loadingDepartments ? (
                    <tr>
                      <td colSpan={4} className="py-14 text-center text-[var(--ls-text-subtle-color)]">
                        <div className="flex items-center justify-center gap-2">
                          <Loader2 className="h-5 w-5 animate-spin text-[var(--ls-primary)]" />
                          Đang tải dữ liệu...
                        </div>
                      </td>
                    </tr>
                  ) : departments.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="py-14 text-center text-[var(--ls-text-subtle-color)]">
                        Không có khoa phù hợp.
                      </td>
                    </tr>
                  ) : (
                    departments.map((department) => {
                      const isSelected = String(department.deptId) === selectedDeptId;
                      const isProcessing = processingDeptId === department.deptId;

                      return (
                        <tr
                          key={department.deptId}
                          className={isSelected ? 'bg-blue-50/70' : ''}
                          onClick={() => setSelectedDeptId(String(department.deptId))}
                        >
                          <td className="cursor-pointer">
                            <div className="space-y-1">
                              <div className="font-medium text-[var(--ls-title-page-color)]">{department.deptName}</div>
                              <div className="ls-text-muted ls-text-muted-12">
                                {department.doctorCount} BS, {department.nurseCount} y tá
                              </div>
                            </div>
                          </td>
                          <td>{department.deptType}</td>
                          <td className="text-center">
                            <StatusBadge
                              active={department.isActive}
                              activeLabel="Đang hoạt động"
                              inactiveLabel="Đang khóa"
                            />
                          </td>
                          <td>
                            <div className="flex justify-end gap-2" onClick={(event) => event.stopPropagation()}>
                              <button
                                type="button"
                                className="btn btn-secondary !h-9 !w-9 !px-0 text-[var(--ls-primary)]"
                                title="Chỉnh sửa khoa"
                                onClick={() => {
                                  setEditDeptId(department.deptId);
                                  setIsDeptModalOpen(true);
                                }}
                              >
                                <Edit className="h-4 w-4" />
                              </button>
                              <button
                                type="button"
                                disabled={isProcessing}
                                className={`btn btn-secondary !h-9 !w-9 !px-0 ${
                                  department.isActive ? 'text-blue-700' : 'text-[var(--ls-primary)]'
                                }`}
                                title={department.isActive ? 'Khóa khoa' : 'Mở khóa'}
                                onClick={() => handleToggleDepartmentStatus(department, !department.isActive)}
                              >
                                {isProcessing ? (
                                  <Loader2 className="h-4 w-4 animate-spin" />
                                ) : department.isActive ? (
                                  <Lock className="h-4 w-4" />
                                ) : (
                                  <Unlock className="h-4 w-4" />
                                )}
                              </button>
                              <button
                                type="button"
                                className="btn btn-danger-soft !h-9 !w-9 !px-0"
                                title="Xóa mềm khoa"
                                onClick={() => {
                                  setDeleteDept(department);
                                  setIsDeleteModalOpen(true);
                                }}
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div className="page-card !p-0 overflow-hidden">
            <div className="flex items-center justify-between border-b border-[var(--ls-border)] bg-[var(--ls-bg-table-header)] px-6 py-4">
              <div>
                <div className="ls-section-title flex items-center gap-2">
                  <DoorOpen className="h-5 w-5 text-[var(--ls-primary)]" />
                  Danh sách phòng
                </div>
                <p className="ls-text-muted ls-text-muted-12 mt-1">
                  {selectedDepartment
                    ? `Đang lọc theo khoa: ${selectedDepartment.deptName}`
                    : 'Đang hiển thị tất cả phòng chưa bị xóa mềm.'}
                </p>
              </div>

              <button
                type="button"
                disabled={!canAddRoom}
                onClick={() => {
                  setEditRoomId(null);
                  setIsRoomModalOpen(true);
                }}
                className="btn btn-primary gap-2"
              >
                <Plus className="h-4 w-4" />
                Thêm phòng
              </button>
            </div>

            <div className="space-y-3 border-b border-[var(--ls-border)] p-4">
              <div className="form-control-with-icon">
                <Search className="form-control-leading-icon" />
                <input
                  type="text"
                  placeholder="Tìm theo mã hoặc tên phòng..."
                  value={roomSearch}
                  onChange={(event) => setRoomSearch(event.target.value)}
                  className="form-control"
                />
              </div>

              {selectedDepartment && !selectedDepartment.isActive && (
                <div className="rounded-[var(--ls-radius-sm)] border border-blue-200 bg-blue-50 px-3 py-2 text-sm text-blue-800">
                  Khoa đang khóa. Bạn vẫn xem, sửa, mở khóa hoặc xóa mềm các phòng cũ, nhưng không thể thêm phòng mới.
                </div>
              )}
            </div>

            <div className="overflow-x-auto">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Phòng</th>
                    <th>Thuộc khoa</th>
                    <th className="text-center">Trạng thái</th>
                    <th className="text-right !w-[168px]">Xử lý</th>
                  </tr>
                </thead>
                <tbody>
                  {loadingRooms ? (
                    <tr>
                      <td colSpan={4} className="py-14 text-center text-[var(--ls-text-subtle-color)]">
                        <div className="flex items-center justify-center gap-2">
                          <Loader2 className="h-5 w-5 animate-spin text-[var(--ls-primary)]" />
                          Đang tải dữ liệu...
                        </div>
                      </td>
                    </tr>
                  ) : rooms.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="py-14 text-center text-[var(--ls-text-subtle-color)]">
                        Không có phòng phù hợp.
                      </td>
                    </tr>
                  ) : (
                    rooms.map((room) => {
                      const isProcessing = processingRoomId === room.roomId;
                      const isActive = room.status === 'ACTIVE';

                      return (
                        <tr key={room.roomId}>
                          <td>
                            <div className="space-y-1">
                              <div className="font-medium text-[var(--ls-title-page-color)]">{room.roomName}</div>
                              <div className="ls-text-muted ls-text-muted-12">{room.roomId}</div>
                            </div>
                          </td>
                          <td>{room.deptName || 'Chưa gán khoa'}</td>
                          <td className="text-center">
                            <StatusBadge
                              active={isActive}
                              activeLabel="Đang hoạt động"
                              inactiveLabel="Đang khóa"
                            />
                          </td>
                          <td>
                            <div className="flex justify-end gap-2">
                              <button
                                type="button"
                                className="btn btn-secondary !h-9 !w-9 !px-0 text-[var(--ls-primary)]"
                                title="Chỉnh sửa phòng"
                                onClick={() => {
                                  setEditRoomId(room.roomId);
                                  setIsRoomModalOpen(true);
                                }}
                              >
                                <Edit className="h-4 w-4" />
                              </button>
                              <button
                                type="button"
                                disabled={isProcessing}
                                className={`btn btn-secondary !h-9 !w-9 !px-0 ${
                                  isActive ? 'text-blue-700' : 'text-[var(--ls-primary)]'
                                }`}
                                title={isActive ? 'Khóa phòng' : 'Mở khóa phòng'}
                                onClick={() => handleToggleRoomStatus(room, isActive ? 'INACTIVE' : 'ACTIVE')}
                              >
                                {isProcessing ? (
                                  <Loader2 className="h-4 w-4 animate-spin" />
                                ) : isActive ? (
                                  <Lock className="h-4 w-4" />
                                ) : (
                                  <Unlock className="h-4 w-4" />
                                )}
                              </button>
                              <button
                                type="button"
                                className="btn btn-danger-soft !h-9 !w-9 !px-0"
                                title="Xóa mềm phòng"
                                onClick={() => {
                                  setDeleteRoom(room);
                                  setIsDeleteRoomModalOpen(true);
                                }}
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <DepartmentModal
        isOpen={isDeptModalOpen}
        onClose={() => {
          setIsDeptModalOpen(false);
          setEditDeptId(null);
        }}
        onSuccess={() => {
          void handleDepartmentSuccess();
        }}
        editDeptId={editDeptId}
      />

      <DeleteDepartmentModal
        isOpen={isDeleteModalOpen}
        onClose={() => {
          setIsDeleteModalOpen(false);
          setDeleteDept(null);
        }}
        onConfirm={handleConfirmDeleteDepartment}
        deptName={deleteDept?.deptName ?? ''}
        loading={deleteLoading}
      />

      <RoomModal
        isOpen={isRoomModalOpen}
        onClose={() => {
          setIsRoomModalOpen(false);
          setEditRoomId(null);
        }}
        onSuccess={() => {
          void handleRoomSuccess();
        }}
        editRoomId={editRoomId}
        selectedDeptId={selectedDeptIdNum}
        selectedDeptName={selectedDepartment?.deptName ?? ''}
      />

      <DeleteRoomModal
        isOpen={isDeleteRoomModalOpen}
        onClose={() => {
          setIsDeleteRoomModalOpen(false);
          setDeleteRoom(null);
        }}
        onConfirm={handleConfirmDeleteRoom}
        roomName={deleteRoom?.roomName ?? ''}
        loading={deleteRoomLoading}
      />
    </div>
  );
}
