package com.chan.med0515.global;

import com.chan.med0515.inspection.entity.InspectionItem;
import com.chan.med0515.inspection.entity.InspectionStandard;
import com.chan.med0515.inspection.entity.RevisionHistory;
import com.chan.med0515.inspection.repository.InspectionItemRepository;
import com.chan.med0515.inspection.repository.InspectionStandardRepository;
import com.chan.med0515.inspection.repository.RevisionHistoryRepository;
import com.chan.med0515.material.entity.Material;
import com.chan.med0515.material.repository.MaterialRepository;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 1. 초기 사용자 등록
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .displayName("관리자")
                    .role(UserRole.ADMIN)
                    .build());
        }

        // 2. OJT 이미지 기반 자재 및 검사기준 데이터 등록
        String partCode = "10018500701";
        if (!materialRepository.existsByPartCode(partCode)) {
            // 자재 등록
            Material lcd = Material.builder()
                    .modelName("ER-2000 SMART")
                    .partName("LCD")
                    .partCode(partCode)
                    .supplier("KJC Display corporation")
                    .materialSpec("2.4inch")
                    .build();
            materialRepository.save(lcd);

            // 검사 기준서 등록 (최종 Rev. 2 기준)
            InspectionStandard standard = InspectionStandard.builder()
                    .material(lcd)
                    .rev(2)
                    .establishedAt(LocalDate.of(2024, 7, 1))
                    .inspectionType("Sample검사")
                    .inspectionLevel("II")
                    .strictness("보통검사")
                    .aql(new BigDecimal("2.5"))
                    .aqlAc(0)
                    .aqlRe(1)
                    .build();
            standardRepository.save(standard);

            // 검사 항목 등록
            itemRepository.save(InspectionItem.builder()
                    .standard(standard)
                    .itemName("DOT 깨짐")
                    .specification("DOT깨짐이 0일 것.")
                    .method("육안")
                    .equipment("육안확인")
                    .timing("입고 시")
                    .build());

            itemRepository.save(InspectionItem.builder()
                    .standard(standard)
                    .itemName("FILM CABLE")
                    .specification("접촉 커넥터 부위에 이물질이 없을 것.")
                    .method("육안")
                    .equipment("육안확인")
                    .timing("입고 시")
                    .build());

            itemRepository.save(InspectionItem.builder()
                    .standard(standard)
                    .itemName("BLACK LIGHT")
                    .specification("밝기가 균일할 것.")
                    .method("육안")
                    .equipment("육안확인")
                    .timing("입고 시")
                    .build());

            // 개정 이력 등록
            revisionRepository.save(RevisionHistory.builder()
                    .standard(standard)
                    .rev(0)
                    .revisionDate(LocalDate.of(2022, 7, 1))
                    .revisionNote("최초개정")
                    .confirmedBy("배포")
                    .build());

            revisionRepository.save(RevisionHistory.builder()
                    .standard(standard)
                    .rev(1)
                    .revisionDate(LocalDate.of(2022, 8, 29))
                    .revisionNote("검사기준 세부 내용 개정")
                    .confirmedBy("배포")
                    .build());

            revisionRepository.save(RevisionHistory.builder()
                    .standard(standard)
                    .rev(2)
                    .revisionDate(LocalDate.of(2024, 7, 1))
                    .revisionNote("검사 기준 수정 및 신규 검사항목 추가")
                    .confirmedBy("배포")
                    .build());
        }
    }
}
