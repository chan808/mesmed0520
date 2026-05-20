package com.chan.med0515.inspection.service;

import com.chan.med0515.global.error.BusinessException;
import com.chan.med0515.inspection.dto.*;
import com.chan.med0515.inspection.entity.InspectionItem;
import com.chan.med0515.inspection.entity.InspectionStandard;
import com.chan.med0515.inspection.entity.RevisionHistory;
import com.chan.med0515.inspection.error.InspectionErrorCode;
import com.chan.med0515.inspection.repository.InspectionItemRepository;
import com.chan.med0515.inspection.repository.InspectionStandardRepository;
import com.chan.med0515.inspection.repository.RevisionHistoryRepository;
import com.chan.med0515.material.entity.Material;
import com.chan.med0515.material.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionService {

    private final InspectionStandardRepository standardRepository;
    private final InspectionItemRepository itemRepository;
    private final RevisionHistoryRepository revisionRepository;
    private final MaterialService materialService;

    @Transactional
    public InspectionStandardResponse registerStandard(InspectionStandardRequest request) {
        Material material = materialService.getEntityById(request.materialId());
        InspectionStandard standard = InspectionStandard.builder()
                .material(material)
                .rev(0)
                .establishedAt(request.establishedAt())
                .inspectionType(request.inspectionType())
                .inspectionLevel(request.inspectionLevel())
                .strictness(request.strictness())
                .aql(request.aql())
                .aqlAc(request.aqlAc())
                .aqlRe(request.aqlRe())
                .build();
        standardRepository.save(standard);

        revisionRepository.save(RevisionHistory.builder()
                .standard(standard)
                .rev(0)
                .revisionDate(request.revisionDate())
                .revisionNote(request.revisionNote())
                .confirmedBy(request.confirmedBy())
                .build());

        return InspectionStandardResponse.from(standard);
    }

    @Transactional
    public InspectionStandardResponse updateStandard(Long id, InspectionStandardUpdateRequest request) {
        InspectionStandard standard = standardRepository.findById(id)
                .orElseThrow(() -> new BusinessException(InspectionErrorCode.STANDARD_NOT_FOUND));

        standard.updateFields(
                request.establishedAt(),
                request.inspectionType(),
                request.inspectionLevel(),
                request.strictness(),
                request.aql(),
                request.aqlAc(),
                request.aqlRe()
        );
        standard.incrementRev();

        revisionRepository.save(RevisionHistory.builder()
                .standard(standard)
                .rev(standard.getRev())
                .revisionDate(request.revisionDate())
                .revisionNote(request.revisionNote())
                .confirmedBy(request.confirmedBy())
                .build());

        return InspectionStandardResponse.from(standard);
    }

    public List<InspectionStandardResponse> findStandardsByMaterial(Long materialId) {
        return standardRepository.findAllByMaterialId(materialId).stream()
                .map(InspectionStandardResponse::from)
                .toList();
    }

    public InspectionStandardResponse findStandardById(Long id) {
        return standardRepository.findById(id)
                .map(InspectionStandardResponse::from)
                .orElseThrow(() -> new BusinessException(InspectionErrorCode.STANDARD_NOT_FOUND));
    }

    public List<InspectionItemResponse> findItemsByStandard(Long standardId) {
        return itemRepository.findAllByStandardId(standardId).stream()
                .map(InspectionItemResponse::from)
                .toList();
    }

    @Transactional
    public RevisionHistoryResponse addRevision(RevisionRequest request) {
        InspectionStandard standard = standardRepository.findById(request.standardId())
                .orElseThrow(() -> new BusinessException(InspectionErrorCode.STANDARD_NOT_FOUND));

        boolean hasItemChanges = (request.addItems() != null && !request.addItems().isEmpty())
                || (request.deleteItemIds() != null && !request.deleteItemIds().isEmpty());

        // 항목 변경 시 rev를 먼저 올려야 추가/삭제 항목에 새 rev를 기록할 수 있음
        if (hasItemChanges) {
            standard.incrementRev();
        }

        if (request.deleteItemIds() != null) {
            request.deleteItemIds().forEach(itemId -> {
                InspectionItem item = itemRepository.findById(itemId)
                        .orElseThrow(() -> new BusinessException(InspectionErrorCode.ITEM_NOT_FOUND));
                item.softDelete(standard.getRev());
            });
        }

        List<InspectionItem> addedItems = List.of();
        if (request.addItems() != null && !request.addItems().isEmpty()) {
            addedItems = request.addItems().stream()
                    .map(itemReq -> InspectionItem.builder()
                            .standard(standard)
                            .itemName(itemReq.itemName())
                            .specification(itemReq.specification())
                            .method(itemReq.method())
                            .equipment(itemReq.equipment())
                            .timing(itemReq.timing())
                            .addedAtRev(standard.getRev())
                            .build())
                    .map(itemRepository::save)
                    .toList();
        }

        RevisionHistory revision = RevisionHistory.builder()
                .standard(standard)
                .rev(standard.getRev())
                .revisionDate(request.revisionDate())
                .revisionNote(request.revisionNote())
                .confirmedBy(request.confirmedBy())
                .build();

        List<InspectionItemResponse> addedItemResponses = addedItems.stream()
                .map(InspectionItemResponse::from)
                .toList();

        return RevisionHistoryResponse.from(revisionRepository.save(revision), addedItemResponses);
    }

    public List<RevisionHistoryResponse> findRevisionsByStandard(Long standardId) {
        return revisionRepository.findAllByStandardIdOrderByRevAsc(standardId).stream()
                .map(r -> RevisionHistoryResponse.from(r, List.of()))
                .toList();
    }

    public InspectionSnapshotResponse getSnapshotByRev(Long standardId, int rev) {
        standardRepository.findById(standardId)
                .orElseThrow(() -> new BusinessException(InspectionErrorCode.STANDARD_NOT_FOUND));

        RevisionHistory revision = revisionRepository.findByStandardIdAndRev(standardId, rev)
                .orElseThrow(() -> new BusinessException(InspectionErrorCode.REVISION_NOT_FOUND));

        List<InspectionItemResponse> items = itemRepository.findSnapshotItems(standardId, rev).stream()
                .map(InspectionItemResponse::from)
                .toList();

        return InspectionSnapshotResponse.from(revision, items);
    }
}
