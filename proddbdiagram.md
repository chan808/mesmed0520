현대 MES는 실시간 제어는 엣지(온프레미스)에 두고, 무거운 분석과 장기 저장은 클라우드로 오프로드하는 하이브리드 구조가 표준이 돼가고 있어. Wikipedia



진짜 공부해야 할 패턴들

1\. Event Sourcing (가장 중요)

Event Sourcing은 데이터의 최종 상태만 저장하는 게 아니라, 모든 변환 이력을 통째로 저장하는 방식이야. append-only 로그 구조라 병렬 처리와 스트리밍 데이터에 잘 맞아. Upsolver

MES에서 왜 중요하냐면 — 불량 발생 시 "이 제품이 어떤 공정을 거쳤는지" 재현해야 하는 Traceability가 필수이기 때문이야. 상태만 저장하면 이게 불가능해.

2\. CQRS (Command Query Responsibility Segregation)

이벤트 발생 시 하나 이상의 읽기 모델을 업데이트할 수 있어서, 쓰기 쪽은 이력 캡처에 집중하고 읽기 쪽은 조회에 최적화된 뷰를 독립적으로 제공할 수 있어. Mia-Platform

공장 현장에서는 실시간 대시보드(읽기)와 공정 기록(쓰기)의 패턴이 완전히 달라서 이게 딱 맞아.

완전한 감사 추적이 필요하고, 이벤트 재현이 필요하며, 읽기/쓰기 패턴이 극단적으로 다른 시스템이라면 CQRS + Event Sourcing은 강력한 선택이야. Systemsarchitect

3\. ISA-95 표준

ISA-95 표준 기반 MES 플랫폼은 ERP 시스템과 연결되고, 장기적으로 안정적인 아키텍처 기반을 제공해. Cerexio

MES는 도메인 표준이 있는 영역이라, 이 표준을 모르면 팀 회의에서 말이 안 통해. 꼭 훑어봐.

4\. Time Series DB

공정 데이터(온도, 압력, 속도 등)는 관계형 DB나 JSONB로 처리하면 금방 한계가 와. InfluxDB, TimescaleDB 같은 시계열 DB가 이 영역을 담당해.



신입 공부 우선순위 제안

1순위 — ISA-95 훑어보기 (팀 언어 이해)

2순위 — Event Sourcing 개념 (MES의 근간)

3순위 — CQRS 패턴 (읽기/쓰기 분리)

4순위 — Time Series DB 기초

5순위 — JSONB/NoSQL (그다음 선택지)

JSONB나 NoSQL이 나쁜 건 아닌데, MES에서는 "유연한 속성 저장" 문제보다 "이력 추적과 실시간 처리" 문제가 훨씬 크게 다가오거든. 우선순위가 달라.

