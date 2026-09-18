package com.lovebox.modules.catalog.controller;

import com.lovebox.common.response.ApiResponse;
import com.lovebox.modules.catalog.dto.response.CatalogResponse;
import com.lovebox.modules.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CatalogResponse.CategoryDto>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(catalogService.getCategories()));
    }

    @GetMapping("/box-sizes")
    public ResponseEntity<ApiResponse<List<CatalogResponse.BoxSizeDto>>> getBoxSizes() {
        return ResponseEntity.ok(ApiResponse.success(catalogService.getBoxSizes()));
    }

    @GetMapping("/showcase-designs")
    public ResponseEntity<ApiResponse<List<CatalogResponse.ShowcaseDesignDto>>> getShowcaseDesigns(
            @RequestParam(required = false) UUID categoryId) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.getShowcaseDesigns(categoryId)));
    }
}
