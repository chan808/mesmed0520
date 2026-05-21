import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productionApi } from './api';
import { StatusBadge } from '../../shared/components/StatusBadge';
import { errorMessage } from '../../shared/api/client';
import type { ProductionPlanRequest, LotDetailResponse, InspectionResultCode } from './types';

export function ProductionPage() {
  const queryClient = useQueryClient();
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [showPlanModal, setShowPlanModal] = useState(false);
  const [activeLotId, setActiveLotId] = useState<number | null>(null);

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
          queryClient.invalidateQueries({ queryKey: ['production', 'plans', selectedDate] });
        }}
        onFail={() => {
            if (confirm('이 Lot을 최종 불합격 처리하시겠습니까?')) {
                failLot.mutate(activeLotId);
            }
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
                    <button className="small primary" onClick={() => startLot.mutate(p.id)}>
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

function LotInspectionView({ lotId, onClose, onFail }: { lotId: number; onClose: () => void; onFail: () => void }) {
  const queryClient = useQueryClient();
  const { data: lot, isLoading } = useQuery({
    queryKey: ['production', 'lots', lotId],
    queryFn: () => productionApi.getLot(lotId),
  });

  const submitResult = useMutation({
    mutationFn: ({ itemId, result, memo }: { itemId: number; result: InspectionResultCode; memo?: string }) =>
      productionApi.submitResult(lotId, { inspectionItemId: itemId, result, memo }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['production', 'lots', lotId] });
    },
  });

  if (isLoading) return <p className="empty">Lot 정보를 불러오는 중…</p>;
  if (!lot) return <p className="empty">Lot 정보를 찾을 수 없습니다.</p>;

  const isCompleted = lot.status !== 'IN_PROGRESS';

  return (
    <div className="inspection-view">
      <div className="content-header">
        <div className="row" style={{ gap: 12 }}>
          <button onClick={onClose}>← 뒤로</button>
          <h1>
            Lot #{lot.lotNo} - {lot.modelName} (상태: <StatusBadge value={lot.status} />)
          </h1>
        </div>
        {!isCompleted && (
          <button className="danger" onClick={onFail}>
            Lot 실패(불합격) 처리
          </button>
        )}
      </div>

      <div className="section">
        <div className="section-title">자재별 검사 현황</div>
        <div className="material-list">
          {lot.materials.map((m) => (
            <div key={m.materialId} className="material-card section" style={{ padding: 16 }}>
              <div className="row" style={{ justifyContent: 'space-between', marginBottom: 12 }}>
                <h3>
                  {m.partName} ({m.partCode})
                </h3>
                <StatusBadge value={m.materialResult ?? 'WAITING'} suffix="material" />
              </div>
              <table>
                <thead>
                  <tr>
                    <th>검사항목</th>
                    <th>규격</th>
                    <th>최근결과</th>
                    <th>차수</th>
                    <th style={{ width: 150 }}>판정</th>
                  </tr>
                </thead>
                <tbody>
                  {m.items.map((item) => (
                    <tr key={item.itemId}>
                      <td>{item.itemName}</td>
                      <td>{item.specification}</td>
                      <td>
                        {item.currentResult ? (
                          <StatusBadge value={item.currentResult} />
                        ) : (
                          <span className="muted">-</span>
                        )}
                      </td>
                      <td className="num">{item.latestRound}회차</td>
                      <td>
                        {!isCompleted && (
                          <div className="row" style={{ gap: 4 }}>
                            <button
                              className="small primary"
                              onClick={() => submitResult.mutate({ itemId: item.itemId, result: 'PASS' })}
                              disabled={submitResult.isPending}
                            >
                              PASS
                            </button>
                            <button
                              className="small danger"
                              onClick={() => {
                                const memo = prompt('불합격 사유를 입력하세요');
                                if (memo !== null) {
                                  submitResult.mutate({ itemId: item.itemId, result: 'NG', memo });
                                }
                              }}
                              disabled={submitResult.isPending}
                            >
                              NG
                            </button>
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))}
        </div>
      </div>
      
      <div className="row-center" style={{ marginTop: 24 }}>
          <button className="primary large" onClick={onClose}>검사 화면 닫기</button>
      </div>
    </div>
  );
}
