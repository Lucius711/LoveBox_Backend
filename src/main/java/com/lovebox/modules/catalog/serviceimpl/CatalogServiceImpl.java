package com.lovebox.modules.catalog.serviceimpl;

import com.lovebox.modules.catalog.dto.response.CatalogResponse;
import com.lovebox.modules.catalog.entity.ShowcaseDesign;
import com.lovebox.modules.catalog.repository.BoxSizeRepository;
import com.lovebox.modules.catalog.repository.ProductCategoryRepository;
import com.lovebox.modules.catalog.repository.ShowcaseDesignRepository;
import com.lovebox.modules.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final ProductCategoryRepository categoryRepository;
    private final BoxSizeRepository boxSizeRepository;
    private final ShowcaseDesignRepository showcaseDesignRepository;

    @Override
    public List<CatalogResponse.CategoryDto> getCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc()
                .stream().map(CatalogResponse.CategoryDto::from).toList();
    }

    @Override
    public List<CatalogResponse.BoxSizeDto> getBoxSizes() {
        return boxSizeRepository.findByIsActiveOrderByPriceAsc((short) 1)
                .stream().map(CatalogResponse.BoxSizeDto::from).toList();
    }

    @Override
    public List<CatalogResponse.ShowcaseDesignDto> getShowcaseDesigns(UUID categoryId) {
        List<ShowcaseDesign> designs = categoryId != null
                ? showcaseDesignRepository.findByCategoryIdAndIsActiveOrderByDisplayOrderAsc(categoryId, (short) 1)
                : showcaseDesignRepository.findByIsActiveOrderByDisplayOrderAsc((short) 1);
        return designs.stream().map(CatalogResponse.ShowcaseDesignDto::from).toList();
    }
}
