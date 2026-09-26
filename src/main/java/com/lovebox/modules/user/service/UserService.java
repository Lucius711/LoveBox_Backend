package com.lovebox.modules.user.service;

import com.lovebox.common.constant.ErrorCode;
import com.lovebox.common.exception.AppException;
import com.lovebox.modules.product.Vocab;
import com.lovebox.modules.user.dto.request.StyleProfileRequest;
import com.lovebox.modules.user.dto.request.UpdateProfileRequest;
import com.lovebox.modules.user.dto.response.UserResponse;
import com.lovebox.modules.user.entity.User;
import com.lovebox.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User get(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getDeletedAt() != null) throw new AppException(ErrorCode.USER_ALREADY_DELETED);
        return user;
    }

    public UserResponse getProfile(UUID userId) {
        return UserResponse.from(get(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User u = get(userId);
        if (req.name() != null && !req.name().isBlank()) u.setName(req.name().trim());
        if (req.phone() != null) u.setPhone(req.phone());
        if (req.address() != null) u.setAddress(req.address());
        if (req.bankAccount() != null) u.setBankAccount(req.bankAccount());
        if (req.bankName() != null) {
            if (!req.bankName().isBlank() && !Vocab.BANKS.containsKey(req.bankName()))
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Ngân hàng không hỗ trợ: " + req.bankName());
            u.setBankName(req.bankName());
        }
        return UserResponse.from(u);
    }

    /** Lưu hồ sơ phong cách (onboarding lần đầu hoặc sửa ở trang hồ sơ). Ghi đè toàn bộ. */
    @Transactional
    public UserResponse saveStyleProfile(UUID userId, StyleProfileRequest r) {
        User u = get(userId);
        u.setHeightCm(r.heightCm());
        u.setWeightKg(r.weightKg());
        u.setBust(r.bust());
        u.setWaist(r.waist());
        u.setHip(r.hip());
        u.setClothingSize(r.clothingSize());
        u.setBudgetMax(r.budgetMax() == null || r.budgetMax() == 0 ? null : r.budgetMax());
        u.setFavOccasions(pick(r.favOccasions(), Vocab.OCCASIONS));
        u.setFavStyles(pick(r.favStyles(), Vocab.STYLES));
        u.setFavColors(pick(r.favColors(), Vocab.COLORS));
        u.setFitNote(r.fitNote() == null || r.fitNote().isBlank() ? null : r.fitNote().trim());
        if (u.getOnboardedAt() == null) u.setOnboardedAt(Instant.now());
        return UserResponse.from(u);
    }

    private static List<String> pick(List<String> values, List<String> vocab) {
        return values == null ? List.of() : values.stream().filter(vocab::contains).distinct().toList();
    }

    /** Renter → Owner: bật tính năng "Đăng đồ cho thuê". Admin giữ nguyên. */
    @Transactional
    public UserResponse becomeOwner(UUID userId) {
        User u = get(userId);
        if (User.RENTER.equals(u.getRole())) u.setRole(User.OWNER);
        return UserResponse.from(u);
    }
}
