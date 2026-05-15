package com.awsp.service;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Service;

@Service
public class SqsMessageConsumer {

    // Spring Cloud AWS, kuyruktaki mesajları arkada otomatik olarak dinler.
    @SqsListener("${app.sqs.queue.registration}")
    public void receiveMessage(String message) {
        System.out.println("------------------------------------------------");
        System.out.println("🚀 [CONSUMER - ASENKRON İŞLEM] SQS Kuyruğundan mesaj okundu!");
        System.out.println("📩 İşlenen Mesaj: " + message);
        
        // Burada Amazon SES (Simple Email Service) ile gerçekten mail attığımızı varsayalım.
        // Mail atma işleminin vakit aldığını simüle etmek için 2 saniye bekletiyoruz.
        try {
            Thread.sleep(2000); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("✅ Mail başarıyla gönderildi!");
        System.out.println("------------------------------------------------");
    }
}
