# Backend Entity Relationship Diagram (DBML)

이 문서는 `dbdiagram.io`에서 사용할 수 있는 DBML 코드입니다.

```dbml
// --- Enums ---
Enum UserRole {
  ADMIN
  INSPECTOR
  OPERATOR
}

Enum MeasurementType {
  VISUAL
  NUMERIC
}

Enum PlanStatus {
  PLANNED
  IN_PROGRESS
  COMPLETED
}

Enum LotStatus {
  IN_PROGRESS
  PASS
  FAIL
}

Enum InspectionResultCode {
  PASS
  NG
}

// --- Tables ---

Table users {
  id bigint [pk, increment]
  username varchar(50) [unique, not null]
  password varchar(255) [not null]
  display_name varchar(100)
  role UserRole
  created_at timestamp
  updated_at timestamp
}

Table material {
  id bigint [pk, increment]
  model_name varchar(50) [not null]
  part_name varchar(50) [not null]
  part_code varchar(50)
  supplier varchar(50)
  material_spec varchar(20)
  deleted_at timestamp
  created_at timestamp
  updated_at timestamp
}

Table inspection_standard {
  id bigint [pk, increment]
  material_id bigint [not null]
  rev int [not null]
  established_at date [not null]
  inspection_type varchar(255)
  inspection_level varchar(255)
  strictness varchar(255)
  aql decimal(10,4)
  aql_ac int
  aql_re int
  created_at timestamp
  updated_at timestamp
}

Table inspection_item {
  id bigint [pk, increment]
  standard_id bigint [not null]
  item_name varchar(20) [not null]
  specification varchar(100)
  method varchar(20)
  equipment varchar(20)
  timing varchar(20)
  measurement_type MeasurementType [not null]
  min_value decimal(10,4)
  max_value decimal(10,4)
  unit varchar(20)
  added_at_rev int [not null]
  deleted_at_rev int
  created_at timestamp
  updated_at timestamp
}

Table revisionHistory {
  id bigint [pk, increment]
  standard_id bigint [not null]
  rev int [not null]
  revision_date date [not null]
  revision_note varchar(100) [not null]
  confirmed_by varchar(20)
  created_at timestamp
  updated_at timestamp
}

Table production_plan {
  id bigint [pk, increment]
  model_name varchar(50) [not null]
  plan_date date [not null]
  target_qty int [not null]
  pass_count int [not null]
  fail_count int [not null]
  status PlanStatus [not null]
  created_at timestamp
  updated_at timestamp
}

Table production_lot {
  id bigint [pk, increment]
  plan_id bigint [not null]
  lot_no int [not null]
  status LotStatus [not null]
  created_at timestamp
  updated_at timestamp
}

Table lot_inspection_result {
  id bigint [pk, increment]
  lot_id bigint [not null]
  inspection_item_id bigint [not null]
  round int [not null]
  result InspectionResultCode [not null]
  measured_value decimal(10,4)
  inspected_at timestamp [not null]
  memo varchar(200)
  created_at timestamp
  updated_at timestamp
}

// --- Relationships ---

Ref: inspection_standard.material_id > material.id
Ref: inspection_item.standard_id > inspection_standard.id
Ref: revisionHistory.standard_id > inspection_standard.id
Ref: production_lot.plan_id > production_plan.id
Ref: lot_inspection_result.lot_id > production_lot.id
Ref: lot_inspection_result.inspection_item_id > inspection_item.id
```
