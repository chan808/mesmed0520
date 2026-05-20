package com.chan.med0515.production;

import com.chan.med0515.inspection.entity.InspectionItem;
import com.chan.med0515.inspection.entity.InspectionStandard;
import com.chan.med0515.inspection.repository.InspectionItemRepository;
import com.chan.med0515.inspection.repository.InspectionStandardRepository;
import com.chan.med0515.material.entity.Material;
import com.chan.med0515.material.repository.MaterialRepository;
import com.chan.med0515.production.dto.DailyDashboardResponse;
import com.chan.med0515.production.entity.*;
import com.chan.med0515.production.enums.InspectionResultCode;
import com.chan.med0515.production.repository.*;
import com.chan.med0515.production.service.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class DashboardServiceTest {

    @Autowired DashboardService dashboardService;
    @Autowired ProductModelRepository modelRepository;
    @Autowired MaterialRepository materialRepository;
    @Autowired InspectionStandardRepository standardRepository;
    @Autowired InspectionItemRepository itemRepository;
    @Autowired ProductionPlanRepository planRepository;
    @Autowired ProductionLotRepository lotRepository;
    @Autowired LotInspectionResultRepository resultRepository;

    // 오늘 날짜로 생성된 시드 데이터와 충돌하지 않는 날짜
    private static final LocalDate TEST_DATE = LocalDate.of(2024, 6, 1);

    @Test
    @DisplayName("해당 날짜에 계획이 없으면 빈 목록과 0 집계를 반환한다")
    void getDailySummary_noPlans_returnsEmpty() {
        DailyDashboardResponse result = dashboardService.getDailySummary(LocalDate.of(2000, 1, 1));

        assertThat(result.byModel()).isEmpty();
        assertThat(result.totalTargetQty()).isZero();
        assertThat(result.totalPassCount()).isZero();
    }

    @Test
    @DisplayName("NG, 재검사, 통과 데이터가 있을 때 집계 수치가 정확히 계산된다")
    void getDailySummary_withResults_correctAggregation() {
        // 테스트 전용 모델 + 품목 + 항목 세팅
        ProductModel model = modelRepository.save(ProductModel.builder()
                .name("대시보드테스트-" + System.nanoTime()).build());

        Material material = Material.builder()
                .modelName(model.getName())
                .partName("부품A")
                .partCode("DASH-" + System.nanoTime())
                .build();
        material.assignModel(model);
        material = materialRepository.save(material);

        InspectionStandard std = standardRepository.save(InspectionStandard.builder()
                .material(material).rev(0).establishedAt(TEST_DATE)
                .inspectionType("테스트").inspectionLevel("I").strictness("보통")
                .aql(new BigDecimal("1.0")).aqlAc(0).aqlRe(1).build());

        InspectionItem item = itemRepository.save(InspectionItem.builder()
                .standard(std).itemName("항목1").specification("기준")
                .method("육안").equipment("육안").timing("입고시").addedAtRev(0).build());

        // 계획: 목표 5개
        ProductionPlan plan = planRepository.save(ProductionPlan.builder()
                .model(model).planDate(TEST_DATE).targetQty(5).build());
        plan.startIfPlanned();

        // lot1: NG(round=1) → PASS(round=2 재검사) → lot PASS 처리
        ProductionLot lot1 = lotRepository.save(
                ProductionLot.builder().plan(plan).lotNo(1).build());
        resultRepository.save(LotInspectionResult.builder()
                .lot(lot1).inspectionItem(item).round(1)
                .result(InspectionResultCode.NG).memo("1차 실패").build());
        resultRepository.save(LotInspectionResult.builder()
                .lot(lot1).inspectionItem(item).round(2)
                .result(InspectionResultCode.PASS).memo(null).build());
        lot1.pass();
        plan.incrementPassCount();

        // lot2: NG(round=1) — 미통과, IN_PROGRESS 유지
        ProductionLot lot2 = lotRepository.save(
                ProductionLot.builder().plan(plan).lotNo(2).build());
        resultRepository.save(LotInspectionResult.builder()
                .lot(lot2).inspectionItem(item).round(1)
                .result(InspectionResultCode.NG).memo("이물질").build());

        // 대시보드 조회
        DailyDashboardResponse response = dashboardService.getDailySummary(TEST_DATE);

        DailyDashboardResponse.ModelSummary summary = response.byModel().stream()
                .filter(s -> s.modelId().equals(model.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("해당 모델 요약 없음"));

        assertThat(summary.targetQty()).isEqualTo(5);
        assertThat(summary.passCount()).isEqualTo(1);        // lot1만 통과
        assertThat(summary.lotCount()).isEqualTo(2);
        assertThat(summary.inProgressCount()).isEqualTo(1);  // lot2 진행중
        assertThat(summary.failCount()).isEqualTo(0);
        assertThat(summary.ngResultCount()).isEqualTo(2);    // lot1 round1 + lot2 round1
        assertThat(summary.recheckCount()).isEqualTo(1);     // lot1 round=2 결과 1건
        assertThat(summary.passRate()).isEqualTo(20.0);      // 1/5 = 20%
    }
}