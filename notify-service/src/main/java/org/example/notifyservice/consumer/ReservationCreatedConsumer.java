package org.example.notifyservice.consumer;

import org.example.notifyservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReservationCreatedConsumer {

    private final EmailService emailService;

    public ReservationCreatedConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    private static final Logger log = LoggerFactory.getLogger(ReservationCreatedConsumer.class);

    @KafkaListener(topics = "${notification.kafka.reservation-created-topic:resvervation-created}")
    public void consume(String email) {
        log.info("Received reservation created message for email: {}", email);
        if (StringUtils.hasText(email)) {
            emailService.sendReservationCreatedEmail(email.trim());
        }
    }
}
