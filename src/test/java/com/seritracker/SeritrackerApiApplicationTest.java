package com.seritracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SeritrackerApiApplication")
class SeritrackerApiApplicationTest {

    @Test
    @DisplayName("context should load successfully")
    void contextLoads() {
    }
}