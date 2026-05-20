import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import { materialApi } from './api';
import { inspectionApi } from '../inspection/api';
import { useAuth } from '../../shared/auth/useAuth';
import { errorMessage } from '../../shared/api/client';
import type {
  InspectionStandardRequest,
  InspectionStandardUpdateRequest,
  InspectionItemRequest,
  RevisionRequest,
  InspectionItem,
} from '../inspection/types';

export function MaterialDetailPage() {
  const { id } = useParams<{ id: string }>();
  const materialId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { role } = useAuth();
  const isAdmin = role === 'ADMIN';

  const [showStandardForm, setShowStandardForm] = useState(false);
  const [showStandardUpdateForm, setShowStandardUpdateForm] = useState(false);

  const [pendingAddItems, setPendingAddItems] = useState<InspectionItemRequest[]>([]);
  const [pendingDeleteIds, setPendingDeleteIds] = useState<number[]>([]);
  const [showRevisionForm, setShowRevisionForm] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);

  // null = 최신 rev 보기, 숫자 = 과거 스냅샷 보기
  const [selectedRev, setSelectedRev] = useState<number | null>(null);

  const { data: material, isLoading: materialLoading } = useQuery({
    queryKey: ['materials', materialId],
    queryFn: () => materialApi.detail(materialId),
    enabled: !Number.isNaN(materialId),
  });

  const { data: standards, isLoading: standardsLoading } = useQuery({
    queryKey: ['materials', materialId, 'standards'],
    queryFn: () => inspectionApi.listStandards(materialId),
    enabled: !Number.isNaN(materialId),
  });

  // 원자재당 기준서는 하나 — 마지막(최신) 기준서를 사용
  const currentStandard = standards && standards.length > 0
    ? standards[standards.length - 1]
    : undefined;

  const { data: revisions } = useQuery({
    queryKey: ['standards', currentStandard?.id, 'revisions'],
    queryFn: () => inspectionApi.listRevisions(currentStandard!.id),
    enabled: !!currentStandard,
  });

  const latestRev = revisions && revisions.length > 0
    ? revisions[revisions.length - 1].rev
    : null;

  // null이면 최신, 숫자이면 과거 스냅샷
  const isViewingLatest = selectedRev === null;

  const { data: latestItems } = useQuery({
    queryKey: ['standards', currentStandard?.id, 'items'],
    queryFn: () => inspectionApi.listItems(currentStandard!.id),
    enabled: !!currentStandard && isViewingLatest,
  });

  const { data: snapshot } = useQuery({
    queryKey: ['standards', currentStandard?.id, 'snapshot', selectedRev],
    queryFn: () => inspectionApi.getSnapshot(currentStandard!.id, selectedRev!),
    enabled: !!currentStandard && !isViewingLatest && selectedRev !== null,
  });

  const displayItems = isViewingLatest ? latestItems : snapshot?.items;

  const createStandard = useMutation({
    mutationFn: inspectionApi.createStandard,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['materials', materialId, 'standards'] });
      setSelectedRev(null);
      setShowStandardForm(false);
    },
  });

  const updateStandard = useMutation({
    mutationFn: inspectionApi.updateStandard,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['materials', materialId, 'standards'] });
      queryClient.invalidateQueries({ queryKey: ['standards', currentStandard?.id, 'revisions'] });
      setSelectedRev(null);
      setShowStandardUpdateForm(false);
    },
  });

  const addRevision = useMutation({
    mutationFn: inspectionApi.addRevision,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['materials', materialId, 'standards'] });
      queryClient.invalidateQueries({ queryKey: ['standards', currentStandard?.id, 'items'] });
      queryClient.invalidateQueries({ queryKey: ['standards', currentStandard?.id, 'revisions'] });
      setPendingAddItems([]);
      setPendingDeleteIds([]);
      setShowRevisionForm(false);
      setIsEditMode(false);
      setSelectedRev(null);
    },
  });

  const handleAddItemIntent = (item: InspectionItemRequest) => {
    setPendingAddItems([...pendingAddItems, item]);
  };

  const handleDeleteItemIntent = (itemId: number) => {
    setPendingDeleteIds([...pendingDeleteIds, itemId]);
  };

  if (materialLoading || standardsLoading) return <p className="empty">로딩 중…</p>;
  if (!material) return <p className="empty">자재를 찾을 수 없습니다.</p>;

  const hasStandard = standards && standards.length > 0;

  return (
    <div className="inspection-standard-view">
      <div className="content-header">
        <h1>검사기준 관리</h1>
        <div className="row" style={{ gap: 8 }}>
          {/* 개정 이력 기반 버전 선택 드롭다운 */}
          {hasStandard && revisions && revisions.length > 0 && (
            <select
              value={selectedRev ?? latestRev ?? 0}
              onChange={(e) => {
                const rev = Number(e.target.value);
                setSelectedRev(rev === latestRev ? null : rev);
                setIsEditMode(false);
                setPendingAddItems([]);
                setPendingDeleteIds([]);
              }}
            >
              {[...revisions].reverse().map((r) => (
                <option key={r.rev} value={r.rev}>
                  Rev.{r.rev} ({r.revisionDate}){r.rev === latestRev ? ' ★최신' : ''}
                </option>
              ))}
            </select>
          )}
          {/* 기준서가 없을 때만 제정, 있고 최신 rev 볼 때는 개정 */}
          {isAdmin && !hasStandard && (
            <button className="primary" onClick={() => setShowStandardForm(true)}>
              기준서 제정
            </button>
          )}
          {isAdmin && hasStandard && isViewingLatest && (
            <button className="primary" onClick={() => setShowStandardUpdateForm(true)}>
              기준서 개정
            </button>
          )}
          <button onClick={() => navigate('/materials')}>목록으로</button>
        </div>
      </div>

      {/* 과거 버전 조회 중 알림 */}
      {!isViewingLatest && (
        <div className="info-banner" style={{
          background: 'var(--warning-bg, #fff7e6)',
          border: '1px solid var(--warning-border, #ffc069)',
          borderRadius: 6,
          padding: '8px 12px',
          marginBottom: 12,
          fontSize: 13,
          color: '#7c5200',
        }}>
          Rev.{selectedRev} 스냅샷 보기 중 — 읽기 전용입니다.
        </div>
      )}

      {/* 1. 기준서 헤더 */}
      <div className="section standard-header">
        <table>
          <tbody>
            <tr>
              <th>모델명</th>
              <td>{material.modelName}</td>
              <th>제정일</th>
              <td>{currentStandard?.establishedAt ?? '-'}</td>
            </tr>
            <tr>
              <th>품 명</th>
              <td>{material.partName}</td>
              <th>검사 방식</th>
              <td>{currentStandard?.inspectionType ?? '-'}</td>
            </tr>
            <tr>
              <th>품 번</th>
              <td>{material.partCode ?? '-'}</td>
              <th>검사 수준</th>
              <td>{currentStandard?.inspectionLevel ?? '-'}</td>
            </tr>
            <tr>
              <th>공급자</th>
              <td>{material.suppier ?? '-'}</td>
              <th>검사의 엄격도</th>
              <td>{currentStandard?.strictness ?? '-'}</td>
            </tr>
            <tr>
              <th>규격 or 재질</th>
              <td>{material.materialSpec ?? '-'}</td>
              <th>AQL</th>
              <td>
                {currentStandard
                  ? `${currentStandard.aql ?? '-'} (Ac: ${currentStandard.aqlAc ?? '-'}, Re: ${currentStandard.aqlRe ?? '-'})`
                  : '-'}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      {/* 2. 검사항목 */}
      <div className="section">
        <div className="row" style={{ justifyContent: 'space-between', marginBottom: 8 }}>
          <div className="section-title">검사항목</div>
          {/* 편집 모드는 최신 rev 볼 때만 */}
          {isAdmin && currentStandard && isViewingLatest && (
            isEditMode ? (
              <div className="row" style={{ gap: 8 }}>
                <AddItemInlineForm onConfirm={handleAddItemIntent} />
                <button className="primary" onClick={() => setShowRevisionForm(true)}>최종 개정 저장</button>
                <button onClick={() => {
                  setIsEditMode(false);
                  setPendingAddItems([]);
                  setPendingDeleteIds([]);
                }}>취소</button>
              </div>
            ) : (
              <button onClick={() => setIsEditMode(true)}>개정 편집 모드</button>
            )
          )}
        </div>
        <table>
          <thead>
            <tr>
              <th>검사항목</th>
              <th>규격(Spec)</th>
              <th>검사 방법</th>
              <th>측정기기</th>
              <th>주기</th>
              {isAdmin && isViewingLatest && <th style={{ width: 60 }} />}
            </tr>
          </thead>
          <tbody>
            {((displayItems && displayItems.length > 0) || pendingAddItems.length > 0) ? (
              <>
                {displayItems?.map((item) => {
                  const isDeleted = pendingDeleteIds.includes(item.id);
                  return (
                    <tr key={item.id} style={{ textDecoration: isDeleted ? 'line-through' : 'none', color: isDeleted ? 'red' : 'inherit' }}>
                      <td>{item.itemName}</td>
                      <td>{item.specification ?? '-'}</td>
                      <td>{item.method ?? '-'}</td>
                      <td>{item.equipment ?? '-'}</td>
                      <td>{item.timing ?? '-'}</td>
                      {isAdmin && isViewingLatest && (
                        <td>
                          {isEditMode && !isDeleted && (
                            <button
                              className="small danger"
                              onClick={() => handleDeleteItemIntent(item.id)}
                            >
                              삭제
                            </button>
                          )}
                          {isEditMode && isDeleted && (
                            <button
                              className="small"
                              onClick={() => setPendingDeleteIds(pendingDeleteIds.filter(id => id !== item.id))}
                            >
                              취소
                            </button>
                          )}
                        </td>
                      )}
                    </tr>
                  );
                })}
                {isViewingLatest && pendingAddItems.map((item, idx) => (
                  <tr key={`new-${idx}`} style={{ color: 'green', fontWeight: 'bold' }}>
                    <td>{item.itemName} (신규)</td>
                    <td>{item.specification ?? '-'}</td>
                    <td>{item.method ?? '-'}</td>
                    <td>{item.equipment ?? '-'}</td>
                    <td>{item.timing ?? '-'}</td>
                    {isAdmin && isViewingLatest && (
                      <td>
                        {isEditMode && (
                          <button
                            className="small danger"
                            onClick={() => setPendingAddItems(pendingAddItems.filter((_, i) => i !== idx))}
                          >
                            제거
                          </button>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </>
            ) : (
              <tr>
                <td colSpan={(isAdmin && isViewingLatest) ? 6 : 5} className="empty">
                  등록된 검사항목이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* 3. 개정 이력 */}
      <div className="section">
        <div className="section-title" style={{ marginBottom: 8 }}>개정 이력</div>
        <table>
          <thead>
            <tr>
              <th style={{ width: 60 }}>Rev</th>
              <th style={{ width: 120 }}>개정일</th>
              <th>개정내용</th>
              <th style={{ width: 100 }}>확인</th>
            </tr>
          </thead>
          <tbody>
            {revisions && revisions.length > 0 ? (
              revisions.map((rev) => (
                <tr
                  key={rev.id}
                  style={{
                    cursor: 'pointer',
                    background: selectedRev === rev.rev ? 'var(--row-hover, #f0f7ff)' : undefined,
                    fontWeight: rev.rev === latestRev && isViewingLatest ? 'bold' : undefined,
                  }}
                  onClick={() => {
                    setSelectedRev(rev.rev === latestRev ? null : rev.rev);
                    setIsEditMode(false);
                    setPendingAddItems([]);
                    setPendingDeleteIds([]);
                  }}
                >
                  <td className="num">{rev.rev}</td>
                  <td>{rev.revisionDate}</td>
                  <td>{rev.revisionNote}</td>
                  <td>{rev.confirmedBy ?? '-'}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={4} className="empty">
                  개정 이력이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* 기준서 제정 모달 */}
      {showStandardForm && (
        <StandardFormModal
          materialId={materialId}
          onClose={() => setShowStandardForm(false)}
          onSubmit={(data: InspectionStandardRequest) => createStandard.mutate(data)}
          isLoading={createStandard.isPending}
          error={createStandard.isError ? errorMessage(createStandard.error) : undefined}
        />
      )}

      {/* 기준서 헤더 개정 모달 */}
      {showStandardUpdateForm && currentStandard && (
        <StandardUpdateFormModal
          standard={currentStandard}
          onClose={() => setShowStandardUpdateForm(false)}
          onSubmit={(body: InspectionStandardUpdateRequest) =>
            updateStandard.mutate({ id: currentStandard.id, body })
          }
          isLoading={updateStandard.isPending}
          error={updateStandard.isError ? errorMessage(updateStandard.error) : undefined}
        />
      )}

      {/* 개정 이력 모달 */}
      {showRevisionForm && currentStandard && (
        <RevisionFormModal
          standardId={currentStandard.id}
          pendingAddItems={pendingAddItems}
          pendingDeleteIds={pendingDeleteIds}
          onClose={() => setShowRevisionForm(false)}
          onSubmit={(data: RevisionRequest) => addRevision.mutate(data)}
          isLoading={addRevision.isPending}
          error={addRevision.isError ? errorMessage(addRevision.error) : undefined}
        />
      )}
    </div>
  );
}

function AddItemInlineForm({ onConfirm }: { onConfirm: (item: InspectionItemRequest) => void }) {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<InspectionItemRequest>({
    itemName: '',
    specification: '',
    method: '육안',
    equipment: '육안확인',
    timing: '입고 시',
  });

  if (!open) {
    return (
      <button className="small" onClick={() => setOpen(true)}>
        + 항목 추가
      </button>
    );
  }

  return (
    <div className="modal-backdrop">
      <div className="modal">
        <h2>검사항목 추가</h2>
        <p className="muted" style={{ marginBottom: 12 }}>
          추가된 항목은 '최종 개정 저장' 시 일괄 반영됩니다.
        </p>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            setOpen(false);
            onConfirm(form);
          }}
        >
          <div className="field">
            <label>검사항목</label>
            <input
              value={form.itemName}
              onChange={(e) => setForm({ ...form, itemName: e.target.value })}
              required
            />
          </div>
          <div className="field">
            <label>규격(Spec)</label>
            <input
              value={form.specification}
              onChange={(e) => setForm({ ...form, specification: e.target.value })}
            />
          </div>
          <div className="field">
            <label>검사 방법</label>
            <input
              value={form.method}
              onChange={(e) => setForm({ ...form, method: e.target.value })}
            />
          </div>
          <div className="field">
            <label>측정기기</label>
            <input
              value={form.equipment}
              onChange={(e) => setForm({ ...form, equipment: e.target.value })}
            />
          </div>
          <div className="field">
            <label>주기</label>
            <input
              value={form.timing}
              onChange={(e) => setForm({ ...form, timing: e.target.value })}
            />
          </div>
          <div className="row-end">
            <button type="button" onClick={() => setOpen(false)}>
              취소
            </button>
            <button type="submit" className="primary">
              임시 추가
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function RevisionFormModal({
  standardId,
  pendingAddItems,
  pendingDeleteIds,
  onClose,
  onSubmit,
  isLoading,
  error,
}: {
  standardId: number;
  pendingAddItems: InspectionItemRequest[];
  pendingDeleteIds: number[];
  onClose: () => void;
  onSubmit: (data: RevisionRequest) => void;
  isLoading?: boolean;
  error?: string;
}) {
  const [form, setForm] = useState({
    revisionDate: new Date().toISOString().split('T')[0],
    revisionNote: '',
    confirmedBy: '',
  });

  const hasChanges = pendingAddItems.length > 0 || pendingDeleteIds.length > 0;

  return (
    <div className="modal-backdrop">
      <div className="modal">
        <h2>개정 이력 등록</h2>

        {hasChanges && (
          <div className="revision-summary">
            {pendingAddItems.length > 0 && (
              <p>추가 항목: {pendingAddItems.map((i) => i.itemName).join(', ')}</p>
            )}
            {pendingDeleteIds.length > 0 && (
              <p>삭제 항목 ID: {pendingDeleteIds.join(', ')}</p>
            )}
          </div>
        )}

        <form
          onSubmit={(e) => {
            e.preventDefault();
            onSubmit({
              standardId,
              revisionDate: form.revisionDate,
              revisionNote: form.revisionNote,
              confirmedBy: form.confirmedBy || undefined,
              addItems: pendingAddItems.length > 0 ? pendingAddItems : undefined,
              deleteItemIds: pendingDeleteIds.length > 0 ? pendingDeleteIds : undefined,
            });
          }}
        >
          <div className="field">
            <label>개정일</label>
            <input
              type="date"
              value={form.revisionDate}
              onChange={(e) => setForm({ ...form, revisionDate: e.target.value })}
              required
            />
          </div>
          <div className="field">
            <label>개정내용</label>
            <textarea
              value={form.revisionNote}
              onChange={(e) => setForm({ ...form, revisionNote: e.target.value })}
              required
            />
          </div>
          <div className="field">
            <label>확인자</label>
            <input
              value={form.confirmedBy}
              onChange={(e) => setForm({ ...form, confirmedBy: e.target.value })}
            />
          </div>
          {error && <div className="error">{error}</div>}
          <div className="row-end">
            <button type="button" onClick={onClose} disabled={isLoading}>
              취소
            </button>
            <button type="submit" className="primary" disabled={isLoading}>
              {isLoading ? '저장 중…' : '저장'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function StandardFormModal({
  materialId,
  onClose,
  onSubmit,
  isLoading,
  error,
}: {
  materialId: number;
  onClose: () => void;
  onSubmit: (data: InspectionStandardRequest) => void;
  isLoading?: boolean;
  error?: string;
}) {
  const [form, setForm] = useState<InspectionStandardRequest>({
    materialId,
    establishedAt: new Date().toISOString().split('T')[0],
    inspectionType: 'Sample검사',
    inspectionLevel: 'II',
    strictness: '보통검사',
    aql: 2.5,
    aqlAc: 0,
    aqlRe: 1,
    revisionDate: new Date().toISOString().split('T')[0],
    revisionNote: '최초 제정',
    confirmedBy: '',
  });

  return (
    <div className="modal-backdrop">
      <div className="modal">
        <h2>검사기준서 제정</h2>
        <form onSubmit={(e) => { e.preventDefault(); onSubmit(form); }}>
          <div className="field">
            <label>제정일</label>
            <input
              type="date"
              value={form.establishedAt}
              onChange={(e) => setForm({ ...form, establishedAt: e.target.value })}
              required
            />
          </div>
          <div className="field-row">
            <div className="field">
              <label>검사 방식</label>
              <input
                value={form.inspectionType}
                onChange={(e) => setForm({ ...form, inspectionType: e.target.value })}
              />
            </div>
            <div className="field">
              <label>검사 수준</label>
              <input
                value={form.inspectionLevel}
                onChange={(e) => setForm({ ...form, inspectionLevel: e.target.value })}
              />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label>엄격도</label>
              <input
                value={form.strictness}
                onChange={(e) => setForm({ ...form, strictness: e.target.value })}
              />
            </div>
            <div className="field">
              <label>AQL</label>
              <input
                type="number"
                step="0.1"
                value={form.aql}
                onChange={(e) => setForm({ ...form, aql: Number(e.target.value) })}
              />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label>Ac (합격수)</label>
              <input
                type="number"
                value={form.aqlAc ?? 0}
                onChange={(e) => setForm({ ...form, aqlAc: Number(e.target.value) })}
              />
            </div>
            <div className="field">
              <label>Re (불합격수)</label>
              <input
                type="number"
                value={form.aqlRe ?? 1}
                onChange={(e) => setForm({ ...form, aqlRe: Number(e.target.value) })}
              />
            </div>
          </div>
          <hr style={{ margin: '16px 0', borderColor: 'var(--border-color)' }} />
          <div className="field-row">
            <div className="field">
              <label>제정(개정)일</label>
              <input
                type="date"
                value={form.revisionDate}
                onChange={(e) => setForm({ ...form, revisionDate: e.target.value })}
                required
              />
            </div>
            <div className="field">
              <label>확인자</label>
              <input
                value={form.confirmedBy}
                onChange={(e) => setForm({ ...form, confirmedBy: e.target.value })}
              />
            </div>
          </div>
          <div className="field">
            <label>제정 사유</label>
            <textarea
              value={form.revisionNote}
              onChange={(e) => setForm({ ...form, revisionNote: e.target.value })}
              required
            />
          </div>
          {error && <div className="error">{error}</div>}
          <div className="row-end">
            <button type="button" onClick={onClose} disabled={isLoading}>취소</button>
            <button type="submit" className="primary" disabled={isLoading}>
              {isLoading ? '제정 중…' : '제정'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// 기준서 개정 모달 — 기존 헤더 값 pre-fill, updateStandard 호출
function StandardUpdateFormModal({
  standard,
  onClose,
  onSubmit,
  isLoading,
  error,
}: {
  standard: import('../inspection/types').InspectionStandard;
  onClose: () => void;
  onSubmit: (data: InspectionStandardUpdateRequest) => void;
  isLoading?: boolean;
  error?: string;
}) {
  const [form, setForm] = useState<InspectionStandardUpdateRequest>({
    establishedAt: standard.establishedAt,
    inspectionType: standard.inspectionType ?? '',
    inspectionLevel: standard.inspectionLevel ?? '',
    strictness: standard.strictness ?? '',
    aql: standard.aql ?? undefined,
    aqlAc: standard.aqlAc ?? undefined,
    aqlRe: standard.aqlRe ?? undefined,
    revisionDate: new Date().toISOString().split('T')[0],
    revisionNote: '',
    confirmedBy: '',
  });

  return (
    <div className="modal-backdrop">
      <div className="modal">
        <h2>검사기준서 개정</h2>
        <form onSubmit={(e) => { e.preventDefault(); onSubmit(form); }}>
          <div className="field">
            <label>제정일</label>
            <input
              type="date"
              value={form.establishedAt}
              onChange={(e) => setForm({ ...form, establishedAt: e.target.value })}
              required
            />
          </div>
          <div className="field-row">
            <div className="field">
              <label>검사 방식</label>
              <input
                value={form.inspectionType}
                onChange={(e) => setForm({ ...form, inspectionType: e.target.value })}
              />
            </div>
            <div className="field">
              <label>검사 수준</label>
              <input
                value={form.inspectionLevel}
                onChange={(e) => setForm({ ...form, inspectionLevel: e.target.value })}
              />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label>엄격도</label>
              <input
                value={form.strictness}
                onChange={(e) => setForm({ ...form, strictness: e.target.value })}
              />
            </div>
            <div className="field">
              <label>AQL</label>
              <input
                type="number"
                step="0.1"
                value={form.aql ?? ''}
                onChange={(e) => setForm({ ...form, aql: e.target.value ? Number(e.target.value) : undefined })}
              />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label>Ac (합격수)</label>
              <input
                type="number"
                value={form.aqlAc ?? ''}
                onChange={(e) => setForm({ ...form, aqlAc: e.target.value ? Number(e.target.value) : undefined })}
              />
            </div>
            <div className="field">
              <label>Re (불합격수)</label>
              <input
                type="number"
                value={form.aqlRe ?? ''}
                onChange={(e) => setForm({ ...form, aqlRe: e.target.value ? Number(e.target.value) : undefined })}
              />
            </div>
          </div>
          <hr style={{ margin: '16px 0', borderColor: 'var(--border-color)' }} />
          <div className="field-row">
            <div className="field">
              <label>개정일</label>
              <input
                type="date"
                value={form.revisionDate}
                onChange={(e) => setForm({ ...form, revisionDate: e.target.value })}
                required
              />
            </div>
            <div className="field">
              <label>확인자</label>
              <input
                value={form.confirmedBy}
                onChange={(e) => setForm({ ...form, confirmedBy: e.target.value })}
              />
            </div>
          </div>
          <div className="field">
            <label>개정 사유</label>
            <textarea
              value={form.revisionNote}
              onChange={(e) => setForm({ ...form, revisionNote: e.target.value })}
              required
            />
          </div>
          {error && <div className="error">{error}</div>}
          <div className="row-end">
            <button type="button" onClick={onClose} disabled={isLoading}>취소</button>
            <button type="submit" className="primary" disabled={isLoading}>
              {isLoading ? '개정 중…' : '개정'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
