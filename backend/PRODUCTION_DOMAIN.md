# Production 도메인 구현 설명

생산 실행 + 수입검사 결과 수집 + 대시보드 집계 기능.

---

## 1. 해결한 문제

기존 코드: 품목(Material)별로 검사 기준서(InspectionStandard)와 검사항목(InspectionItem)을 정의하는 것까지만 구현.
→ "실제로 오늘 몇 개를 생산했고, 각 품목을 검사했더니 결과가 어땠는가"를 기록할 구조가 없었음.

이 도메인은 그 **실행 결과**를 기록한다.

---

## 2. 핵심 개념 (도메인 용어)

| 용어 | 의미 |
|------|------|
| **ProductModel** | 최종 생산품 (예: ER-2000 SMART, BP-3000) |
| **ProductionPlan** | "오늘 ER-2000 SMART를 50개 만들겠다"는 일일 계획 |
| **ProductionLot** | 그 계획 안의 개별 생산 단위 1개. lot 1번, 2번... |
| **LotInspectionResult** | lot 안에서 특정 InspectionItem을 검사한 결과 1건 (PASS / NG) |

**판정 계층:**

```
InspectionItem 결과 → 품목(Material) 합불 → Lot 합불 → Plan 달성률
```

- InspectionItem 하나라도 NG → 해당 품목 NG
- 품목 하나라도 NG → Lot는 IN_PROGRESS (재검사 대기)
- 모든 품목의 모든 InspectionItem 최신 결과 = PASS → Lot PASS
- Lot PASS → Plan.passCount++

---

## 3. 엔티티 구조 및 관계

```
ProductModel (id, name, description)
      ↓ 1:N  [Material.productModel FK]
Material (기존 엔티티, productModel 컬럼 추가됨)
      ↓ 1:N
InspectionStandard → InspectionItem  (기존, 변경 없음)

ProductModel
      ↓ 1:N
ProductionPlan (model, planDate, targetQty, passCount, status)
      ↓ 1:N
ProductionLot (plan, lotNo, status)
      ↓ 1:N
LotInspectionResult (lot, inspectionItem, round, result, inspectedAt, memo)
```

### ProductModel

```java
// product_model 테이블
String name;         // 모델명 (unique 권고)
String description;  // 설명
```

### ProductionPlan

```java
// production_plan 테이블
ProductModel model;
LocalDate planDate;    // 생산 목표 날짜
int targetQty;         // 목표 수량
int passCount;         // 현재까지 PASS된 lot 수 (lot.pass() 시 자동 증가)
PlanStatus status;     // PLANNED → IN_PROGRESS → COMPLETED
```

### ProductionLot

```java
// production_lot 테이블
ProductionPlan plan;
int lotNo;          // 계획 내 순번 (1, 2, 3...)
LotStatus status;   // IN_PROGRESS → PASS 또는 FAIL
```

### LotInspectionResult

```java
// lot_inspection_result 테이블
ProductionLot lot;
InspectionItem inspectionItem;  // 기존 InspectionItem 참조
int round;                       // 재검사 횟수. 첫 검사=1, 재검사=2, 3...
InspectionResultCode result;     // PASS 또는 NG
LocalDateTime inspectedAt;       // 검사 시각 (자동 기록)
String memo;                     // 비고 (선택)
```

**round 동작 방식:**
같은 lot의 같은 InspectionItem에 결과를 제출할 때마다 round가 1씩 증가한다.
"최신 결과"는 항상 가장 높은 round의 결과.

---

## 4. Enum

| Enum | 값 | 설명 |
|------|----|------|
| `PlanStatus` | PLANNED / IN_PROGRESS / COMPLETED | 계획 상태 |
| `LotStatus` | IN_PROGRESS / PASS / FAIL | lot 상태 |
| `InspectionResultCode` | PASS / NG | 검사 결과 |

---

## 5. 기존 코드 변경 사항

### Material 엔티티 (material/entity/Material.java)

```java
// 추가된 필드
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "product_model_id")   // nullable — 기준서만 등록된 품목 허용
private ProductModel productModel;

// 추가된 메서드
public void assignModel(ProductModel model) { ... }
```

기존 builder는 건드리지 않음. 모델 배정은 `assignModel()`로만.

### MaterialRepository

```java
// 추가
List<Material> findByProductModelIdAndDeletedAtIsNull(Long productModelId);
```

소프트딜리트된 품목은 생산 검사 대상에서 제외.

### InspectionStandardRepository

```java
// 추가
Optional<InspectionStandard> findTopByMaterialIdOrderByRevDesc(Long materialId);
```

품목에 기준서가 여러 개(개정 이력)일 수 있으므로, 검사 실행 시에는 항상 최신 rev를 사용.

---

## 6. 핵심 로직: 검사 결과 제출 흐름

`ProductionService.submitResult()` 내부 동작:

```
1. lot 존재 확인 + 상태가 IN_PROGRESS인지 확인
   (PASS/FAIL인 lot에는 결과 제출 불가)

2. InspectionItem 존재 확인

3. 이 item의 Material.productModel == lot의 모델인지 확인
   (다른 모델 품목 항목을 이 lot에 제출하는 걸 막음)

4. round = 이 (lot, item) 조합의 기존 결과 수 + 1

5. LotInspectionResult 저장

6. 결과가 PASS인 경우에만 전체 통과 여부 재평가:
   evaluateAndUpdateLotStatus()
   - 이 모델의 모든 품목(Material) 순회
   - 각 품목의 최신 기준서에 속한 모든 InspectionItem 순회
   - 각 item의 최신 round 결과가 PASS인지 확인
   - 하나라도 미검사/NG → return (lot 그대로 IN_PROGRESS)
   - 전부 PASS → lot.pass(), plan.passCount++

NG를 제출할 때는 평가하지 않음 (어차피 PASS가 될 수 없으므로).
```

---

## 7. API 목록

### 생산 모델 (`/api/models`)

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| POST | `/api/models` | 모델 등록 | 필요 |
| GET | `/api/models` | 모델 전체 목록 | 필요 |
| GET | `/api/models/{id}/materials` | 모델에 배정된 품목 목록 | 필요 |

**POST /api/models 요청 예시:**
```json
{
  "name": "ER-2000 SMART",
  "description": "스마트 응급처치 모니터"
}
```

### 생산 계획·실행 (`/api/production`)

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/production/plans` | 일일 계획 등록 (날짜+모델 중복 불가) |
| GET | `/api/production/plans?date=2026-05-20` | 특정 날짜 계획 목록 |
| GET | `/api/production/plans/{id}` | 계획 단건 조회 |
| POST | `/api/production/plans/{planId}/lots` | lot 생산 시작 (lotNo 자동 부여) |
| GET | `/api/production/lots/{lotId}` | lot 상세 + 전체 검사 현황 |
| POST | `/api/production/lots/{lotId}/results` | 검사 결과 제출 |
| PATCH | `/api/production/lots/{lotId}/fail` | 수동 불합격 처리 |

**POST /api/production/plans 요청 예시:**
```json
{
  "modelId": 1,
  "planDate": "2026-05-20",
  "targetQty": 50
}
```

**POST /api/production/lots/{lotId}/results 요청 예시:**
```json
{
  "inspectionItemId": 3,
  "result": "NG",
  "memo": "DOT 깨짐 2개 발견"
}
```

**응답: LotDetailResponse 구조**
```json
{
  "lotId": 1,
  "lotNo": 1,
  "status": "IN_PROGRESS",
  "planId": 1,
  "planDate": "2026-05-20",
  "modelName": "ER-2000 SMART",
  "materials": [
    {
      "materialId": 1,
      "partName": "LCD",
      "partCode": "10018500701",
      "materialResult": "NG",        // null=미검사, PASS, NG
      "items": [
        {
          "itemId": 1,
          "itemName": "DOT 깨짐",
          "specification": "DOT깨짐이 0일 것",
          "currentResult": "NG",     // 최신 결과
          "latestRound": 1,
          "rounds": [
            { "round": 1, "result": "NG", "inspectedAt": "2026-05-20T10:00:00", "memo": "..." }
          ]
        }
      ]
    }
  ]
}
```

### 대시보드 (`/api/dashboard`)

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/dashboard/daily` | 오늘 요약 (date 생략 시 오늘) |
| GET | `/api/dashboard/daily?date=2026-05-19` | 특정 날짜 요약 |

**응답: DailyDashboardResponse 구조**
```json
{
  "date": "2026-05-20",
  "totalTargetQty": 80,
  "totalPassCount": 12,
  "overallPassRate": 15.0,
  "byModel": [
    {
      "planId": 1,
      "modelId": 1,
      "modelName": "ER-2000 SMART",
      "targetQty": 50,
      "passCount": 8,
      "lotCount": 10,
      "inProgressCount": 2,
      "failCount": 0,
      "ngResultCount": 5,     // 오늘 이 모델에서 발생한 NG 판정 총 건수
      "recheckCount": 3,      // 재검사 횟수 (round > 1인 결과 수)
      "passRate": 16.0,
      "planStatus": "IN_PROGRESS"
    }
  ]
}
```

---

## 8. 시드 데이터 (DataInitializer)

서버 최초 기동 시 자동 등록. `modelRepository.existsByName("ER-2000 SMART")`로 중복 방지.

```
생산 모델
├── ER-2000 SMART (스마트 응급처치 모니터)
│   ├── LCD          partCode: 10018500701  → 검사항목: DOT깨짐, FILM CABLE, BLACK LIGHT
│   ├── 버튼 스위치   partCode: BTN-SW-001   → 검사항목: 외관검사, 클릭감 확인
│   └── 배터리팩      partCode: BAT-PACK-001  → 검사항목: 전압측정, 외관검사
│
└── BP-3000 (자동 혈압계)
    ├── 압력센서모듈   partCode: SEN-BP-001   → 검사항목: 압력정확도, 외관검사
    ├── LCD B         partCode: LCD-B-001    → 검사항목: DOT깨짐, 휘도 균일성
    └── 하우징         partCode: HOU-BP-001   → 검사항목: 치수검사, 외관검사

오늘 날짜 생산 계획
├── ER-2000 SMART: 목표 50개, lot 3개 생성 (IN_PROGRESS)
└── BP-3000: 목표 30개, lot 2개 생성 (IN_PROGRESS)
```

---

## 9. 재검사 시나리오 예시

**상황:** BP-3000 lot 1번. 압력센서모듈의 "압력정확도" 항목에서 NG 발생.

```
# 1차 검사 (round=1)
POST /api/production/lots/5/results
{ "inspectionItemId": 10, "result": "NG", "memo": "±4mmHg 초과" }
→ lot 여전히 IN_PROGRESS

# 재검사 (round=2)
POST /api/production/lots/5/results
{ "inspectionItemId": 10, "result": "PASS", "memo": "재측정 통과" }
→ 서버가 모든 item 최신 결과 확인
→ 전부 PASS이면 lot.status = PASS, plan.passCount++
→ 아직 다른 item이 미검사면 IN_PROGRESS 유지
```

---

## 10. 패키지 위치

```
src/main/java/com/chan/med0515/
└── production/
    ├── controller/
    │   ├── ProductModelController.java   GET/POST /api/models
    │   ├── ProductionController.java     /api/production/**
    │   └── DashboardController.java     /api/dashboard/**
    ├── dto/
    │   ├── ProductModelRequest/Response.java
    │   ├── ProductionPlanRequest/Response.java
    │   ├── LotDetailResponse.java        (중첩 record: MaterialResult, ItemResult, RoundResult)
    │   ├── InspectionResultRequest/Response.java
    │   └── DailyDashboardResponse.java   (중첩 record: ModelSummary)
    ├── entity/
    │   ├── ProductModel.java
    │   ├── ProductionPlan.java
    │   ├── ProductionLot.java
    │   └── LotInspectionResult.java
    ├── enums/
    │   ├── PlanStatus.java
    │   ├── LotStatus.java
    │   └── InspectionResultCode.java
    ├── error/
    │   └── ProductionErrorCode.java
    ├── repository/
    │   ├── ProductModelRepository.java
    │   ├── ProductionPlanRepository.java
    │   ├── ProductionLotRepository.java
    │   └── LotInspectionResultRepository.java  (대시보드 집계 JPQL 포함)
    └── service/
        ├── ProductModelService.java      모델 CRUD
        ├── ProductionPlanService.java    계획 등록/조회
        ├── ProductionService.java        lot 생성 + 결과 제출 + 판정 핵심 로직
        └── DashboardService.java         일별 집계
```