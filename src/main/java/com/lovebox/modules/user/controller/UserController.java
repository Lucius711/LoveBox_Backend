package com.lovebox.modules.user.controller;

import com.lovebox.modules.owner.OwnerApplicationService;
import com.lovebox.common.response.ApiResponse;
import com.lovebox.modules.user.dto.request.StyleProfileRequest;
import com.lovebox.modules.user.dto.request.UpdateProfileRequest;
import com.lovebox.modules.user.dto.response.UserResponse;
import com.lovebox.modules.user.service.UserService;
import com.lovebox.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OwnerApplicationService ownerApplications;

    @GetMapping
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserPrincipal p) {
        return ApiResponse.success(userService.getProfile(p.getId()));
    }

    @PatchMapping
    public ApiResponse<UserResponse> update(@AuthenticationPrincipal UserPrincipal p,
                                            @Valid @RequestBody UpdateProfileRequest req) {
        return ApiResponse.success(userService.updateProfile(p.getId(), req));
    }

    @PutMapping("/style-profile")
    public ApiResponse<UserResponse> styleProfile(@AuthenticationPrincipal UserPrincipal p,
                                                  @Valid @RequestBody StyleProfileRequest req) {
        return ApiResponse.success(userService.saveStyleProfile(p.getId(), req));
    }

    /** Đơn đăng ký làm Chủ đồ gần nhất (null nếu chưa gửi). */
    @GetMapping("/owner-application")
    public ApiResponse<OwnerApplicationService.View> ownerApplication(@AuthenticationPrincipal UserPrincipal p) {
        return ApiResponse.success(ownerApplications.mine(p.getId()));
    }

    @PostMapping("/owner-application")
    public ApiResponse<OwnerApplicationService.View> applyOwner(@AuthenticationPrincipal UserPrincipal p,
                                                                @Valid @RequestBody OwnerApplicationService.Submit req) {
        return ApiResponse.success("Đã gửi đơn, admin sẽ duyệt sớm", ownerApplications.submit(p.getId(), req));
    }
}
