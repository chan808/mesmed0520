import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { materialApi } from './api';
import { errorMessage } from '../../shared/api/client';

export function MaterialCreatePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [modelName, setModelName] = useState('');
  const [partName, setPartName] = useState('');
  const [partCode, setPartCode] = useState('');
  const [supplier, setSupplier] = useState('');
  const [materialSpec, setMaterialSpec] = useState('');

  const mutation = useMutation({
    mutationFn: materialApi.create,
    onSuccess: (m) => {
      queryClient.invalidateQueries({ queryKey: ['materials'] });
      navigate(`/materials/${m.id}`);
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    mutation.mutate({
      modelName,
      partName,
      partCode: partCode || undefined,
      supplier: supplier || undefined,
      materialSpec: materialSpec || undefined,
    });
  };

  return (
    <>
      <div className="content-header">
        <h1>자재 등록</h1>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="field">
          <label>모델명</label>
          <input
            value={modelName}
            onChange={(e) => setModelName(e.target.value)}
            placeholder="예: ER-2000 SMART"
            required
          />
        </div>
        <div className="field">
          <label>품명</label>
          <input
            value={partName}
            onChange={(e) => setPartName(e.target.value)}
            placeholder="예: LCD"
            required
          />
        </div>
        <div className="field">
          <label>품번 (선택)</label>
          <input
            value={partCode}
            onChange={(e) => setPartCode(e.target.value)}
            placeholder="예: 10018500701"
          />
        </div>
        <div className="field">
          <label>공급자 (선택)</label>
          <input
            value={supplier}
            onChange={(e) => setSupplier(e.target.value)}
            placeholder="예: KJC Display corporation"
          />
        </div>
        <div className="field">
          <label>규격 or 재질 (선택)</label>
          <input
            value={materialSpec}
            onChange={(e) => setMaterialSpec(e.target.value)}
            placeholder="예: 2.4inch"
          />
        </div>

        {mutation.isError && <div className="error">{errorMessage(mutation.error)}</div>}

        <div className="row-end">
          <button type="button" onClick={() => navigate('/materials')}>
            취소
          </button>
          <button type="submit" className="primary" disabled={mutation.isPending}>
            {mutation.isPending ? '등록 중…' : '등록'}
          </button>
        </div>
      </form>
    </>
  );
}
