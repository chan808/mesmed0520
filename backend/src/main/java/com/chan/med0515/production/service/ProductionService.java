package com.chan.med0515.production.service;

import com.chan.med0515.global.error.BusinessException;
import com.chan.med0515.inspection.entity.InspectionItem;
import com.chan.med0515.inspection.entity.InspectionStandard;
import com.chan.med0515.inspection.enums.MeasurementType;
import com.chan.med0515.inspection.repository.InspectionItemRepository;
import com.chan.med0515.inspection.repository.InspectionStandardRepository;
import com.chan.med0515.material.entity.Material;
import com.chan.med0515.material.repository.MaterialRepository;
import com.chan.med0515.production.dto.BatchInspectionResultRequest;
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

import java.math.BigDecimal;
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

    // 검사 통과 시 lot pass, NG 시 lot은 IN_PROGRESS 유지 -> 재검사 대상
    @Transactional
    public InspectionResultResponse submitResult(Long lotId, InspectionResultRequest request) {
        ProductionLot lot = getLotEntity(lotId);

        if (lot.getStatus() != LotStatus.IN_PROGRESS) {
            throw new BusinessException(ProductionErrorCode.LOT_ALREADY_CLOSED);
        }

        InspectionItem item = itemRepository.findById(request.inspectionItemId())
                .orElseThrow(() -> new BusinessException(ProductionErrorCode.INSPECTION_ITEM_NOT_FOUND));

        // 항목의 품목 modelName이 이 lot의 모델과 일치하는지 확인
        String modelName = lot.getPlan().getModelName();
        Material material = item.getStandard().getMaterial();
        if (!modelName.equals(material.getModelName())) {
            throw new BusinessException(ProductionErrorCode.ITEM_NOT_BELONG_TO_MODEL);
        }

        InspectionResultCode result = resolveResult(item, request);
        int round = resultRepository.countByLotIdAndInspectionItemId(lotId, item.getId()) + 1;

        LotInspectionResult saved = resultRepository.save(LotInspectionResult.builder()
                .lot(lot)
                .inspectionItem(item)
                .round(round)
                .result(result)
                .measuredValue(request.measuredValue())
                .memo(request.memo())
                .build());

        if (result == InspectionResultCode.PASS) {
            evaluateAndUpdateLotStatus(lot, modelName);
        }

        return InspectionResultResponse.from(saved, lot.getStatus());
    }

    @Transactional
    public LotDetailResponse submitBatchResults(Long lotId, BatchInspectionResultRequest request) {
        ProductionLot lot = getLotEntity(lotId);
        if (lot.getStatus() != LotStatus.IN_PROGRESS) {
            throw new BusinessException(ProductionErrorCode.LOT_ALREADY_CLOSED);
        }
        String modelName = lot.getPlan().getModelName();

        for (InspectionResultRequest req : request.items()) {
            InspectionItem item = itemRepository.findById(req.inspectionItemId())
                    .orElseThrow(() -> new BusinessException(ProductionErrorCode.INSPECTION_ITEM_NOT_FOUND));
            if (!modelName.equals(item.getStandard().getMaterial().getModelName())) {
                throw new BusinessException(ProductionErrorCode.ITEM_NOT_BELONG_TO_MODEL);
            }
            InspectionResultCode result = resolveResult(item, req);
            int round = resultRepository.countByLotIdAndInspectionItemId(lotId, item.getId()) + 1;
            resultRepository.save(LotInspectionResult.builder()
                    .lot(lot).inspectionItem(item).round(round)
                    .result(result).measuredValue(req.measuredValue()).memo(req.memo())
                    .build());
        }

        evaluateAndUpdateLotStatus(lot, modelName);
        return buildLotDetail(lot);
    }

    @Transactional
    public LotDetailResponse failLot(Long lotId) {
        ProductionLot lot = getLotEntity(lotId);
        if (lot.getStatus() != LotStatus.IN_PROGRESS) {
            throw new BusinessException(ProductionErrorCode.LOT_ALREADY_CLOSED);
        }
        lot.fail();
        lot.getPlan().incrementFailCount();
        return buildLotDetail(lot);
    }

    // NUMERIC: measuredValue로 자동 판정 / VISUAL: 사용자 입력 result 사용
    private InspectionResultCode resolveResult(InspectionItem item, InspectionResultRequest request) {
        if (item.getMeasurementType() == MeasurementType.NUMERIC) {
            if (request.measuredValue() == null) {
                throw new BusinessException(ProductionErrorCode.MEASURED_VALUE_REQUIRED);
            }
            BigDecimal v = request.measuredValue();
            boolean inRange = (item.getMinValue() == null || v.compareTo(item.getMinValue()) >= 0)
                    && (item.getMaxValue() == null || v.compareTo(item.getMaxValue()) <= 0);
            return inRange ? InspectionResultCode.PASS : InspectionResultCode.NG;
        }

        // VISUAL
        if (request.result() == null) {
            throw new BusinessException(ProductionErrorCode.RESULT_REQUIRED);
        }
        return request.result();
    }

    private ProductionLot getLotEntity(Long lotId) {
        return lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(ProductionErrorCode.LOT_NOT_FOUND));
    }

    // 모델의 모든 품목 × 모든 검사항목의 최신 결과가 전부 PASS이면 lot을 PASS 처리.
    private void evaluateAndUpdateLotStatus(ProductionLot lot, String modelName) {
        List<Material> materials = materialRepository.findByModelNameAndDeletedAtIsNull(modelName);

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
        lot.getPlan().incrementPassCount();
    }

    private LotDetailResponse buildLotDetail(ProductionLot lot) {
        String modelName = lot.getPlan().getModelName();
        List<Material> materials = materialRepository.findByModelNameAndDeletedAtIsNull(modelName);

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