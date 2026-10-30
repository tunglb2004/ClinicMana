import axiosClient from '@/lib/axios';

export interface RoomResponse {
  roomId: string;
  roomName: string;
  roomType?: string;
  location?: string;
  deptId: number | null;
  deptName?: string;
  doctorCount: number;
  status?: string;
  baseAvgTime?: number;
  currentQueueLength?: number;
}

export interface RoomDetailResponse {
  roomId: string;
  roomName: string;
  roomType?: string;
  location?: string;
  deptId: number | null;
  deptName?: string;
  doctors: unknown[];
  status?: string;
  baseAvgTime?: number;
  currentQueueLength?: number;
}

export interface CreateRoomRequest {
  roomId: string;
  roomName: string;
  roomType?: string;
  location?: string;
  deptId?: number;
  status?: string;
  baseAvgTime?: number;
}

export interface UpdateRoomRequest {
  roomName?: string;
  roomType?: string;
  location?: string;
  deptId?: number;
  status?: string;
  baseAvgTime?: number;
}

export interface RoomsListResponse {
  rooms: RoomResponse[];
  currentPage: number;
  totalItems: number;
  totalPages: number;
}

export interface ServiceRoomResponse {
  roomId: string;
  roomName: string;
  services: {
    serviceId: string;
    serviceName: string;
  }[];
}

export interface ServiceRoomRequest {
  serviceIds: string[];
  confirmReplaceAll?: boolean;
}

const detailCache = new Map<string, RoomDetailResponse>();

export const roomService = {
  getList: async (
    deptId?: number | null,
    keyword?: string,
    page = 0,
    size = 100,
    includeInactive = false
  ): Promise<RoomsListResponse> => {
    const params = new URLSearchParams();
    if (deptId != null) params.append('deptId', String(deptId));
    if (keyword?.trim()) params.append('keyword', keyword.trim());
    params.append('page', String(page));
    params.append('size', String(size));
    if (includeInactive) params.append('includeInactive', 'true');
    const res = await axiosClient.get<RoomsListResponse>(
      `/admin/rooms/list?${params.toString()}`
    );
    return res.data;
  },

  getDetail: async (roomId: string): Promise<RoomDetailResponse> => {
    const cached = detailCache.get(roomId);
    if (cached) return cached;
    const res = await axiosClient.get<RoomDetailResponse>(
      `/admin/rooms/detail/${encodeURIComponent(roomId)}`
    );
    detailCache.set(roomId, res.data);
    return res.data;
  },

  create: async (data: CreateRoomRequest): Promise<RoomResponse> => {
    const res = await axiosClient.post<RoomResponse>('/admin/rooms/create', data);
    return res.data;
  },

  update: async (
    roomId: string,
    data: UpdateRoomRequest
  ): Promise<RoomResponse> => {
    const res = await axiosClient.put<RoomResponse>(
      `/admin/rooms/update/${encodeURIComponent(roomId)}`,
      data
    );
    detailCache.delete(roomId);
    return res.data;
  },

  updateStatus: async (roomId: string, status: 'ACTIVE' | 'INACTIVE'): Promise<RoomResponse> => {
    const res = await axiosClient.patch<RoomResponse>(
      `/admin/rooms/${encodeURIComponent(roomId)}/status`,
      { status }
    );
    detailCache.delete(roomId);
    return res.data;
  },

  delete: async (roomId: string): Promise<void> => {
    await axiosClient.delete(`/admin/rooms/delete/${encodeURIComponent(roomId)}`);
    detailCache.delete(roomId);
  },

  getServicesByRoom: async (roomId: string): Promise<ServiceRoomResponse> => {
    const res = await axiosClient.get<ServiceRoomResponse>(
      `/admin/service-rooms/by-room/${encodeURIComponent(roomId)}`
    );
    return res.data;
  },

  assignServicesToRoom: async (
    roomId: string,
    data: ServiceRoomRequest
  ): Promise<ServiceRoomResponse> => {
    const res = await axiosClient.put<ServiceRoomResponse>(
      `/admin/service-rooms/assign/${encodeURIComponent(roomId)}`,
      data
    );
    return res.data;
  },
};
