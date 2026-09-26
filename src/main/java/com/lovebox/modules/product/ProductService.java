package com.lovebox.modules.product;

import com.lovebox.common.constant.ErrorCode;
import com.lovebox.common.exception.AppException;
import com.lovebox.modules.booking.BookingRepository;
import com.lovebox.modules.booking.RentalRules;
import com.lovebox.modules.booking.Review;
import com.lovebox.modules.booking.ReviewRepository;
import com.lovebox.modules.file.R2Storage;
import com.lovebox.modules.notification.Mailer;
import com.lovebox.modules.product.ProductDtos.*;
import com.lovebox.modules.user.entity.User;
import com.lovebox.modules.user.repository.UserRepository;
import com.lovebox.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final Mailer mailer;
    private final R2Storage r2;

    public record Filter(String q, String category, String style, String color, String size, Long minPrice,
                         Long maxPrice, String availability, String sort, int page, int pageSize) {}

    /** Lọc + phân trang ở DB (page bắt đầu từ 1). */
    public PageResult<Summary> search(Filter f) {
        Sort sort = switch (f.sort() == null ? "new" : f.sort()) {
            case "popular" -> Sort.by(Sort.Direction.DESC, "rentCount");
            case "priceAsc" -> Sort.by("rentPricePerDay");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "rentPricePerDay");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        int size = Math.min(Math.max(f.pageSize(), 1), 48);
        Page<Product> page = productRepository.findAll(spec(f, busyToday()),
                PageRequest.of(Math.max(f.page(), 1) - 1, size, sort.and(Sort.by("id"))));
        return new PageResult<>(page.getContent().stream().map(Summary::from).toList(),
                page.getNumber() + 1, size, page.getTotalElements(), page.getTotalPages());
    }

    private static Specification<Product> spec(Filter f, List<UUID> busy) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("status"), Product.APPROVED));
            if (f.category() != null) ps.add(cb.equal(root.get("category"), f.category()));
            if (f.size() != null) ps.add(cb.equal(root.get("size"), f.size()));
            if (f.minPrice() != null) ps.add(cb.ge(root.<Long>get("rentPricePerDay"), f.minPrice()));
            if (f.maxPrice() != null) ps.add(cb.le(root.<Long>get("rentPricePerDay"), f.maxPrice()));
            if (f.style() != null) ps.add(hasTag(root, query, cb, Product.STYLE, f.style()));
            if (f.color() != null) ps.add(hasTag(root, query, cb, Product.COLOR, f.color()));
            if ("available".equals(f.availability()) && !busy.isEmpty()) ps.add(cb.not(root.get("id").in(busy)));
            if ("rented".equals(f.availability())) ps.add(busy.isEmpty() ? cb.disjunction() : root.get("id").in(busy));
            if (f.q() != null && !f.q().isBlank()) {
                for (String w : f.q().toLowerCase().trim().split("\\s+")) {
                    String like = "%" + w.replace("%", "\\%").replace("_", "\\_") + "%";
                    ps.add(cb.or(cb.like(cb.lower(root.<String>get("name")), like), cb.like(cb.lower(root.<String>get("category")), like),
                            cb.like(cb.lower(root.<String>get("description")), like)));
                }
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }

    private static Predicate hasTag(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb, String type, String value) {
        Subquery<Integer> sq = query.subquery(Integer.class);
        Root<Product> p = sq.from(Product.class);
        Join<Product, Product.Tag> t = p.join("tags");
        sq.select(cb.literal(1)).where(cb.equal(p, root), cb.equal(t.get("type"), type), cb.equal(t.get("value"), value));
        return cb.exists(sq);
    }

    // ponytail: trợ lý AI chấm điểm trong bộ nhớ trên toàn bộ đồ đã duyệt — ổn tới vài nghìn món; hơn thì lọc trước bằng spec().
    public List<Product> approved() {
        return productRepository.findByStatusOrderByCreatedAtDesc(Product.APPROVED);
    }

    public List<UUID> busyToday() {
        LocalDate today = LocalDate.now();
        return bookingRepository.findBusyProductIds(today, today.minusDays(RentalRules.BUFFER_DAYS));
    }

    public Detail detail(UUID id, UserPrincipal viewer) {
        Product p = find(id);
        boolean privileged = viewer != null && (viewer.isAdmin() || p.getOwner().getId().equals(viewer.getId()));
        if (!Product.APPROVED.equals(p.getStatus()) && !privileged) throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        return toDetail(p);
    }

    public Detail toDetail(Product p) {
        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(p.getId());
        Map<UUID, String> names = userRepository.findAllById(reviews.stream().map(Review::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, User::getName));
        User o = p.getOwner();
        return new Detail(p.getId(), p.getName(), p.getDescription(), p.getCategory(), p.getSize(),
                p.getBustMax(), p.getWaistMax(), p.getHipMax(), p.getItemCondition(),
                p.getRetailPrice(), p.getRentPricePerDay(), p.getDepositPercent(), p.deposit(),
                p.tagValues(Product.COLOR), p.tagValues(Product.STYLE), p.tagValues(Product.OCCASION),
                p.tagValues(Product.FEATURE), List.copyOf(p.getImages()), p.getStatus(), p.getRejectReason(),
                p.getRentCount(), new Owner(o.getId(), o.getName(), o.getAvatarUrl(), o.getTrustScore()),
                reviews.stream().mapToInt(Review::getRating).average().orElse(0),
                reviews.stream().map(r -> new ReviewView(names.getOrDefault(r.getUserId(), "Khách"),
                        r.getRating(), r.getComment(), r.getCreatedAt())).toList());
    }

    /** Các ngày đã bị khoá (kèm ngày đệm) từ hôm nay — frontend làm mờ trên lịch. */
    public List<LocalDate> blockedDates(UUID id) {
        LocalDate today = LocalDate.now();
        return bookingRepository.findActiveFrom(id, today.minusDays(RentalRules.BUFFER_DAYS)).stream()
                .flatMap(b -> RentalRules.blockedDays(b.getStartDate(), b.getEndDate()).stream())
                .filter(d -> !d.isBefore(today)).distinct().sorted().toList();
    }

    // ── Chủ đồ ────────────────────────────────────────────────────────────
    public List<Summary> mine(UUID ownerId) {
        return productRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(Summary::from).toList();
    }

    @Transactional
    public Detail create(UUID ownerId, ProductRequest req) {
        Product p = new Product();
        p.setOwner(userRepository.getReferenceById(ownerId));
        apply(p, req);
        return toDetail(productRepository.save(p));
    }

    @Transactional
    public Detail update(UserPrincipal user, UUID id, ProductRequest req) {
        Product p = find(id);
        if (!p.getOwner().getId().equals(user.getId()) && !user.isAdmin()) throw new AppException(ErrorCode.FORBIDDEN);
        apply(p, req);
        if (!user.isAdmin()) { p.setStatus(Product.PENDING); p.setRejectReason(null); }  // sửa → duyệt lại
        return toDetail(p);
    }

    @Transactional
    public void hide(UserPrincipal user, UUID id) {
        Product p = find(id);
        if (!p.getOwner().getId().equals(user.getId()) && !user.isAdmin()) throw new AppException(ErrorCode.FORBIDDEN);
        p.setStatus(Product.HIDDEN);
    }

    private void apply(Product p, ProductRequest r) {
        requireIn(Vocab.CATEGORIES, List.of(r.category()), "Loại đồ");
        requireIn(Vocab.CONDITIONS, List.of(r.itemCondition()), "Độ mới");
        requireIn(Vocab.COLORS, r.colors(), "Màu sắc");
        requireIn(Vocab.STYLES, r.styles(), "Phong cách");
        requireIn(Vocab.OCCASIONS, r.occasions(), "Dịp");
        List<String> features = r.features() == null ? List.of() : r.features();
        requireIn(Vocab.FEATURES, features, "Kiểu dáng");
        p.setName(r.name().trim());
        p.setDescription(r.description().trim());
        p.setCategory(r.category());
        p.setSize(r.size());
        p.setBustMax(r.bustMax());
        p.setWaistMax(r.waistMax());
        p.setHipMax(r.hipMax());
        p.setItemCondition(r.itemCondition());
        p.setRetailPrice(r.retailPrice());
        p.setRentPricePerDay(r.rentPricePerDay());
        p.setDepositPercent(r.depositPercent());
        p.getTags().clear();
        Map<String, List<String>> byType = Map.of(Product.COLOR, r.colors(), Product.STYLE, r.styles(),
                Product.OCCASION, r.occasions(), Product.FEATURE, features);
        byType.forEach((type, values) -> values.forEach(v -> p.getTags().add(new Product.Tag(type, v))));
        // Ảnh mới thêm phải là ảnh đã upload lên R2 của mình (không nhận link ngoài)
        List<String> old = List.copyOf(p.getImages());
        for (String url : r.images())
            if (!old.contains(url) && !r2.isOurs(url)) throw new AppException(ErrorCode.INVALID_FILE, "Ảnh không hợp lệ: " + url);
        p.getImages().clear();
        p.getImages().addAll(r.images());

        // Ảnh bị gỡ → xoá khỏi R2 nếu không sản phẩm nào khác còn dùng
        List<String> removed = old.stream().filter(u -> !r.images().contains(u)).toList();
        if (!removed.isEmpty()) {
            productRepository.flush();
            r2.deleteAfterCommit(removed.stream().filter(u -> productRepository.countImageRefs(u) == 0).toList());
        }
    }

    private static void requireIn(List<String> vocab, List<String> values, String field) {
        for (String v : values)
            if (!vocab.contains(v)) throw new AppException(ErrorCode.VALIDATION_ERROR, field + " không hợp lệ: " + v);
    }

    // ── Admin kiểm duyệt ──────────────────────────────────────────────────
    public List<Detail> byStatus(String status) {
        return productRepository.findAll().stream()
                .filter(p -> status == null || p.getStatus().equals(status))
                .sorted(Comparator.comparing(Product::getCreatedAt))
                .map(this::toDetail).toList();
    }

    @Transactional
    public Detail review(UUID id, ReviewDecision d) {
        Product p = find(id);
        p.setStatus(d.approve() ? Product.APPROVED : Product.REJECTED);
        p.setRejectReason(d.approve() ? null : d.reason());
        mailer.send(p.getOwner().getEmail(),
                d.approve() ? "Món đồ của bạn đã được duyệt" : "Món đồ của bạn chưa được duyệt",
                "<p>Món <b>" + p.getName() + "</b> " + (d.approve()
                        ? "đã lên kệ và sẵn sàng cho thuê.</p>"
                        : "chưa được duyệt.</p><p>Lý do: " + org.springframework.web.util.HtmlUtils.htmlEscape(String.valueOf(d.reason())) + "</p>")
                        + "<p><a href=\"" + mailer.link("/account?tab=owner") + "\">Xem đồ của tôi</a></p>");
        return toDetail(p);
    }

    public Product find(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

}
