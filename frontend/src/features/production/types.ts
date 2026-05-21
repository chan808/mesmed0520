export type PlanStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type LotStatus = 'IN_PROGRESS' | 'PASS' | 'FAIL';
export type InspectionResultCode = 'PASS' | 'NG';

export interface ProductionPlanResponse {
  id: number;
  modelName: string;
  planDate: string;
  targetQty: number;
  passCount: number;
  status: PlanStatus;
}

export interface ProductionPlanRequest {
  modelName: string;
  planDate: string;
  targetQty: number;
}

export interface LotDetailResponse {
  lotId: number;
  lotNo: number;
  status: LotStatus;
  planId: number;
  planDate: string;
  modelName: string;
  materials: MaterialResult[];
}

export interface MaterialResult {
  materialId: number;
  partName: string;
  partCode: string;
  materialResult: InspectionResultCode | null;
  items: ItemResult[];
}

export interface ItemResult {
  itemId: number;
  itemName: string;
  specification: string;
  currentResult: InspectionResultCode | null;
  latestRound: number;
  rounds: RoundResult[];
}

export interface RoundResult {
  round: number;
  result: InspectionResultCode;
  inspectedAt: string;
  memo: string | null;
}

export interface InspectionResultRequest {
  inspectionItemId: number;
  result: InspectionResultCode;
  memo?: string;
}

export interface DailyDashboardResponse {
  date: string;
  totalTargetQty: number;
  totalPassCount: number;
  overallPassRate: number;
  byModel: ModelSummary[];
}

export interface ModelSummary {
  planId: number;
  modelName: string;
  targetQty: number;
  passCount: number;
  lotCount: number;
  inProgressCount: number;
  failCount: number;
  ngResultCount: number;
  recheckCount: number;
  passRate: number;
  planStatus: PlanStatus;
}
