package com.lovebox.modules.product;

import com.lovebox.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "dtb_products")
@Getter @Setter
public class Product {

    public static final String PENDING = "PENDING", APPROVED = "APPROVED", REJECTED = "REJECTED", HIDDEN = "HIDDEN";
    public static final String COLOR = "COLOR", STYLE = "STYLE", OCCASION = "OCCASION", FEATURE = "FEATURE";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    private String name;
    private String description;
    private String category;
    private String size;
    @Column(name = "bust_max") private int bustMax;
    @Column(name = "waist_max") private int waistMax;
    @Column(name = "hip_max") private int hipMax;
    @Column(name = "item_condition") private String itemCondition;
    @Column(name = "retail_price") private long retailPrice;
    @Column(name = "rent_price_per_day") private long rentPricePerDay;
    @Column(name = "deposit_percent") private int depositPercent = 100;
    private String status = PENDING;
    @Column(name = "reject_reason") private String rejectReason;
    @Column(name = "rent_count") private int rentCount;

    @ElementCollection
    @CollectionTable(name = "dtb_product_tags", joinColumns = @JoinColumn(name = "product_id"))
    private Set<Tag> tags = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "dtb_product_images", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "url")
    private List<String> images = new ArrayList<>();

    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;

    @Embeddable
    public record Tag(@Column(name = "tag_type") String type, @Column(name = "tag_value") String value) {}

    /** Tiền cọc = % giá niêm yết (chủ đồ chọn 50-100%). */
    public long deposit() { return retailPrice * depositPercent / 100; }

    public List<String> tagValues(String type) {
        return tags.stream().filter(t -> t.type().equals(type)).map(Tag::value).sorted().toList();
    }

    public String cover() { return images.isEmpty() ? null : images.get(0); }
}
