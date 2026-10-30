import axiosClient from '@/lib/axios';
import { getNormalizedResultFileUrls, getResultFileName } from '@/lib/result-file';
import { serviceOrderService } from '@/services/service-order.service';
import { userService } from '@/services/user.service';
import {
  FollowUpOption,
  HealthRecordResponse,
  DoctorMedicalRecordItem,
  MedicalHistoryItem,
  MedicalHistorySearchParams,
  PageResponse,
} from '@/types/health-record.types';

interface MedicalRecordHistoryResponse {
  recordId: string;
  examinationDate: string;
  patientId?: string | null;
  patientName?: string | null;
  doctorName: string;
  diagnosisSummary: string;
  status: string;
  visitType?: string;
  parentRecordId?: string | null;
  rootRecordId?: string | null;
  canFollowUp?: boolean;
  clinicalServiceId?: string | null;
  clinicalServiceName?: string | null;
  departmentName?: string | null;
}

const HISTORY_GROUP_FETCH_LIMIT = 200;

const inferFileType = (fileUrl: string): string => {
  const normalized = fileUrl.toLowerCase();
  if (normalized.includes('.pdf')) {
    return 'pdf';
  }
  if (/(\.jpg|\.jpeg|\.png|\.gif|\.webp)(\?|$)/i.test(normalized)) {
    return 'image';
  }
  return 'file';
};

const getVisitTime = (item: MedicalHistoryItem): number => {
  const time = new Date(item.visitDate).getTime();
  return Number.isNaN(time) ? 0 : time;
};

const getLatestVisitTime = (item: MedicalHistoryItem): number => {
  const followUpTimes = (item.followUps ?? []).map(getVisitTime);
  return Math.max(getVisitTime(item), ...followUpTimes);
};

const groupMedicalHistory = (items: MedicalHistoryItem[]): MedicalHistoryItem[] => {
  const byId = new Map<string, MedicalHistoryItem>(
    items.map((item) => [item.id, { ...item, followUps: [] }])
  );
  const attachedRecordIds = new Set<string>();

  for (const item of byId.values()) {
    const rootId = item.rootRecordId && item.rootRecordId !== item.id ? item.rootRecordId : null;
    const parentId = item.parentRecordId && item.parentRecordId !== item.id ? item.parentRecordId : null;
    const target = rootId ? byId.get(rootId) : parentId ? byId.get(parentId) : null;

    if (target) {
      target.followUps = target.followUps ?? [];
      target.followUps.push(item);
      attachedRecordIds.add(item.id);
    }
  }

  const grouped = Array.from(byId.values()).filter((item) => !attachedRecordIds.has(item.id));
  grouped.forEach((item) => {
    item.followUps = (item.followUps ?? []).sort((a, b) => getVisitTime(a) - getVisitTime(b));
  });
  grouped.sort((a, b) => getLatestVisitTime(b) - getLatestVisitTime(a));

  return grouped;
};

export const healthRecordService = {
  /**
   * Get patient health record
   * Returns patient info and medical history
   */
  getHealthRecord: async (params?: MedicalHistorySearchParams): Promise<HealthRecordResponse> => {
    const requestedPage = params?.page ?? 0;
    const requestedSize = params?.size ?? 10;
    const queryParams = new URLSearchParams();

    queryParams.append('page', '0');
    queryParams.append('size', Math.max(requestedSize, HISTORY_GROUP_FETCH_LIMIT).toString());
    if (params?.keyword) {
      queryParams.append('keyword', params.keyword);
    }

    const historyUrl = `/medical-examination/records/my-history${queryParams.toString() ? `?${queryParams.toString()}` : ''}`;

    const [profile, historyResponse] = await Promise.all([
      userService.getProfile(),
      axiosClient.get<PageResponse<MedicalRecordHistoryResponse>>(historyUrl),
    ]);

    const birthYear = profile.profile.dateOfBirth
      ? new Date(profile.profile.dateOfBirth).getFullYear()
      : 0;

    const historyItems = historyResponse.data.content ?? [];
    const documentsByRecordId = await Promise.all(
      historyItems.map(async (record) => {
        let serviceOrders: Awaited<ReturnType<typeof serviceOrderService.getMyRecordResults>> = [];
        try {
          serviceOrders = await serviceOrderService.getMyRecordResults(record.recordId);
        } catch (error) {
          console.warn('Failed to fetch record service results:', record.recordId, error);
        }

        const documents = serviceOrders.flatMap((order) =>
          getNormalizedResultFileUrls(order).map((fileUrl, index) => ({
            documentId: `${order.orderId}-${index}`,
            orderId: order.orderId,
            fileName: getResultFileName(fileUrl, order.serviceName, index),
            fileUrl,
            fileType: inferFileType(fileUrl),
            uploadedAt: order.createdAt,
          }))
        );

        return {
          recordId: record.recordId,
          documents,
        };
      })
    );

    const documentMap = new Map(documentsByRecordId.map((entry) => [entry.recordId, entry.documents]));

    const medicalHistory: MedicalHistoryItem[] = historyItems.map((record) => ({
      id: record.recordId,
      visitDate: record.examinationDate,
      patientId: record.patientId,
      patientName: record.patientName,
      diagnosis: record.diagnosisSummary || 'Chưa có chẩn đoán',
      doctorName: record.doctorName || 'N/A',
      documents: documentMap.get(record.recordId) ?? [],
      departmentName: record.departmentName ?? undefined,
      status: record.status,
      visitType: record.visitType,
      parentRecordId: record.parentRecordId,
      rootRecordId: record.rootRecordId,
      canFollowUp: Boolean(record.canFollowUp),
      clinicalServiceId: record.clinicalServiceId,
      clinicalServiceName: record.clinicalServiceName,
    }));

    const groupedMedicalHistory = groupMedicalHistory(medicalHistory);
    const pageStart = requestedPage * requestedSize;
    const pagedMedicalHistory = groupedMedicalHistory.slice(pageStart, pageStart + requestedSize);

    return {
      patientInfo: {
        patientId: profile.profile.patientId || '',
        fullName: profile.fullName,
        birthYear,
        healthInsuranceNumber: profile.profile.healthInsuranceNumber,
      },
      medicalHistory: pagedMedicalHistory,
      totalCount: groupedMedicalHistory.length,
    };
  },

  /**
   * Get medical history paginated
   */
  getMedicalHistory: async (
    patientId: string,
    page: number = 0,
    size: number = 10,
    keyword: string = ''
  ): Promise<PageResponse<unknown>> => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (keyword) {
      params.append('keyword', keyword);
    }

     
    const response = await axiosClient.get<any>(
      `/patients/${patientId}/medical-history?${params.toString()}`
    );
    return response.data;
  },

  getMyFollowUpOptions: async (): Promise<FollowUpOption[]> => {
    const response = await axiosClient.get('/medical-examination/records/follow-up-options/my');
    const raw = response.data as unknown;
    if (raw && typeof raw === 'object' && 'data' in raw) {
      return ((raw as { data?: FollowUpOption[] }).data ?? []) as FollowUpOption[];
    }
    return Array.isArray(raw) ? raw as FollowUpOption[] : [];
  },

  getFollowUpOptions: async (patientId: string): Promise<FollowUpOption[]> => {
    const response = await axiosClient.get(`/medical-examination/records/follow-up-options?patientId=${encodeURIComponent(patientId)}`);
    const raw = response.data as unknown;
    if (raw && typeof raw === 'object' && 'data' in raw) {
      return ((raw as { data?: FollowUpOption[] }).data ?? []) as FollowUpOption[];
    }
    return Array.isArray(raw) ? raw as FollowUpOption[] : [];
  },

  getDoctorExaminedRecords: async (
    page: number = 0,
    size: number = 10,
    keyword: string = '',
    fromDate: string = '',
    toDate: string = ''
  ): Promise<PageResponse<DoctorMedicalRecordItem>> => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    const normalizedKeyword = keyword.trim();
    const normalizedFromDate = fromDate.trim();
    const normalizedToDate = toDate.trim();

    if (normalizedKeyword) {
      params.append('keyword', normalizedKeyword);
    }
    if (normalizedFromDate) {
      params.append('fromDate', normalizedFromDate);
    }
    if (normalizedToDate) {
      params.append('toDate', normalizedToDate);
    }

    const response = await axiosClient.get<PageResponse<DoctorMedicalRecordItem>>(
      `/medical-examination/records/doctor-history?${params.toString()}`
    );
    return response.data;
  },

  /**
   * Download medical document
   */
  downloadDocument: async (documentId: string): Promise<Blob> => {
    const response = await axiosClient.get(`/patients/documents/${documentId}/download`, {
      responseType: 'blob',
    });
    return response.data;
  },

  /**
   * Get document preview URL
   */
  getDocumentUrl: (documentId: string): string => {
    return `/patients/documents/${documentId}/view`;
  },
};
