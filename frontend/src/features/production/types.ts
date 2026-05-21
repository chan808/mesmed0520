export type PlanStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type LotStatus = 'IN_PROGRESS' | 'PASS' | 'FAIL';
export type InspectionResultCode = 'PASS' | 'NG';

export interface ProductionPlanResponse {
  id: number;
  modelName: string;
  planDate: string;
  targetQty: number;
  passCount: number;
  failCount: number;
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

export type MeasurementType = 'VISUAL' | 'NUMERIC';

export interface ItemResult {
  itemId: number;
  itemName: string;
  specification: string;
  measurementType: MeasurementType;
  minValue: number | null;
  maxValue: number | null;
  unit: string | null;
  currentResult: InspectionResultCode | null;
  latestRound: number;
  rounds: RoundResult[];
}

export interface RoundResult {
  round: number;
  result: InspectionResultCode;
  measuredValue: number | null;
  inspectedAt: string;
  memo: string | null;
}

export interface InspectionResultRequest {
  inspectionItemId: number;
  result?: InspectionResultCode;
  measuredValue?: number;
  memo?: string;
}

export interface BatchInspectionResultRequest {
  items: InspectionResultRequest[];
}

export interface UpdateTargetQtyRequest {
  targetQty: number;
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
