package com.awsp.controller;

import com.awsp.entity.UserEntity;
import com.awsp.repository.UserRepository;
import com.awsp.service.S3Service;
import com.awsp.service.SqsMessageProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
}
