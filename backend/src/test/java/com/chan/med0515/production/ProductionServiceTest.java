package com.chan.med0515.production;

import com.chan.med0515.global.error.BusinessException;
import com.chan.med0515.inspection.entity.InspectionItem;
import com.chan.med0515.inspection.entity.InspectionStandard;
import com.chan.med0515.inspection.repository.InspectionItemRepository;
import com.chan.med0515.inspection.repository.InspectionStandardRepository;
import com.chan.med0515.material.entity.Material;
import com.chan.med0515.material.repository.MaterialRepository;
import com.chan.med0515.production.dto.InspectionResultRequest;
import com.chan.med0515.production.dto.InspectionResultResponse;
import com.chan.med0515.production.dto.LotDetailResponse;
import com.chan.med0515.production.entity.ProductionPlan;
import com.chan.med0515.production.enums.InspectionResultCode;
import com.chan.med0515.production.enums.LotStatus;
import com.chan.med0515.production.enums.PlanStatus;
import com.chan.med0515.production.repository.ProductionPlanRepository;
import com.chan.med0515.production.service.ProductionService;
import org.junit.jupiter.api.BeforeEach;
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
class ProductionServiceTest {

    @Autowired ProductionService productionService;
    @Autowired MaterialRepository materialRepository;
    @Autowired InspectionStandardRepository standardRepository;
    @Autowired InspectionItemRepository itemRepository;
    @Autowired ProductionPlanRepository planRepository;

    private static final LocalDate TEST_DATE = LocalDate.of(2024, 1, 1);

    private InspectionItem item1;
    private InspectionItem item2;
    private ProductionPlan plan;

    @BeforeEach
    void setUp() {
        String modelName = "테스트모델-" + System.nanoTime();

        Material material = materialRepository.save(Material.builder()
                .modelName(modelName)
                .partName("테스트부품")
                .partCode("TEST-" + System.nanoTime())
                .build());

        InspectionStandard std = standardRepository.save(InspectionStandard.builder()
                .material(material)
                .rev(0)
                .establishedAt(TEST_DATE)
                .inspectionType("테스트")
                .inspectionLevel("I")
                .strictness("보통")
                .aql(new BigDecimal("1.0"))
                .aqlAc(0)
                .aqlRe(1)
                .build());

        item1 = itemRepository.save(InspectionItem.builder()
                .standard(std).itemName("항목1").specification("기준1")
                .method("육안").equipment("육안확인").timing("입고시").addedAtRev(0).build());

        item2 = itemRepository.save(InspectionItem.builder()
                .standard(std).itemName("항목2").specification("기준2")
                .method("측정").equipment("버니어").timing("입고시").addedAtRev(0).build());

        plan = planRepository.save(ProductionPlan.builder()
                .modelName(modelName).planDate(TEST_DATE).targetQty(10).build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // lot 생성
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("lot 생성 시 lotNo가 계획 내에서 순차적으로 증가한다")
    void startLot_lotNumberIncrementsSequentially() {
        LotDetailResponse lot1 = productionService.startLot(plan.getId());
        LotDetailResponse lot2 = productionService.startLot(plan.getId());
        LotDetailResponse lot3 = productionService.startLot(plan.getId());

        assertThat(lot1.lotNo()).isEqualTo(1);
        assertThat(lot2.lotNo()).isEqualTo(2);
        assertThat(lot3.lotNo()).isEqualTo(3);
    }

    @Test
    @DisplayName("lot 생성 시 PLANNED 상태의 plan이 IN_PROGRESS로 변경된다")
    void startLot_changesPlannedStatusToInProgress() {
        productionService.startLot(plan.getId());

        ProductionPlan updated = planRepository.findById(plan.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PlanStatus.IN_PROGRESS);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 검사 결과 제출
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NG 제출 시 lot은 IN_PROGRESS를 유지한다")
    void submitResult_NG_lotRemainsInProgress() {
        LotDetailResponse lot = productionService.startLot(plan.getId());

        InspectionResultResponse result = productionService.submitResult(
                lot.lotId(),
                new InspectionResultRequest(item1.getId(), InspectionResultCode.NG, "이물질 발견")
        );

        assertThat(result.result()).isEqualTo(InspectionResultCode.NG);
        assertThat(result.round()).isEqualTo(1);
        assertThat(result.lotStatus()).isEqualTo(LotStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("일부 항목만 PASS 시 나머지 미검사이면 lot은 IN_PROGRESS를 유지한다")
    void submitResult_partialPass_lotRemainsInProgress() {
        LotDetailResponse lot = productionService.startLot(plan.getId());

        InspectionResultResponse result = productionService.submitResult(
                lot.lotId(),
                new InspectionResultRequest(item1.getId(), InspectionResultCode.PASS, null)
        );

        assertThat(result.lotStatus()).isEqualTo(LotStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("모든 항목 PASS 시 lot이 PASS되고 plan.passCount가 1 증가한다")
    void submitResult_allItemsPass_lotPassesAndPlanCountIncrements() {
        LotDetailResponse lot = productionService.startLot(plan.getId());
        Long lotId = lot.lotId();

        productionService.submitResult(lotId,
                new InspectionResultRequest(item1.getId(), InspectionResultCode.PASS, null));
        InspectionResultResponse last = productionService.submitResult(lotId,
                new InspectionResultRequest(item2.getId(), InspectionResultCode.PASS, null));

        assertThat(last.lotStatus()).isEqualTo(LotStatus.PASS);

        ProductionPlan updated = planRepository.findById(plan.getId()).orElseThrow();
        assertThat(updated.getPassCount()).isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 재검사
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("NG 후 재검사에서 PASS 시 round가 2로 기록되고 lot이 PASS된다")
    void submitResult_recheckAfterNG_roundIncreasesAndLotPasses() {
        LotDetailResponse lot = productionService.startLot(plan.getId());
        Long lotId = lot.lotId();

        productionService.submitResult(lotId,
                new InspectionResultRequest(item1.getId(), InspectionResultCode.NG, "1차 실패"));
        productionService.submitResult(lotId,
                new InspectionResultRequest(item1.getId(), InspectionResultCode.PASS, "재검사 통과"));
        InspectionResultResponse last = productionService.submitResult(lotId,
                new InspectionResultRequest(item2.getId(), InspectionResultCode.PASS, null));

        assertThat(last.lotStatus()).isEqualTo(LotStatus.PASS);

        LotDetailResponse detail = productionService.getLotDetail(lotId);
        LotDetailResponse.ItemResult item1Detail = detail.materials().stream()
                .flatMap(m -> m.items().stream())
                .filter(i -> i.itemId().equals(item1.getId()))
                .findFirst().orElseThrow();

        assertThat(item1Detail.rounds()).hasSize(2);
        assertThat(item1Detail.rounds().get(0).result()).isEqualTo(InspectionResultCode.NG);
        assertThat(item1Detail.rounds().get(1).result()).isEqualTo(InspectionResultCode.PASS);
        assertThat(item1Detail.latestRound()).isEqualTo(2);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 예외
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("이미 PASS된 lot에 결과를 제출하면 예외가 발생한다")
    void submitResult_toPassedLot_throwsException() {
        LotDetailResponse lot = productionService.startLot(plan.getId());
        Long lotId = lot.lotId();

        productionService.submitResult(lotId,
                new InspectionResultRequest(item1.getId(), InspectionResultCode.PASS, null));
        productionService.submitResult(lotId,
                new InspectionResultRequest(item2.getId(), InspectionResultCode.PASS, null));

        assertThatThrownBy(() -> productionService.submitResult(lotId,
                new InspectionResultRequest(item1.getId(), InspectionResultCode.PASS, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("완료된 lot");
    }

    @Test
    @DisplayName("이 lot의 모델에 속하지 않는 검사항목 제출 시 예외가 발생한다")
    void submitResult_itemFromDifferentModel_throwsException() {
        Material otherMaterial = materialRepository.save(Material.builder()
                .modelName("다른모델-" + System.nanoTime())
                .partName("다른부품")
                .partCode("OTHER-" + System.nanoTime())
                .build());

        InspectionStandard otherStd = standardRepository.save(InspectionStandard.builder()
                .material(otherMaterial).rev(0).establishedAt(TEST_DATE)
                .inspectionType("테스트").inspectionLevel("I").strictness("보통")
                .aql(new BigDecimal("1.0")).aqlAc(0).aqlRe(1).build());
        InspectionItem otherItem = itemRepository.save(InspectionItem.builder()
                .standard(otherStd).itemName("다른항목").specification("기준")
                .method("육안").equipment("육안").timing("입고시").addedAtRev(0).build());

        LotDetailResponse lot = productionService.startLot(plan.getId());

        assertThatThrownBy(() -> productionService.submitResult(lot.lotId(),
                new InspectionResultRequest(otherItem.getId(), InspectionResultCode.PASS, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("속하지 않는");
    }
}
