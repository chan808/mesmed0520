import { api, unwrap } from '../../shared/api/client';
import type { ApiResponse } from '../../shared/api/types';
import type {
  BatchInspectionResultRequest,
  DailyDashboardResponse,
  InspectionResultRequest,
  LotDetailResponse,
  ProductionPlanRequest,
  ProductionPlanResponse,
} from './types';

export const productionApi = {
  // Plans
  listPlans: (date: string) =>
    unwrap(api.get<ApiResponse<ProductionPlanResponse[]>>(`/production/plans?date=${date}`)),
  
  getPlan: (id: number) =>
    unwrap(api.get<ApiResponse<ProductionPlanResponse>>(`/production/plans/${id}`)),
  
  registerPlan: (body: ProductionPlanRequest) =>
    unwrap(api.post<ApiResponse<ProductionPlanResponse>>('/production/plans', body)),

  // Lots
  startLot: (planId: number) =>
    unwrap(api.post<ApiResponse<LotDetailResponse>>(`/production/plans/${planId}/lots`)),
  
  getLot: (lotId: number) =>
    unwrap(api.get<ApiResponse<LotDetailResponse>>(`/production/lots/${lotId}`)),
  
  submitResult: (lotId: number, body: InspectionResultRequest) =>
    unwrap(api.post<ApiResponse<any>>(`/production/lots/${lotId}/results`, body)),

  submitBatchResults: (lotId: number, body: BatchInspectionResultRequest) =>
    unwrap(api.post<ApiResponse<LotDetailResponse>>(`/production/lots/${lotId}/results/batch`, body)),

  failLot: (lotId: number) =>
    unwrap(api.patch<ApiResponse<LotDetailResponse>>(`/production/lots/${lotId}/fail`)),
};

export const dashboardApi = {
  getDaily: (date?: string) =>
    unwrap(api.get<ApiResponse<DailyDashboardResponse>>(`/dashboard/daily${date ? `?date=${date}` : ''}`)),
};
