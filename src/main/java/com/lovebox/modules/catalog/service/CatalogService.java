package com.lovebox.modules.catalog.service;

import com.lovebox.modules.catalog.dto.response.CatalogResponse;

import java.util.List;
import java.util.UUID;

public interface CatalogService {
    List<CatalogResponse.CategoryDto> getCategories();
    List<CatalogResponse.BoxSizeDto> getBoxSizes();
    List<CatalogResponse.ShowcaseDesignDto> getShowcaseDesigns(UUID categoryId);
}
