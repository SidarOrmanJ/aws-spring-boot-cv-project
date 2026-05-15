package com.awsp.controller;

import com.awsp.entity.UserEntity;
import com.awsp.repository.UserRepository;
import com.awsp.service.S3Service;
import com.awsp.service.SqsMessageProducer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final SqsMessageProducer sqsMessageProducer;

    public UserController(UserRepository userRepository, S3Service s3Service, SqsMessageProducer sqsMessageProducer) {
        this.userRepository = userRepository;
        this.s3Service = s3Service;
        this.sqsMessageProducer = sqsMessageProducer;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("profilePicture") MultipartFile profilePicture) {

        try {
            // 1. Resmi S3'e yükle ve sadece dosya anahtarını (key) al
            String s3Key = s3Service.uploadFile(profilePicture);

            // 2. Kullanıcı bilgilerini Veritabanına (RDS PostgreSQL) kaydet
            UserEntity user = new UserEntity();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setProfilePictureS3Key(s3Key);
            userRepository.save(user);

            // 3. Asenkron İşlem: Hoşgeldin Maili Görevi (SQS)
            // Kullanıcı mailin gitmesini beklemez, anında cevap alır.
            sqsMessageProducer.sendWelcomeEmailTask(email, fullName);

            return ResponseEntity.ok("Kullanıcı başarıyla kaydedildi! Kullanıcı ID: " + user.getId());

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Hata [S3 Dosya Yükleme]: " + e.getMessage());
        } catch (Exception e) {
            // SQS, DB gibi diğer servis hatalarını da yakala
            return ResponseEntity.internalServerError().body("Hata [Genel]: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/profile-picture")
    public ResponseEntity<String> getProfilePictureUrl(@PathVariable Long id) {
        // 1. Veritabanından (RDS) kullanıcıyı bul — bulunamazsa 404 fırlat
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + id));

        if (user.getProfilePictureS3Key() == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. S3 Service üzerinden 5 dakikalık geçici ve güvenli URL (Presigned URL) oluştur
        String presignedUrl = s3Service.getPresignedUrl(user.getProfilePictureS3Key());

        // Linki metin olarak geri dön
        return ResponseEntity.ok(presignedUrl);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        List<UserEntity> users = userRepository.findAll();
        List<Map<String, Object>> result = users.stream().map(u -> {
            String presignedUrl = u.getProfilePictureS3Key() != null
                    ? s3Service.getPresignedUrl(u.getProfilePictureS3Key()) : null;
            return Map.<String, Object>of(
                    "id", u.getId(),
                    "fullName", u.getFullName(),
                    "email", u.getEmail(),
                    "profilePictureUrl", presignedUrl != null ? presignedUrl : ""
            );
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/profile-picture/download")
    public ResponseEntity<byte[]> downloadProfilePicture(@PathVariable Long id) throws IOException {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + id));

        if (user.getProfilePictureS3Key() == null) {
            return ResponseEntity.notFound().build();
        }

        String s3Key = user.getProfilePictureS3Key();

        // Uzantıyı s3Key'den al ("uuid.jpg" -> ".jpg")
        String extension = s3Key.contains(".") ? s3Key.substring(s3Key.lastIndexOf(".")).toLowerCase() : "";

        // Temiz bir dosya adı üret (UUID olmadan)
        String cleanName = user.getFullName().replaceAll("[^a-zA-Z0-9çÇğğıİöÖşŞüÜ]", "_");
        String filename = cleanName + "_profil" + extension;

        // Uzantıya göre doğru Content-Type belirle
        MediaType mediaType = switch (extension) {
            case ".jpg", ".jpeg" -> MediaType.IMAGE_JPEG;
            case ".png"          -> MediaType.IMAGE_PNG;
            case ".gif"          -> MediaType.IMAGE_GIF;
            case ".webp"         -> MediaType.parseMediaType("image/webp");
            default              -> MediaType.APPLICATION_OCTET_STREAM;
        };

        byte[] fileBytes = s3Service.downloadFile(s3Key);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .contentLength(fileBytes.length)
                .body(fileBytes);
    }
}
