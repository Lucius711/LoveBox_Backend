package com.lovebox.modules.gift.serviceimpl;

import com.lovebox.common.constant.ErrorCode;
import com.lovebox.common.exception.AppException;
import com.lovebox.modules.gift.dto.request.CreateGiftDesignRequest;
import com.lovebox.modules.gift.dto.request.CreateGreetingWishRequest;
import com.lovebox.modules.gift.dto.response.GiftDesignResponse;
import com.lovebox.modules.gift.dto.response.GreetingWishResponse;
import com.lovebox.modules.gift.entity.GiftDesign;
import com.lovebox.modules.gift.entity.GreetingWish;
import com.lovebox.modules.gift.repository.GiftDesignRepository;
import com.lovebox.modules.gift.repository.GreetingWishRepository;
import com.lovebox.modules.gift.service.GiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class GiftServiceImpl implements GiftService {

    private final GiftDesignRepository giftDesignRepository;
    private final GreetingWishRepository greetingWishRepository;

    @Override
    @Transactional
    public GiftDesignResponse createGiftDesign(UUID userId, CreateGiftDesignRequest request) {
        validateGiftDesignSource(request);

        GiftDesign design = GiftDesign.builder()
                .userId(userId)
                .sourceType(request.getSourceType())
                .aiGeneratedImageId(request.getAiGeneratedImageId() != null
                        ? UUID.fromString(request.getAiGeneratedImageId()) : null)
                .showcaseDesignId(request.getShowcaseDesignId() != null
                        ? UUID.fromString(request.getShowcaseDesignId()) : null)
                .customizationData(request.getCustomizationData())
                .status("DRAFT")
                .build();

        return GiftDesignResponse.from(giftDesignRepository.save(design));
    }

    @Override
    public List<GiftDesignResponse> listGiftDesigns(UUID userId) {
        return giftDesignRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(GiftDesignResponse::from).toList();
    }

    @Override
    public GiftDesignResponse getGiftDesign(UUID id, UUID userId) {
        GiftDesign design = giftDesignRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.GIFT_DESIGN_NOT_FOUND));
        if (!design.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.GIFT_DESIGN_FORBIDDEN);
        }
        return GiftDesignResponse.from(design);
    }

    @Override
    @Transactional
    public GreetingWishResponse createGreetingWish(UUID userId, CreateGreetingWishRequest request) {
        GreetingWish wish = GreetingWish.builder()
                .userId(userId)
                .recipientName(request.getRecipientName())
                .message(request.getMessage())
                .senderName(request.getSenderName())
                .qrToken(UUID.randomUUID())
                .build();
        return GreetingWishResponse.from(greetingWishRepository.save(wish));
    }

    @Override
    public GreetingWishResponse getGreetingWishByQrToken(UUID qrToken) {
        return GreetingWishResponse.from(
                greetingWishRepository.findByQrToken(qrToken)
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_QR_TOKEN)));
    }

    private void validateGiftDesignSource(CreateGiftDesignRequest request) {
        if ("AI_GENERATED".equals(request.getSourceType())) {
            if (!StringUtils.hasText(request.getAiGeneratedImageId())) {
                throw new AppException(ErrorCode.INVALID_GIFT_DESIGN_SOURCE,
                        "AI_GENERATED yêu cầu aiGeneratedImageId");
            }
        } else if ("SHOWCASE".equals(request.getSourceType())) {
            if (!StringUtils.hasText(request.getShowcaseDesignId())) {
                throw new AppException(ErrorCode.INVALID_GIFT_DESIGN_SOURCE,
                        "SHOWCASE yêu cầu showcaseDesignId");
            }
        } else {
            throw new AppException(ErrorCode.INVALID_GIFT_DESIGN_SOURCE);
        }
    }
}
