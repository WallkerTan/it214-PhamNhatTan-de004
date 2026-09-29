package org.example.notifyservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${notification.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendReservationCreatedEmail(String recipient) {
        if (!StringUtils.hasText(recipient)) {
            throw new IllegalArgumentException("Recipient email must not be blank");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(from)) {
            message.setFrom(from);
        }
        message.setTo(recipient.trim());
        message.setSubject("Đặt phòng thành công");
        message.setText("Đơn đặt phòng của quý khách đã được tạo thành công.");
        mailSender.send(message);
    }
}
