package com.chan.med0515.production.service;

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
import com.chan.med0515.production.dto.LotDetailResponse.ItemResult;
import com.chan.med0515.production.dto.LotDetailResponse.MaterialResult;
import com.chan.med0515.production.entity.LotInspectionResult;
import com.chan.med0515.production.entity.ProductionLot;
import com.chan.med0515.production.entity.ProductionPlan;
import com.chan.med0515.production.enums.InspectionResultCode;
import com.chan.med0515.production.enums.LotStatus;
import com.chan.med0515.production.error.ProductionErrorCode;
import com.chan.med0515.production.repository.LotInspectionResultRepository;
import com.chan.med0515.production.repository.ProductionLotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionService {

    private final ProductionPlanService planService;
    private final ProductionLotRepository lotRepository;
    private final LotInspectionResultRepository resultRepository;
    private final MaterialRepository materialRepository;
    private final InspectionStandardRepository standardRepository;
    private final InspectionItemRepository itemRepository;

    @Transactional
    public LotDetailResponse startLot(Long planId) {
        ProductionPlan plan = planService.getEntityById(planId);
        int nextLotNo = lotRepository.countByPlanId(planId) + 1;
        plan.startIfPlanned();

        ProductionLot lot = lotRepository.save(ProductionLot.builder()
                .plan(plan)
                .lotNo(nextLotNo)
                .build());
        return buildLotDetail(lot);
    }

    public LotDetailResponse getLotDetail(Long lotId) {
        ProductionLot lot = getLotEntity(lotId);
        return buildLotDetail(lot);
    }

    /**
     * 검사 결과 제출.
     * PASS 시 전 항목 통과 여부 재평가 → 조건 충족 시 lot PASS.
     * NG 시 lot은 IN_PROGRESS 유지 → 재검사 대상.
     */
    @Transactional
    public InspectionResultResponse submitResult(Long lotId, InspectionResultRequest request) {
        ProductionLot lot = getLotEntity(lotId);

        if (lot.getStatus() != LotStatus.IN_PROGRESS) {
            throw new BusinessException(ProductionErrorCode.LOT_ALREADY_CLOSED);
        }

        InspectionItem item = itemRepository.findById(request.inspectionItemId())
                .orElseThrow(() -> new BusinessException(ProductionErrorCode.INSPECTION_ITEM_NOT_FOUND));

        // 항목의 품목이 이 lot의 모델에 속하는지 확인
        Long modelId = lot.getPlan().getModel().getId();
        Material material = item.getStandard().getMaterial();
        if (material.getProductModel() == null
                || !material.getProductModel().getId().equals(modelId)) {
            throw new BusinessException(ProductionErrorCode.ITEM_NOT_BELONG_TO_MODEL);
        }

        int round = resultRepository.countByLotIdAndInspectionItemId(lotId, item.getId()) + 1;

        LotInspectionResult saved = resultRepository.save(LotInspectionResult.builder()
                .lot(lot)
                .inspectionItem(item)
                .round(round)
                .result(request.result())
                .memo(request.memo())
                .build());

        if (request.result() == InspectionResultCode.PASS) {
            evaluateAndUpdateLotStatus(lot, modelId);
        }

        return InspectionResultResponse.from(saved, lot.getStatus());
    }

    @Transactional
    public LotDetailResponse failLot(Long lotId) {
        ProductionLot lot = getLotEntity(lotId);
        if (lot.getStatus() != LotStatus.IN_PROGRESS) {
            throw new BusinessException(ProductionErrorCode.LOT_ALREADY_CLOSED);
        }
        lot.fail();
        return buildLotDetail(lot);
    }

    // ─────────────────────────────────────────────────
    // private helpers
    // ─────────────────────────────────────────────────

    private ProductionLot getLotEntity(Long lotId) {
        return lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(ProductionErrorCode.LOT_NOT_FOUND));
    }

    /**
     * 모델의 모든 품목 × 모든 검사항목의 최신 결과가 전부 PASS이면 lot을 PASS 처리.
     */
    private void evaluateAndUpdateLotStatus(ProductionLot lot, Long modelId) {
        List<Material> materials = materialRepository.findByProductModelIdAndDeletedAtIsNull(modelId);

        for (Material material : materials) {
            Optional<InspectionStandard> standardOpt =
                    standardRepository.findTopByMaterialIdOrderByRevDesc(material.getId());
            if (standardOpt.isEmpty()) return; // 기준서 없으면 판정 불가

            List<InspectionItem> items = itemRepository.findAllByStandardId(standardOpt.get().getId());
            if (items.isEmpty()) return;

            for (InspectionItem item : items) {
                Optional<LotInspectionResult> latest = resultRepository
                        .findTopByLotIdAndInspectionItemIdOrderByRoundDesc(lot.getId(), item.getId());
                // 미검사 또는 NG가 하나라도 있으면 아직 미통과
                if (latest.isEmpty() || latest.get().getResult() == InspectionResultCode.NG) {
                    return;
                }
            }
        }

        // 모든 항목 PASS 확인
        lot.pass();
        lot.getPlan().startIfPlanned();
        lot.getPlan().incrementPassCount();
    }

    private LotDetailResponse buildLotDetail(ProductionLot lot) {
        Long modelId = lot.getPlan().getModel().getId();
        List<Material> materials = materialRepository.findByProductModelIdAndDeletedAtIsNull(modelId);

        List<MaterialResult> materialResults = materials.stream()
                .map(m -> buildMaterialResult(lot.getId(), m))
                .toList();

        return LotDetailResponse.from(lot, materialResults);
    }

    private MaterialResult buildMaterialResult(Long lotId, Material material) {
        Optional<InspectionStandard> standardOpt =
                standardRepository.findTopByMaterialIdOrderByRevDesc(material.getId());

        if (standardOpt.isEmpty()) {
            return LotDetailResponse.toMaterialResult(material, null, List.of());
        }

        List<InspectionItem> items = itemRepository.findAllByStandardId(standardOpt.get().getId());

        List<ItemResult> itemResults = items.stream()
                .map(item -> buildItemResult(lotId, item))
                .toList();

        // 미검사 항목 있으면 null, 전부 PASS면 PASS, 하나라도 NG면 NG
        InspectionResultCode materialResult = null;
        if (itemResults.stream().noneMatch(ir -> ir.currentResult() == null)) {
            boolean allPass = itemResults.stream()
                    .allMatch(ir -> ir.currentResult() == InspectionResultCode.PASS);
            materialResult = allPass ? InspectionResultCode.PASS : InspectionResultCode.NG;
        }

        return LotDetailResponse.toMaterialResult(material, materialResult, itemResults);
    }

    private ItemResult buildItemResult(Long lotId, InspectionItem item) {
        List<LotInspectionResult> results = resultRepository
                .findByLotIdAndInspectionItemIdOrderByRoundAsc(lotId, item.getId());
        return ItemResult.from(item, results);
    }
}