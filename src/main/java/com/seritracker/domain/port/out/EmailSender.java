package com.seritracker.domain.port.out;

public interface EmailSender {
    void send(String to, String subject, String body);
}
