package com.lovebox.modules.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 255) String name,
        @Pattern(regexp = "^$|^0\\d{9,10}$", message = "Số điện thoại không hợp lệ") String phone,
        @Size(max = 500) String address,
        @Pattern(regexp = "^$|^\\d{6,20}$", message = "Số tài khoản chỉ gồm 6-20 chữ số") String bankAccount,
        @Size(max = 100) String bankName) {}
