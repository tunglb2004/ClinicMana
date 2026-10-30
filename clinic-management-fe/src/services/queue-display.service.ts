import axiosClient from '@/lib/axios';

// ── Lobby Dashboard Types ──────────────────────────────────────────────────
export interface CurrentPatient {
  appointmentId: string;
  patientName: string;
  patientId: string;
}

export interface RoomOverview {
  roomId: string;
  roomName: string;
  roomType: string;
  location: string;
  departmentName: string | null;
  status: string;
  currentQueueLength: number;
  baseAvgTime: number;
  estimatedWaitMinutes: number;
  doctorName: string | null;
  currentPatient: CurrentPatient | null;
}

export interface LobbyDashboardResponse {
  rooms: RoomOverview[];
  totalWaiting: number;
  totalInProgress: number;
}

// ── Room Queue Board Types ─────────────────────────────────────────────────
export interface QueueEntry {
  appointmentId: string;
  patientName: string;
  patientId: string;
  queuePosition: number;
  priorityScore: number | null;
  waitingSince: string | null;
  estimatedWaitMinutes: number;
  status: string;
}

export interface RoomQueueBoardResponse {
  roomId: string;
  roomName: string;
  roomType: string;
  location: string;
  departmentName: string | null;
  doctorName: string | null;
  currentPatient: QueueEntry | null;
  waitingList: QueueEntry[];
  totalWaiting: number;
  estimatedWaitMinutes: number;
}

// ── Service ────────────────────────────────────────────────────────────────
export const queueDisplayService = {
  getLobbyDashboard: async (): Promise<LobbyDashboardResponse> => {
    const response = await axiosClient.get<LobbyDashboardResponse>('/queue-display/lobby');
    return response.data;
  },

  getRoomQueueBoard: async (roomId: string): Promise<RoomQueueBoardResponse> => {
    const response = await axiosClient.get<RoomQueueBoardResponse>(`/queue-display/room/${roomId}`);
    return response.data;
  },
};
