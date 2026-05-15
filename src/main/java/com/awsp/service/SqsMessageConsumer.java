package com.awsp.service;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Service;

@Service
public class SqsMessageConsumer {

    private final org.springframework.mail.javamail.JavaMailSender mailSender;

    public SqsMessageConsumer(org.springframework.mail.javamail.JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Spring Cloud AWS, kuyruktaki mesajları arkada otomatik olarak dinler.
    @SqsListener("${app.sqs.queue.registration}")
    public void receiveMessage(String message) {
        System.out.println("------------------------------------------------");
        System.out.println("🚀 [CONSUMER] SQS Kuyruğundan mesaj okundu!");
        
        try {
            String[] parts = message.split("\\|\\|\\|");
            if (parts.length != 2) {
                System.err.println("❌ Geçersiz mesaj formatı: " + message);
                return;
            }
            
            String email = parts[0];
            String fullName = parts[1];
            
            System.out.println("📩 Mail atılıyor -> Alıcı: " + email);

            org.springframework.mail.SimpleMailMessage mailMessage = new org.springframework.mail.SimpleMailMessage();
            mailMessage.setTo(email);
            mailMessage.setSubject("AWS CV Projesine Hoşgeldiniz!");
            mailMessage.setText("Merhaba " + fullName + ",\n\nAWS Spring Boot projesine başarıyla kayıt oldunuz.\nBu e-posta SQS kuyruğundan tetiklenerek asenkron olarak gönderilmiştir!\n\nSaygılarımızla,\nAWS CV Projesi");

            mailSender.send(mailMessage);
            
            System.out.println("✅ Gerçek mail başarıyla gönderildi: " + email);
        } catch (Exception e) {
            System.err.println("❌ Mail gönderme hatası: " + e.getMessage());
        }
        System.out.println("------------------------------------------------");
    }
}
