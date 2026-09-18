package com.lovebox.modules.user.controller;

import com.lovebox.common.response.ApiResponse;
import com.lovebox.modules.user.dto.response.UserResponse;
import com.lovebox.modules.user.service.UserService;
import com.lovebox.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(userService.getProfile(principal.getId())));
    }
}
