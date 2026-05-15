package com.awsp.service;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SqsMessageProducer {

    private final SqsTemplate sqsTemplate;

    @Value("${app.sqs.queue.registration}")
    private String queueName;

    public SqsMessageProducer(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    public void sendWelcomeEmailTask(String email, String fullName) {
        // Gerçek bir senaryoda JSON objesi gönderilir, biz basitlik için metin gönderiyoruz.
        String messagePayload = String.format("GÖREV: Hoşgeldin Maili Gönder | ALICI: %s | İSİM: %s", email, fullName);
        
        sqsTemplate.send(queueName, messagePayload);
        System.out.println("📤 [PRODUCER] Mesaj SQS kuyruğuna başarıyla bırakıldı: " + messagePayload);
    }
}
