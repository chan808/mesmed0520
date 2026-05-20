import { useQuery } from '@tanstack/react-query';
import { materialApi } from '../material/api';

export function DashboardPage() {
  const { data: materials } = useQuery({
    queryKey: ['materials'],
    queryFn: materialApi.list,
  });

  return (
    <>
      <div className="content-header">
        <h1>대시보드</h1>
      </div>

      <div className="card-grid">
        <Card label="등록된 자재" value={materials?.length ?? 0} />
      </div>

      <div className="section" style={{ marginTop: 24 }}>
        <div className="section-title">시스템 안내</div>
        <p>본 시스템은 검사 기준서 전산화(Inspection Digitization)를 목표로 합니다.</p>
        <p>상단 '자재 관리' 탭에서 자재를 등록하고, 각 자재별 검사 기준을 관리할 수 있습니다.</p>
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
