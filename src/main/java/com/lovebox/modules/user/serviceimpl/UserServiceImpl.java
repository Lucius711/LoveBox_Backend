package com.lovebox.modules.user.serviceimpl;

import com.lovebox.common.constant.ErrorCode;
import com.lovebox.common.exception.AppException;
import com.lovebox.modules.user.dto.response.UserResponse;
import com.lovebox.modules.user.entity.User;
import com.lovebox.modules.user.repository.UserRepository;
import com.lovebox.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getDeletedAt() != null) {
            throw new AppException(ErrorCode.USER_ALREADY_DELETED);
        }
        return UserResponse.from(user);
    }
}
