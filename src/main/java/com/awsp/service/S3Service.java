package com.awsp.service;

import io.awspring.cloud.s3.S3Template;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Template s3Template;
    private final S3Presigner s3Presigner;

    @Value("${app.s3.bucket}")
    private String bucketName;

    public S3Service(S3Template s3Template, S3Presigner s3Presigner) {
        this.s3Template = s3Template;
        this.s3Presigner = s3Presigner;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // Benzersiz bir dosya adı üret (UUID) - güvenlik ve çakışmayı önlemek için
        String s3Key = UUID.randomUUID().toString() + extension;

        // Dosyayı S3'e yükle (Spring Cloud AWS S3Template ile tek satırda!)
        s3Template.upload(bucketName, s3Key, file.getInputStream());

        return s3Key; // Veritabanına dosyanın tamamını değil, sadece bu anahtarı (key) kaydedeceğiz
    }

    public String getPresignedUrl(String s3Key) {
        // Sadece 5 dakika (300 saniye) geçerli olacak, sonrasında geçersiz olacak bir link üret
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest =
                s3Presigner.presignGetObject(getObjectPresignRequest);

        return presignedGetObjectRequest.url().toString();
    }

    public byte[] downloadFile(String s3Key) throws IOException {
        return s3Template.download(bucketName, s3Key).getInputStream().readAllBytes();
    }

    public void deleteFile(String s3Key) {
        if (s3Key != null && !s3Key.isEmpty()) {
            s3Template.deleteObject(bucketName, s3Key);
        }
    }
}
