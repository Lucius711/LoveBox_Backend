package com.lovebox.modules.user.controller;

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

    @PostMapping("/become-owner")
    public ApiResponse<UserResponse> becomeOwner(@AuthenticationPrincipal UserPrincipal p) {
        return ApiResponse.success(userService.becomeOwner(p.getId()));
    }
}
