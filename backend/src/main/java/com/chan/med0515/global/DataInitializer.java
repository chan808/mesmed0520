package com.chan.med0515.global;

import com.chan.med0515.inspection.entity.InspectionItem;
import com.chan.med0515.inspection.entity.InspectionStandard;
import com.chan.med0515.inspection.entity.RevisionHistory;
import com.chan.med0515.inspection.repository.InspectionItemRepository;
import com.chan.med0515.inspection.repository.InspectionStandardRepository;
import com.chan.med0515.inspection.repository.RevisionHistoryRepository;
import com.chan.med0515.material.entity.Material;
import com.chan.med0515.material.repository.MaterialRepository;
import com.chan.med0515.production.entity.ProductModel;
import com.chan.med0515.production.entity.ProductionLot;
import com.chan.med0515.production.entity.ProductionPlan;
import com.chan.med0515.production.repository.ProductModelRepository;
import com.chan.med0515.production.repository.ProductionLotRepository;
import com.chan.med0515.production.repository.ProductionPlanRepository;
import com.chan.med0515.user.entity.User;
import com.chan.med0515.user.enums.UserRole;
import com.chan.med0515.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MaterialRepository materialRepository;
    private final InspectionStandardRepository standardRepository;
    private final InspectionItemRepository itemRepository;
    private final RevisionHistoryRepository revisionRepository;
    private final ProductModelRepository modelRepository;
    private final ProductionPlanRepository planRepository;
    private final ProductionLotRepository lotRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedUser();
        seedProductionData();
    }

    private void seedUser() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .displayName("관리자")
                    .role(UserRole.ADMIN)
                    .build());
        }
    }

    private void seedProductionData() {
        if (modelRepository.existsByName("ER-2000 SMART")) return;

        // ── 생산 모델 2개 ──────────────────────────────────────
        ProductModel er2000 = modelRepository.save(ProductModel.builder()
                .name("ER-2000 SMART")
                .description("스마트 응급처치 모니터")
                .build());

        ProductModel bp3000 = modelRepository.save(ProductModel.builder()
                .name("BP-3000")
                .description("자동 혈압계")
                .build());

        // ── ER-2000 SMART 품목 3개 ─────────────────────────────
        Material lcd = seedMaterial(er2000, "LCD", "10018500701", "KJC Display corporation", "2.4inch");
        Material btnSwitch = seedMaterial(er2000, "버튼 스위치", "BTN-SW-001", "대한전자", "6x6mm");
        Material battery = seedMaterial(er2000, "배터리팩", "BAT-PACK-001", "삼성SDI", "3.7V 2500mAh");

        // ── BP-3000 품목 3개 ──────────────────────────────────
        Material sensor = seedMaterial(bp3000, "압력센서모듈", "SEN-BP-001", "Honeywell", "0-300mmHg");
        Material lcdB = seedMaterial(bp3000, "LCD B", "LCD-B-001", "KJC Display corporation", "1.8inch");
        Material housing = seedMaterial(bp3000, "하우징", "HOU-BP-001", "우진플라텍", "ABS 수지");

        // ── 각 품목 검사 기준서 + 항목 등록 ──────────────────────
        seedLcdStandard(lcd);
        seedBtnSwitchStandard(btnSwitch);
        seedBatteryStandard(battery);
        seedSensorStandard(sensor);
        seedLcdBStandard(lcdB);
        seedHousingStandard(housing);

        // ── 오늘 날짜 생산 계획 2개 (대시보드 확인용) ──────────────
        LocalDate today = LocalDate.now();
        ProductionPlan er2000Plan = planRepository.save(ProductionPlan.builder()
                .model(er2000).planDate(today).targetQty(50).build());
        ProductionPlan bp3000Plan = planRepository.save(ProductionPlan.builder()
                .model(bp3000).planDate(today).targetQty(30).build());

        // ER-2000 SMART: lot 3개 생성 (IN_PROGRESS 상태 — 대시보드 확인용)
        er2000Plan.startIfPlanned();
        for (int i = 1; i <= 3; i++) {
            lotRepository.save(ProductionLot.builder().plan(er2000Plan).lotNo(i).build());
        }

        // BP-3000: lot 2개 생성
        bp3000Plan.startIfPlanned();
        for (int i = 1; i <= 2; i++) {
            lotRepository.save(ProductionLot.builder().plan(bp3000Plan).lotNo(i).build());
        }
    }

    private Material seedMaterial(ProductModel model, String partName, String partCode,
                                   String supplier, String spec) {
        Material m = Material.builder()
                .modelName(model.getName())
                .partName(partName)
                .partCode(partCode)
                .supplier(supplier)
                .materialSpec(spec)
                .build();
        m.assignModel(model);
        return materialRepository.save(m);
    }

    // ── 검사 기준서 등록 헬퍼 ─────────────────────────────────────

    private void seedLcdStandard(Material lcd) {
        InspectionStandard std = saveStandard(lcd, 2, LocalDate.of(2024, 7, 1),
                "Sample검사", "II", "보통검사", "2.5", 0, 1);
        saveItem(std, "DOT 깨짐", "DOT깨짐이 0일 것", "육안", "육안확인", "입고 시");
        saveItem(std, "FILM CABLE", "접촉 커넥터 부위에 이물질이 없을 것", "육안", "육안확인", "입고 시");
        saveItem(std, "BLACK LIGHT", "밝기가 균일할 것", "육안", "육안확인", "입고 시");
        revisionRepository.save(RevisionHistory.builder().standard(std).rev(0)
                .revisionDate(LocalDate.of(2022, 7, 1)).revisionNote("최초개정").confirmedBy("배포").build());
        revisionRepository.save(RevisionHistory.builder().standard(std).rev(2)
                .revisionDate(LocalDate.of(2024, 7, 1)).revisionNote("검사 기준 수정 및 신규 항목 추가").confirmedBy("배포").build());
    }

    private void seedBtnSwitchStandard(Material m) {
        InspectionStandard std = saveStandard(m, 1, LocalDate.of(2024, 1, 1),
                "전수검사", "I", "보통검사", "1.0", 0, 1);
        saveItem(std, "외관검사", "스크래치·이물질 없을 것", "육안", "육안확인", "입고 시");
        saveItem(std, "클릭감 확인", "클릭음 명확하고 걸림 없을 것", "작동", "수작업", "입고 시");
        revisionRepository.save(RevisionHistory.builder().standard(std).rev(0)
                .revisionDate(LocalDate.of(2024, 1, 1)).revisionNote("최초개정").confirmedBy("배포").build());
    }

    private void seedBatteryStandard(Material m) {
        InspectionStandard std = saveStandard(m, 1, LocalDate.of(2024, 3, 1),
                "Sample검사", "II", "보통검사", "1.0", 0, 1);
        saveItem(std, "전압측정", "3.6V ~ 3.8V 범위일 것", "측정", "멀티미터", "입고 시");
        saveItem(std, "외관검사", "변형·누액 없을 것", "육안", "육안확인", "입고 시");
        revisionRepository.save(RevisionHistory.builder().standard(std).rev(0)
                .revisionDate(LocalDate.of(2024, 3, 1)).revisionNote("최초개정").confirmedBy("배포").build());
    }

    private void seedSensorStandard(Material m) {
        InspectionStandard std = saveStandard(m, 1, LocalDate.of(2023, 6, 1),
                "전수검사", "I", "보통검사", "1.0", 0, 1);
        saveItem(std, "압력정확도", "±2mmHg 이내일 것", "측정", "교정기", "입고 시");
        saveItem(std, "외관검사", "핀 휨·이물질 없을 것", "육안", "육안확인", "입고 시");
        revisionRepository.save(RevisionHistory.builder().standard(std).rev(0)
                .revisionDate(LocalDate.of(2023, 6, 1)).revisionNote("최초개정").confirmedBy("배포").build());
    }

    private void seedLcdBStandard(Material m) {
        InspectionStandard std = saveStandard(m, 1, LocalDate.of(2023, 6, 1),
                "Sample검사", "II", "보통검사", "2.5", 0, 1);
        saveItem(std, "DOT 깨짐", "DOT깨짐이 0일 것", "육안", "육안확인", "입고 시");
        saveItem(std, "휘도 균일성", "밝기 편차 10% 이내", "측정", "조도계", "입고 시");
        revisionRepository.save(RevisionHistory.builder().standard(std).rev(0)
                .revisionDate(LocalDate.of(2023, 6, 1)).revisionNote("최초개정").confirmedBy("배포").build());
    }

    private void seedHousingStandard(Material m) {
        InspectionStandard std = saveStandard(m, 1, LocalDate.of(2023, 6, 1),
                "Sample검사", "II", "보통검사", "2.5", 0, 1);
        saveItem(std, "치수검사", "도면 허용공차 이내일 것", "측정", "버니어캘리퍼스", "입고 시");
        saveItem(std, "외관검사", "크랙·플래시 없을 것", "육안", "육안확인", "입고 시");
        revisionRepository.save(RevisionHistory.builder().standard(std).rev(0)
                .revisionDate(LocalDate.of(2023, 6, 1)).revisionNote("최초개정").confirmedBy("배포").build());
    }

    private InspectionStandard saveStandard(Material material, int rev, LocalDate date,
                                             String type, String level, String strictness,
                                             String aql, int ac, int re) {
        return standardRepository.save(InspectionStandard.builder()
                .material(material).rev(rev).establishedAt(date)
                .inspectionType(type).inspectionLevel(level).strictness(strictness)
                .aql(new BigDecimal(aql)).aqlAc(ac).aqlRe(re)
                .build());
    }

    private void saveItem(InspectionStandard std, String name, String spec, String method,
                           String equipment, String timing) {
        itemRepository.save(InspectionItem.builder()
                .standard(std).itemName(name).specification(spec)
                .method(method).equipment(equipment).timing(timing)
                .addedAtRev(0)
                .build());
    }
}