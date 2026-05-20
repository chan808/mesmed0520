import { api, unwrap } from '../../shared/api/client';
import type { ApiResponse } from '../../shared/api/types';
import type {
  Material,
  MaterialRequest,
} from './types';

export const materialApi = {
  list: () => unwrap(api.get<ApiResponse<Material[]>>('/materials')),
  detail: (id: number) =>
    unwrap(api.get<ApiResponse<Material>>(`/materials/${id}`)),
  create: (body: MaterialRequest) =>
    unwrap(api.post<ApiResponse<Material>>('/materials', body)),
  remove: (id: number) =>
    unwrap(api.delete<ApiResponse<void>>(`/materials/${id}`)),
};
