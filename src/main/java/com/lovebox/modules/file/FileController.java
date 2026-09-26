package com.lovebox.modules.file;

import com.lovebox.common.constant.ErrorCode;
import com.lovebox.common.exception.AppException;
import com.lovebox.common.response.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Trình duyệt xin URL có chữ ký rồi PUT ảnh thẳng lên R2 — ảnh không đi qua backend. */
@RestController
@RequiredArgsConstructor
public class FileController {

    private static final Map<String, String> EXT = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");
    private static final long MAX_BYTES = 5 * 1024 * 1024;

    private final R2Storage r2;

    public record PresignRequest(@NotBlank String contentType, @Positive long size) {}

    @PostMapping("/files/presign")
    public ApiResponse<R2Storage.Presigned> presign(@RequestBody PresignRequest req) {
        String ext = EXT.get(req.contentType());
        if (ext == null || req.size() <= 0 || req.size() > MAX_BYTES) throw new AppException(ErrorCode.INVALID_FILE);
        return ApiResponse.success(r2.presignUpload(ext, req.contentType(), req.size()));
    }
}
