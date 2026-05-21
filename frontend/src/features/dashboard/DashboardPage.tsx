import { useQuery } from '@tanstack/react-query';
import { dashboardApi } from '../production/api';
import { StatusBadge } from '../../shared/components/StatusBadge';

export function DashboardPage() {
  const today = new Date().toISOString().split('T')[0];

  const { data, isLoading } = useQuery({
    queryKey: ['dashboard', 'daily', today],
    queryFn: () => dashboardApi.getDaily(today),
  });

  if (isLoading) return <p className="empty">로딩 중…</p>;

  return (
    <>
      <div className="content-header">
        <h1>생산 현황판 ({data?.date})</h1>
      </div>

      <div className="card-grid">
        <Card label="총 목표 수량" value={data?.totalTargetQty ?? 0} />
        <Card label="총 합격 수량" value={data?.totalPassCount ?? 0} />
        <Card label="전체 합격률" value={`${(data?.overallPassRate ?? 0).toFixed(1)}%`} />
      </div>

      <div className="section" style={{ marginTop: 24 }}>
        <div className="section-title">모델별 생산 상세</div>
        {data?.byModel && data.byModel.length > 0 ? (
          <table>
            <thead>
              <tr>
                <th>모델명</th>
                <th>상태</th>
                <th>목표/합격</th>
                <th>진행중 Lot</th>
                <th>불합격 Lot</th>
                <th>검사 NG 횟수</th>
                <th>재검사 횟수</th>
                <th>합격률</th>
              </tr>
            </thead>
            <tbody>
              {data.byModel.map((m) => (
                <tr key={m.planId}>
                  <td>{m.modelName}</td>
                  <td><StatusBadge value={m.planStatus} /></td>
                  <td>{m.targetQty} / {m.passCount}</td>
                  <td>{m.inProgressCount}</td>
                  <td>{m.failCount}</td>
                  <td>{m.ngResultCount}</td>
                  <td>{m.recheckCount}</td>
                  <td>{m.passRate.toFixed(1)}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="empty">오늘 등록된 생산 계획이 없습니다.</p>
        )}
      </div>

      <div className="section" style={{ marginTop: 24 }}>
        <div className="section-title">시스템 안내</div>
        <p>본 시스템은 검사 기준서 전산화(Inspection Digitization)를 목표로 합니다.</p>
        <p>상단 메뉴에서 자재 및 검사기준을 관리하고, 생산 계획에 맞춰 실시간 검사 결과를 입력할 수 있습니다.</p>
      </div>
    </>
  );
}

function Card({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="card">
      <div className="card-label">{label}</div>
      <div className="card-value">{value}</div>
    </div>
  );
}
