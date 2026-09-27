package com.foodiehub.order_service.controller;

import com.foodiehub.order_service.dto.RefundTierRequest;
import com.foodiehub.order_service.model.OrderStatus;
import com.foodiehub.order_service.model.RefundActor;
import com.foodiehub.order_service.model.RefundTier;
import com.foodiehub.order_service.service.RefundTierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/refund-tiers")
@RequiredArgsConstructor
public class RefundTierController {

    private final RefundTierService refundTierService;

    @PutMapping("/{actor}/{orderStatus}")
    public ResponseEntity<RefundTier> updateRefundTier(
            @PathVariable RefundActor actor,
            @PathVariable OrderStatus orderStatus,
            @Valid @RequestBody RefundTierRequest request
    ) {

        return ResponseEntity.ok(
                refundTierService.updateRefundPercentage(
                        actor,
                        orderStatus,
                        request.refundPercentage()
                )
        );
    }
}