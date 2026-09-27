package com.foodiehub.order_service.controller;

import com.foodiehub.order_service.client.CatalogClient;
import com.foodiehub.order_service.dto.CatalogMenuItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/test/")
@RequiredArgsConstructor
public class CatalogClientTestController {
    private final CatalogClient catalogClient;



    @GetMapping("/catalog/menu-item/{menuItemId}")
    public ResponseEntity<CatalogMenuItemResponse> testCatalog(
            @PathVariable UUID menuItemId
    ) {
        return ResponseEntity.ok(
                catalogClient.getMenuItem(menuItemId)
        );
    }
}
