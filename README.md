# AWS Cloud-Native Spring Boot Portfolio Project ☁️

**[Canlı Demo Linki]** 👉 `http://52.59.200.223:8080`
*(Not: AWS Free Tier limitleri sebebiyle sunucu ileride kapatılabilir, detaylar için aşağıdaki ekran görüntülerine bakabilirsiniz.)*

Bu proje, modern bulut mimarisi konseptlerini (Cloud-Native) kullanarak geliştirilmiş, uçtan uca bir "Kullanıcı Kayıt ve Profil Yönetim Sistemi"dir. Monolitik bir yapıdan ziyade, servislerin birbirinden bağımsız (decoupled) çalıştığı asenkron bir AWS mimarisi tasarlanmıştır.

### 🏗️ Mimaride Kullanılan AWS Servisleri & Kararlar

*   **Amazon EC2 (Linux):** Projenin canlıya alındığı ana sunucudur. Uygulama arka planda (`nohup`) güvenli bir şekilde koşturulmaktadır.
*   **Amazon S3 (Simple Storage Service):** Kullanıcı profil fotoğrafları güvenlik sebebiyle dışarıya kapalı (Private) bir bucket'ta tutulmaktadır. Tarayıcıya resimler **Pre-signed URL (Geçici İmzalı Link)** mantığıyla gönderilir; bu da veri hırsızlığını ve "hotlinking"i önler. Çakışmaları önlemek için dosya isimleri S3'e yüklenirken UUID ile şifrelenir.
*   **Amazon SQS (Simple Queue Service):** Asenkron işlem yönetimi. Kullanıcı kayıt olduğunda "Hoşgeldin E-postası" atma işlemi anlık olarak yapılmaz. Görev, SQS kuyruğuna fırlatılır ve arka planda çalışan bir Consumer (Dinleyici) mesajı alıp `JavaMailSender` (SMTP) aracılığıyla gönderimi tamamlar. Bu sayede kullanıcı, mailin gitmesini beklerken arayüzde donma/bekleme yaşamaz.
*   **Amazon RDS (PostgreSQL):** İlişkisel veritabanı yönetimi. Uygulama verileri bulut üzerinde güvenli, yedekli ve ölçeklenebilir bir PostgreSQL veritabanında saklanır.

### 🛠️ Teknolojiler

*   **Backend:** Java 21, Spring Boot 3
*   **AWS Entegrasyonu:** Spring Cloud AWS
*   **Veritabanı ORM:** Spring Data JPA, Hibernate
*   **Frontend:** HTML5, CSS3, Vanilla JS, Flexbox (Modern Dark UI)
*   **Versiyon Kontrol & Deployment:** Git, Maven, SSH/SCP ile manuel EC2 deployment
