export interface Material {
  id: number;
  modelName: string;
  partName: string;
  partCode: string | null;
  suppier: string | null; // Backend has this typo: 'suppier'
  materialSpec: string | null;
}

export interface MaterialRequest {
  modelName: string;
  partName: string;
  partCode?: string;
  supplier?: string;
  materialSpec?: string;
}
