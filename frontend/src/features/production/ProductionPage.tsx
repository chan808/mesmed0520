import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productionApi } from './api';
import { StatusBadge } from '../../shared/components/StatusBadge';
import { errorMessage } from '../../shared/api/client';
import type { ProductionPlanRequest, LotDetailResponse, InspectionResultCode, InspectionResultRequest } from './types';

export function ProductionPage() {
  const queryClient = useQueryClient();
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [showPlanModal, setShowPlanModal] = useState(false);
  const [activeLotId, setActiveLotId] = useState<number | null>(null);
  const [activePlanId, setActivePlanId] = useState<number | null>(null);

  const { data: plans, isLoading: plansLoading } = useQuery({
    queryKey: ['production', 'plans', selectedDate],
    queryFn: () => productionApi.listPlans(selectedDate),
  });

  const registerPlan = useMutation({
    mutationFn: productionApi.registerPlan,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['production', 'plans', selectedDate] });
      setShowPlanModal(false);
    },
  });

  const startLot = useMutation({
    mutationFn: productionApi.startLot,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['production', 'plans', selectedDate] });
      setActiveLotId(data.lotId);
    },
  });

  const handleStartLot = (planId: number) => {
    setActivePlanId(planId);
    startLot.mutate(planId);
  };

  const failLot = useMutation({
    mutationFn: productionApi.failLot,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['production', 'plans', selectedDate] });
      setActiveLotId(null);
    },
  });

  if (activeLotId) {
    return (
      <LotInspectionView
        lotId={activeLotId}
        onClose={() => {
          setActiveLotId(null);
          setActivePlanId(null);
          queryClient.invalidateQueries({ queryKey: ['production', 'plans', selectedDate] });
        }}
        onFail={() => {
          if (confirm('이 Lot을 최종 불합격 처리하시겠습니까?')) {
            failLot.mutate(activeLotId);
          }
        }}
        onNextLot={() => {
          if (activePlanId) handleStartLot(activePlanId);
        }}
      />
    );
  }

  return (
    <>
      <div className="content-header">
        <h1>생산 공정 관리</h1>
        <div className="row" style={{ gap: 12 }}>
          <input
            type="date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
          />
          <button className="primary" onClick={() => setShowPlanModal(true)}>
            + 생산 계획 등록
          </button>
        </div>
      </div>

      {plansLoading ? (
        <p className="empty">로딩 중…</p>
      ) : plans && plans.length > 0 ? (
        <table>
          <thead>
            <tr>
              <th>모델명</th>
              <th>목표 수량</th>
              <th>합격 수량</th>
              <th>상태</th>
              <th style={{ width: 120 }}>작업</th>
            </tr>
          </thead>
          <tbody>
            {plans.map((p) => (
              <tr key={p.id}>
                <td>{p.modelName}</td>
                <td className="num">{p.targetQty}</td>
                <td className="num">{p.passCount}</td>
                <td>
                  <StatusBadge value={p.status} />
                </td>
                <td>
                  {p.status !== 'COMPLETED' && p.status !== 'CANCELLED' && (
                    <button className="small primary" onClick={() => handleStartLot(p.id)}>
                      Lot 시작
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <p className="empty">{selectedDate}에 등록된 생산 계획이 없습니다.</p>
      )}

      {showPlanModal && (
        <PlanModal
          date={selectedDate}
          onClose={() => setShowPlanModal(false)}
          onSubmit={(data) => registerPlan.mutate(data)}
          isLoading={registerPlan.isPending}
          error={registerPlan.isError ? errorMessage(registerPlan.error) : undefined}
        />
      )}
    </>
  );
}

function PlanModal({
  date,
  onClose,
  onSubmit,
  isLoading,
  error,
}: {
  date: string;
  onClose: () => void;
  onSubmit: (data: ProductionPlanRequest) => void;
  isLoading: boolean;
  error?: string;
}) {
  const [form, setForm] = useState<ProductionPlanRequest>({
    modelName: '',
    planDate: date,
    targetQty: 10,
  });

  return (
    <div className="modal-backdrop">
      <div className="modal">
        <h2>생산 계획 등록</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            onSubmit(form);
          }}
        >
          <div className="field">
            <label>계획일</label>
            <input
              type="date"
              value={form.planDate}
              onChange={(e) => setForm({ ...form, planDate: e.target.value })}
              required
            />
          </div>
          <div className="field">
            <label>모델명</label>
            <input
              value={form.modelName}
              onChange={(e) => setForm({ ...form, modelName: e.target.value })}
              placeholder="예: 갤럭시 S24"
              required
            />
          </div>
          <div className="field">
            <label>목표 수량</label>
            <input
              type="number"
              min={1}
              value={form.targetQty}
              onChange={(e) => setForm({ ...form, targetQty: Number(e.target.value) })}
              required
            />
          </div>
          {error && <div className="error">{error}</div>}
          <div className="row-end">
            <button type="button" onClick={onClose} disabled={isLoading}>
              취소
            </button>
            <button type="submit" className="primary" disabled={isLoading}>
              {isLoading ? '등록 중…' : '등록'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

type ItemFormState = { result: InspectionResultCode; measuredValue: string; memo: string };

function LotInspectionView({ lotId, onClose, onFail, onNextLot }: {
  lotId: number;
  onClose: () => void;
  onFail: () => void;
  onNextLot: () => void;
}) {
  const queryClient = useQueryClient();
  const { data: lot, isLoading } = useQuery({
    queryKey: ['production', 'lots', lotId],
    queryFn: () => productionApi.getLot(lotId),
  });

  const [form, setForm] = useState<Record<number, ItemFormState>>({});
  const [validationError, setValidationError] = useState<string>();

  // PASS 항목은 잠금, 나머지만 폼 상태 유지
  useEffect(() => {
    if (!lot) return;
    setForm(prev => {
      const next: Record<number, ItemFormState> = {};
      lot.materials.flatMap(m => m.items).forEach(item => {
        if (item.currentResult === 'PASS') return;
        next[item.itemId] = prev[item.itemId] ?? { result: 'PASS', measuredValue: '', memo: '' };
      });
      return next;
    });
  }, [lot]);

  const submitBatch = useMutation({
    mutationFn: (items: InspectionResultRequest[]) =>
      productionApi.submitBatchResults(lotId, { items }),
    onSuccess: (data) => {
      queryClient.setQueryData(['production', 'lots', lotId], data);
      if (data.status === 'PASS') {
        onNextLot();
      }
    },
  });

  const handleSubmit = () => {
    if (!lot) return;
    const allItems = lot.materials.flatMap(m => m.items);
    const pendingItems = allItems.filter(i => i.itemId in form);

    for (const item of pendingItems) {
      if (item.measurementType === 'NUMERIC' && !form[item.itemId].measuredValue) {
        setValidationError(`'${item.itemName}' 실측값을 입력하세요`);
        return;
      }
    }
    setValidationError(undefined);

    const requests: InspectionResultRequest[] = pendingItems.map(item => {
      const s = form[item.itemId];
      if (item.measurementType === 'NUMERIC') {
        return { inspectionItemId: item.itemId, measuredValue: parseFloat(s.measuredValue) };
      }
      return { inspectionItemId: item.itemId, result: s.result, memo: s.memo || undefined };
    });

    submitBatch.mutate(requests);
  };

  if (isLoading) return <p className="empty">Lot 정보를 불러오는 중…</p>;
  if (!lot) return <p className="empty">Lot 정보를 찾을 수 없습니다.</p>;

  const isCompleted = lot.status !== 'IN_PROGRESS';
  const pendingCount = Object.keys(form).length;
  const hasRecheck = lot.materials.flatMap(m => m.items).some(i => i.currentResult === 'NG');

  return (
    <div className="inspection-view">
      <div className="content-header">
        <div className="row" style={{ gap: 12 }}>
          <button onClick={onClose}>← 뒤로</button>
          <h1>Lot #{lot.lotNo} — {lot.modelName} (<StatusBadge value={lot.status} />)</h1>
        </div>
        {!isCompleted && (
          <button className="danger" onClick={onFail}>Lot 불합격 처리</button>
        )}
      </div>

      {lot.materials.map(m => (
        <div key={m.materialId} className="section" style={{ padding: 16, marginBottom: 16 }}>
          <div className="row" style={{ justifyContent: 'space-between', marginBottom: 12 }}>
            <h3>{m.partName} ({m.partCode})</h3>
            <StatusBadge value={m.materialResult ?? 'WAITING'} suffix="material" />
          </div>
          <table>
            <thead>
              <tr>
                <th>검사항목</th>
                <th>규격</th>
                <th>판정 입력</th>
                <th>차수</th>
              </tr>
            </thead>
            <tbody>
              {m.items.map(item => {
                const locked = item.currentResult === 'PASS';
                const s = form[item.itemId];
                return (
                  <tr key={item.itemId} style={item.currentResult === 'NG' ? { background: '#fff1f0' } : undefined}>
                    <td>{item.itemName}</td>
                    <td>
                      {item.specification}
                      {item.measurementType === 'NUMERIC' &&
                        ` (${item.minValue ?? '?'} ~ ${item.maxValue ?? '?'} ${item.unit ?? ''})`}
                    </td>
                    <td>
                      {locked ? (
                        <StatusBadge value="PASS" />
                      ) : isCompleted ? (
                        item.currentResult
                          ? <StatusBadge value={item.currentResult} />
                          : <span className="muted">미검사</span>
                      ) : s ? (
                        item.measurementType === 'NUMERIC' ? (
                          <input
                            type="number"
                            step="any"
                            placeholder="실측값"
                            style={{ width: 100 }}
                            value={s.measuredValue}
                            onChange={e =>
                              setForm(prev => ({
                                ...prev,
                                [item.itemId]: { ...prev[item.itemId], measuredValue: e.target.value },
                              }))
                            }
                          />
                        ) : (
                          <div className="row" style={{ gap: 4 }}>
                            <button
                              className={`small ${s.result === 'PASS' ? 'primary' : ''}`}
                              onClick={() =>
                                setForm(prev => ({
                                  ...prev,
                                  [item.itemId]: { ...prev[item.itemId], result: 'PASS', memo: '' },
                                }))
                              }
                            >
                              PASS
                            </button>
                            <button
                              className={`small ${s.result === 'NG' ? 'danger' : ''}`}
                              onClick={() =>
                                setForm(prev => ({
                                  ...prev,
                                  [item.itemId]: { ...prev[item.itemId], result: 'NG' },
                                }))
                              }
                            >
                              NG
                            </button>
                            {s.result === 'NG' && (
                              <input
                                placeholder="불합격 사유"
                                style={{ width: 120 }}
                                value={s.memo}
                                onChange={e =>
                                  setForm(prev => ({
                                    ...prev,
                                    [item.itemId]: { ...prev[item.itemId], memo: e.target.value },
                                  }))
                                }
                              />
                            )}
                          </div>
                        )
                      ) : null}
                    </td>
                    <td className="num">{item.latestRound > 0 ? `${item.latestRound}회차` : '-'}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ))}

      {!isCompleted && (
        <div className="row-center" style={{ marginTop: 24, flexDirection: 'column', gap: 8 }}>
          {validationError && <div className="error">{validationError}</div>}
          {submitBatch.isError && <div className="error">{errorMessage(submitBatch.error)}</div>}
          {pendingCount > 0 ? (
            <button
              className="primary large"
              onClick={handleSubmit}
              disabled={submitBatch.isPending}
            >
              {submitBatch.isPending
                ? '제출 중…'
                : hasRecheck
                ? `재검사 제출 (${pendingCount}개 항목)`
                : `검사 결과 일괄 제출 (${pendingCount}개 항목)`}
            </button>
          ) : (
            <p className="muted">모든 항목이 검사 완료되었습니다.</p>
          )}
        </div>
      )}

      {isCompleted && (
        <div className="row-center" style={{ marginTop: 24 }}>
          <button className="primary large" onClick={onClose}>검사 화면 닫기</button>
        </div>
      )}
    </div>
  );
}
