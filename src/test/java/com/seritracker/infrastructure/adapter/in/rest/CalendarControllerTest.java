package com.seritracker.infrastructure.adapter.in.rest;

import com.seritracker.domain.model.UpcomingEpisode;
import com.seritracker.domain.port.in.CalendarUseCase;
import com.seritracker.infrastructure.config.GlobalExceptionHandler;
import com.seritracker.infrastructure.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarController")
class CalendarControllerTest {

    @Mock private CalendarUseCase calendarUseCase;

    @InjectMocks private CalendarController calendarController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserPrincipal principal = new UserPrincipal(1L, "test@test.com", "hashed_password", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        mockMvc = MockMvcBuilders
                .standaloneSetup(calendarController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UpcomingEpisode buildUpcomingEpisode() {
        return UpcomingEpisode.builder()
                .userSeriesId(1L)
                .tmdbId(1396)
                .seriesTitle("Breaking Bad")
                .posterUrl("https://image.tmdb.org/t/p/w300/poster.jpg")
                .seasonNumber(6)
                .episodeNumber(1)
                .episodeTitle("Felina")
                .airDate(LocalDate.now().plusDays(1))
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/calendar/upcoming")
    class GetUpcoming {

        @Test
        @DisplayName("should return 200 with the list of upcoming episodes")
        void shouldReturn200_withUpcomingEpisodes() throws Exception {
            when(calendarUseCase.getUpcomingEpisodes(1L)).thenReturn(List.of(buildUpcomingEpisode()));

            mockMvc.perform(get("/api/v1/calendar/upcoming"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].seriesTitle").value("Breaking Bad"))
                    .andExpect(jsonPath("$.data[0].seasonNumber").value(6))
                    .andExpect(jsonPath("$.data[0].episodeNumber").value(1))
                    .andExpect(jsonPath("$.data[0].isTomorrow").value(true));
        }

        @Test
        @DisplayName("should return 200 with an empty list when there is nothing upcoming")
        void shouldReturn200_withEmptyList() throws Exception {
            when(calendarUseCase.getUpcomingEpisodes(1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/calendar/upcoming"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }
}
