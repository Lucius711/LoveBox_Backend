package com.lovebox.modules.user.service;

import com.lovebox.modules.user.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {
    UserResponse getProfile(UUID userId);
}
