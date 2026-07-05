package com.seritracker.infrastructure.adapter.out.email;

import com.seritracker.domain.port.out.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpEmailSenderAdapter implements EmailSender {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String from;

    @Override
    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            log.debug("Sending email to {} with subject '{}'", to, subject);
            mailSender.send(message);
            log.info("Email sent successfully");
        } catch (Exception e) {
            // No propagamos — el flujo de forgot-password nunca debe revelar
            // si el envío falló, para no filtrar qué emails existen.
            log.error("Failed to send email", e);
        }
    }
}
