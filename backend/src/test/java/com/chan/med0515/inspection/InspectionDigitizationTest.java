package com.chan.med0515.inspection;

import com.chan.med0515.inspection.dto.*;
import com.chan.med0515.inspection.service.InspectionService;
import com.chan.med0515.material.dto.MaterialRequest;
import com.chan.med0515.material.dto.MaterialResponse;
import com.chan.med0515.material.service.MaterialService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class InspectionDigitizationTest {

    @Autowired
    private InspectionService inspectionService;

    @Autowired
    private MaterialService materialService;

    @Test
    @DisplayName("이미지 데이터를 기반으로 수입검사 기준서를 전산화 등록한다")
    void digitizeInspectionStandardFromImage() {
        // 1. 자재 등록 (ER-2000 SMART, LCD)
        MaterialRequest materialRequest = new MaterialRequest(
                "ER-2000 SMART",
                "LCD",
                "10018500701",
                "KJC Display corporation",
                "2.4inch"
        );
        MaterialResponse materialResponse = materialService.register(materialRequest);

        // 2. 검사 기준서 등록 (Rev 0 기준)
        InspectionStandardRequest standardRequest = new InspectionStandardRequest(
                materialResponse.id(),
                LocalDate.of(2022, 7, 1),
                "Sample검사",
                "통상검사 G=II",
                "보통검사",
                new BigDecimal("2.5"),
                null,
                null,
                "KD023LQTB008\n51.9 x 49.5(mm), 해상도: 320 x 240",
                "배포"
        );
        InspectionStandardResponse standardResponse = inspectionService.registerStandard(standardRequest);

        // 3. 개정 이력 등록 (Rev 0)
        inspectionService.addRevision(new RevisionRequest(
                standardResponse.id(),
                LocalDate.of(2022, 7, 1),
                "최초개정",
                "확인됨"
        ));

        // 4. 개정 이력 등록 (Rev 1)
        inspectionService.addRevision(new RevisionRequest(
                standardResponse.id(),
                LocalDate.of(2022, 8, 29),
                "검사기준 세부 내용 개정",
                "확인됨"
        ));

        // 5. 개정 이력 등록 (Rev 2)
        inspectionService.addRevision(new RevisionRequest(
                standardResponse.id(),
                LocalDate.of(2024, 7, 1),
                "검사 기준 수정 및 신규 검사항목 추가",
                "확인됨"
        ));

        // 6. 검사항목 등록
        inspectionService.addItem(new InspectionItemRequest(standardResponse.id(), "DOT 깨짐", "DOT깨짐이 0일 것.", "육안", "육안확인", "입고 시"));
        inspectionService.addItem(new InspectionItemRequest(standardResponse.id(), "FILM CABLE", "접촉 커넥터 부위에 이물질이 없을 것.", "육안", "육안확인", "입고 시"));
        inspectionService.addItem(new InspectionItemRequest(standardResponse.id(), "BLACK LIGHT", "밝기가 균일할 것.", "육안", "육안확인", "입고 시"));

        // 7. 검증 - 최신 기준서 조회
        InspectionStandardResponse latest = inspectionService.findLatestStandardByMaterial(materialResponse.id());
        
        assertThat(latest.rev()).isEqualTo(3); // 0(최초) + 3번 개정 = 3 (Service logic checks out)
        // Note: InspectionService.registerStandard sets rev to 0. 
        // addRevision increments rev and then saves history.
        // So 3 revisions added means rev becomes 3.

        assertThat(latest.modelName()).isEqualTo("ER-2000 SMART");
        assertThat(latest.memo()).contains("KD023LQTB008");
        
        // 8. 검사항목 조회 검증
        List<InspectionItemResponse> items = inspectionService.findItemsByStandard(latest.id());
        assertThat(items).hasSize(3);
        assertThat(items.get(0).itemName()).isEqualTo("DOT 깨짐");

        // 9. 개정 이력 조회 검증
        List<RevisionHistoryResponse> revisions = inspectionService.findRevisionsByStandard(latest.id());
        assertThat(revisions).hasSize(3);
        assertThat(revisions.get(2).revisionNote()).isEqualTo("검사 기준 수정 및 신규 검사항목 추가");
    }
}
