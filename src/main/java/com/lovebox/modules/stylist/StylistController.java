package com.lovebox.modules.stylist;

import com.lovebox.common.response.ApiResponse;
import com.lovebox.modules.product.ProductDtos.PageResult;
import com.lovebox.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Cần đăng nhập: số đo, ngân sách, sở thích lấy từ hồ sơ onboarding. */
@RestController
@RequestMapping("/stylist")
@RequiredArgsConstructor
public class StylistController {

    private final StylistService stylistService;

    /** chatId trống → mở cuộc chat mới; có → nói tiếp cuộc cũ. */
    @PostMapping("/search")
    public ApiResponse<StylistService.ChatReply> search(@AuthenticationPrincipal UserPrincipal p,
                                                        @Valid @RequestBody StylistService.Request req) {
        return ApiResponse.success(stylistService.chat(p.getId(), req.chatId(), req.prompt()));
    }

    @GetMapping("/for-you")
    public ApiResponse<List<StylistService.Match>> forYou(@AuthenticationPrincipal UserPrincipal p) {
        return ApiResponse.success(stylistService.recommend(p.getId()));
    }

    @GetMapping("/chats")
    public ApiResponse<PageResult<StylistService.ChatSummary>> chats(@AuthenticationPrincipal UserPrincipal p,
                                                                     @RequestParam(defaultValue = "false") boolean archived,
                                                                     @RequestParam(defaultValue = "1") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(stylistService.chats(p.getId(), archived, page, size));
    }

    @GetMapping("/chats/{id}")
    public ApiResponse<StylistService.ChatDetail> chat(@AuthenticationPrincipal UserPrincipal p, @PathVariable UUID id) {
        return ApiResponse.success(stylistService.chatDetail(p.getId(), id));
    }

    @PatchMapping("/chats/{id}")
    public ApiResponse<StylistService.ChatSummary> archive(@AuthenticationPrincipal UserPrincipal p, @PathVariable UUID id,
                                                           @RequestBody StylistService.ArchiveRequest req) {
        return ApiResponse.success(stylistService.archive(p.getId(), id, req.archived()));
    }
}
