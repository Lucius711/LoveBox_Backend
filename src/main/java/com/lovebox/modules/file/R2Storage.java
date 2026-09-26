package com.lovebox.modules.file;

import com.lovebox.common.util.AfterCommit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.UUID;

/** Cloudflare R2 (S3-compatible): cấp URL upload có chữ ký cho trình duyệt + xoá ảnh không còn dùng. */
@Slf4j
@Component
public class R2Storage {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final String publicUrl;

    public R2Storage(@Value("${app.r2.account-id}") String accountId,
                     @Value("${app.r2.access-key-id}") String accessKey,
                     @Value("${app.r2.secret-access-key}") String secretKey,
                     @Value("${app.r2.bucket}") String bucket,
                     @Value("${app.r2.public-url}") String publicUrl) {
        URI endpoint = URI.create("https://" + accountId + ".r2.cloudflarestorage.com");
        var creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        this.bucket = bucket;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
        this.s3 = S3Client.builder().endpointOverride(endpoint).region(Region.of("auto")).credentialsProvider(creds)
                // R2 không cần checksum mặc định của SDK ≥2.30
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
        this.presigner = S3Presigner.builder().endpointOverride(endpoint).region(Region.of("auto")).credentialsProvider(creds).build();
    }

    public record Presigned(String uploadUrl, String publicUrl) {}

    /**
     * URL PUT có hạn 5 phút. Content-Type và Content-Length nằm trong chữ ký → trình duyệt
     * không đổi được loại file hay upload file lớn hơn kích thước đã khai.
     */
    public Presigned presignUpload(String ext, String contentType, long size) {
        String key = "products/" + UUID.randomUUID() + ext;
        PutObjectRequest put = PutObjectRequest.builder().bucket(bucket).key(key)
                .contentType(contentType).contentLength(size)
                .cacheControl("public, max-age=31536000, immutable")   // tên file ngẫu nhiên → cache vĩnh viễn
                .build();
        String url = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5)).putObjectRequest(put).build()).url().toString();
        return new Presigned(url, publicUrl + "/" + key);
    }

    /** Ảnh này có nằm trong bucket của mình không (chặn gắn link ảnh ngoài vào sản phẩm). */
    public boolean isOurs(String url) {
        return url != null && url.startsWith(publicUrl + "/products/");
    }

    /** Xoá sau khi transaction commit (rollback thì giữ ảnh). Lỗi chỉ log — tệ nhất là sót 1 file rác. */
    public void deleteAfterCommit(Collection<String> urls) {
        var keys = urls.stream().filter(this::isOurs).map(u -> u.substring(publicUrl.length() + 1)).toList();
        if (keys.isEmpty()) return;
        Runnable job = () -> keys.forEach(k -> {
            try {
                s3.deleteObject(b -> b.bucket(bucket).key(k));
            } catch (Exception e) {
                log.warn("[R2] xoá {} lỗi: {}", k, e.getMessage());
            }
        });
        AfterCommit.run(job);
    }
}
