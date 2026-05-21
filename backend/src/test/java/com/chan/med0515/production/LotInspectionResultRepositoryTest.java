package com.chan.med0515.production;

import com.chan.med0515.inspection.entity.InspectionItem;
import com.chan.med0515.inspection.entity.InspectionStandard;
import com.chan.med0515.inspection.repository.InspectionItemRepository;
import com.chan.med0515.inspection.repository.InspectionStandardRepository;
import com.chan.med0515.material.entity.Material;
import com.chan.med0515.material.repository.MaterialRepository;
import com.chan.med0515.production.entity.*;
import com.chan.med0515.production.enums.InspectionResultCode;
import com.chan.med0515.production.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * 대시보드 집계 JPQL 쿼리를 DB와 직접 검증한다.
 * @DataJpaTest = JPA 레이어만 로드, DataInitializer 없음, H2 사용
 */
@DataJpaTest
@ActiveProfiles("test")
class LotInspectionResultRepositoryTest {

    @Autowired LotInspectionResultRepository resultRepository;
    @Autowired MaterialRepository materialRepository;
    @Autowired InspectionStandardRepository standardRepository;
    @Autowired InspectionItemRepository itemRepository;
    @Autowired ProductionPlanRepository planRepository;
    @Autowired ProductionLotRepository lotRepository;

    private static final LocalDate TEST_DATE = LocalDate.of(2024, 1, 1);
    private static final String MODEL_NAME = "레포테스트모델";

    private InspectionItem item;
    private ProductionLot lot;

    @BeforeEach
    void setUp() {
        Material material = materialRepository.save(Material.builder()
                .modelName(MODEL_NAME)
                .partName("테스트부품")
                .partCode("REPO-001")
                .build());

        InspectionStandard std = standardRepository.save(InspectionStandard.builder()
                .material(material).rev(0).establishedAt(TEST_DATE)
                .inspectionType("테스트").inspectionLevel("I").strictness("보통")
                .aql(new BigDecimal("1.0")).aqlAc(0).aqlRe(1)
                .build());

        item = itemRepository.save(InspectionItem.builder()
                .standard(std).itemName("항목1").specification("기준")
                .method("육안").equipment("육안").timing("입고시").addedAtRev(0)
                .build());

        ProductionPlan plan = planRepository.save(ProductionPlan.builder()
                .modelName(MODEL_NAME).planDate(TEST_DATE).targetQty(10)
                .build());
        plan.startIfPlanned();

        lot = lotRepository.save(ProductionLot.builder()
                .plan(plan).lotNo(1)
                .build());
    }

    @Test
    @DisplayName("특정 날짜·모델의 NG 결과 수를 정확히 집계한다")
    void countByPlanDateAndModelNameAndResult_countNGCorrectly() {
        resultRepository.save(LotInspectionResult.builder()
                .lot(lot).inspectionItem(item).round(1)
                .result(InspectionResultCode.PASS).memo(null).build());
        resultRepository.save(LotInspectionResult.builder()
                .lot(lot).inspectionItem(item).round(2)
                .result(InspectionResultCode.NG).memo(null).build());
        resultRepository.save(LotInspectionResult.builder()
                .lot(lot).inspectionItem(item).round(3)
                .result(InspectionResultCode.NG).memo(null).build());

        long ngCount = resultRepository.countByPlanDateAndModelNameAndResult(
                TEST_DATE, MODEL_NAME, InspectionResultCode.NG);

        assertThat(ngCount).isEqualTo(2);
    }

    @Test
    @DisplayName("재검사 횟수(round > 1)를 정확히 집계한다")
    void countRechecksByPlanDateAndModelName_countRound2Plus() {
        resultRepository.save(LotInspectionResult.builder()
                .lot(lot).inspectionItem(item).round(1)
                .result(InspectionResultCode.NG).memo(null).build());
        resultRepository.save(LotInspectionResult.builder()
                .lot(lot).inspectionItem(item).round(2)
                .result(InspectionResultCode.NG).memo(null).build());
        resultRepository.save(LotInspectionResult.builder()
                .lot(lot).inspectionItem(item).round(3)
                .result(InspectionResultCode.PASS).memo(null).build());

        long recheckCount = resultRepository.countRechecksByPlanDateAndModelName(
                TEST_DATE, MODEL_NAME);

        assertThat(recheckCount).isEqualTo(2);
    }

    @Test
    @DisplayName("특정 lot·항목의 가장 최신(round 가장 높은) 결과를 조회한다")
    void findTopByLotIdAndInspectionItemIdOrderByRoundDesc_returnsLatest() {
        resultRepository.save(LotInspectionResult.builder()
                .lot(lot).inspectionItem(item).round(1)
                .result(InspectionResultCode.NG).memo(null).build());
        resultRepository.save(LotInspectionResult.builder()
                .lot(lot).inspectionItem(item).round(2)
                .result(InspectionResultCode.PASS).memo(null).build());

        Optional<LotInspectionResult> latest = resultRepository
                .findTopByLotIdAndInspectionItemIdOrderByRoundDesc(lot.getId(), item.getId());

        assertThat(latest).isPresent();
        assertThat(latest.get().getRound()).isEqualTo(2);
        assertThat(latest.get().getResult()).isEqualTo(InspectionResultCode.PASS);
    }
}