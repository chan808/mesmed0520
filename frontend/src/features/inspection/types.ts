export interface InspectionStandard {
  id: number;
  materialId: number;
  modelName: string;
  partName: string;
  rev: number;
  establishedAt: string;
  inspectionType: string | null;
  inspectionLevel: string | null;
  strictness: string | null;
  aql: number | null;
  aqlAc: number | null;
  aqlRe: number | null;
}

export interface InspectionStandardRequest {
  materialId: number;
  establishedAt: string;
  inspectionType?: string;
  inspectionLevel?: string;
  strictness?: string;
  aql?: number;
  aqlAc?: number;
  aqlRe?: number;
  revisionDate: string;
  revisionNote: string;
  confirmedBy?: string;
}

export interface InspectionStandardUpdateRequest {
  establishedAt: string;
  inspectionType?: string;
  inspectionLevel?: string;
  strictness?: string;
  aql?: number;
  aqlAc?: number;
  aqlRe?: number;
  revisionDate: string;
  revisionNote: string;
  confirmedBy?: string;
}

export interface InspectionItem {
  id: number;
  standardId: number;
  itemName: string;
  specification: string | null;
  method: string | null;
  equipment: string | null;
  timing: string | null;
}

export interface InspectionItemRequest {
  itemName: string;
  specification?: string;
  method?: string;
  equipment?: string;
  timing?: string;
}

export interface RevisionHistory {
  id: number;
  standardId: number;
  rev: number;
  revisionDate: string;
  revisionNote: string;
  confirmedBy: string | null;
  addedItems: InspectionItem[];
}

export interface InspectionSnapshotResponse {
  standardId: number;
  rev: number;
  revisionDate: string;
  revisionNote: string;
  confirmedBy: string | null;
  items: InspectionItem[];
}

export interface RevisionRequest {
  standardId: number;
  revisionDate: string;
  revisionNote: string;
  confirmedBy?: string;
  addItems?: InspectionItemRequest[];
  deleteItemIds?: number[];
}
