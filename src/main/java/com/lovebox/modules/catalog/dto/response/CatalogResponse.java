package com.lovebox.modules.catalog.dto.response;

import com.lovebox.modules.catalog.entity.BoxSize;
import com.lovebox.modules.catalog.entity.ProductCategory;
import com.lovebox.modules.catalog.entity.ShowcaseDesign;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Builder
public class CatalogResponse {

    @Getter @Builder
    public static class CategoryDto {
        private UUID id;
        private String name;
        private String slug;
        private String description;
        private Integer displayOrder;

        public static CategoryDto from(ProductCategory c) {
            return CategoryDto.builder()
                    .id(c.getId()).name(c.getName()).slug(c.getSlug())
                    .description(c.getDescription()).displayOrder(c.getDisplayOrder()).build();
        }
    }

    @Getter @Builder
    public static class BoxSizeDto {
        private UUID id;
        private String code;
        private String name;
        private BigDecimal price;
        private String description;

        public static BoxSizeDto from(BoxSize b) {
            return BoxSizeDto.builder()
                    .id(b.getId()).code(b.getCode()).name(b.getName())
                    .price(b.getPrice()).description(b.getDescription()).build();
        }
    }

    @Getter @Builder
    public static class ShowcaseDesignDto {
        private UUID id;
        private UUID categoryId;
        private String name;
        private String thumbnailUrl;
        private String description;
        private Integer displayOrder;

        public static ShowcaseDesignDto from(ShowcaseDesign s) {
            return ShowcaseDesignDto.builder()
                    .id(s.getId()).categoryId(s.getCategoryId()).name(s.getName())
                    .thumbnailUrl(s.getThumbnailUrl()).description(s.getDescription())
                    .displayOrder(s.getDisplayOrder()).build();
        }
    }
}
