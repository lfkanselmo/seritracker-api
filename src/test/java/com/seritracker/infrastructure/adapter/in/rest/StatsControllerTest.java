package com.seritracker.infrastructure.adapter.in.rest;

import com.seritracker.domain.model.Badge;
import com.seritracker.domain.model.BadgeCode;
import com.seritracker.domain.model.GenreStat;
import com.seritracker.domain.model.UserStats;
import com.seritracker.domain.model.YearSummary;
import com.seritracker.domain.port.in.StatsUseCase;
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

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatsController")
class StatsControllerTest {

    @Mock private StatsUseCase statsUseCase;

    @InjectMocks private StatsController statsController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserPrincipal principal = new UserPrincipal(1L, "test@test.com", "hashed_password", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        mockMvc = MockMvcBuilders
                .standaloneSetup(statsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserStats buildStats() {
        return UserStats.builder()
                .totalEpisodesWatched(120)
                .totalMinutesWatched(5400)
                .totalSeriesTracked(8)
                .totalSeriesCompleted(3)
                .currentStreakDays(5)
                .badges(List.of(Badge.builder()
                        .code(BadgeCode.FIRST_EPISODE)
                        .earned(true)
                        .progressCurrent(1)
                        .progressTarget(1)
                        .build()))
                .currentYear(YearSummary.builder()
                        .year(2026)
                        .episodesWatched(40)
                        .topGenres(List.of(GenreStat.builder().genre("Drama").episodeCount(30).build()))
                        .mostWatchedSeriesTitle("Breaking Bad")
                        .mostWatchedSeriesEpisodeCount(20)
                        .longestStreakDays(5)
                        .build())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/stats")
    class GetStats {

        @Test
        @DisplayName("should return 200 with the user's stats")
        void shouldReturn200_withStats() throws Exception {
            when(statsUseCase.getStats(1L)).thenReturn(buildStats());

            mockMvc.perform(get("/api/v1/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalEpisodesWatched").value(120))
                    .andExpect(jsonPath("$.data.totalMinutesWatched").value(5400))
                    .andExpect(jsonPath("$.data.currentYear.mostWatchedSeriesTitle").value("Breaking Bad"))
                    .andExpect(jsonPath("$.data.currentYear.topGenres[0].genre").value("Drama"))
                    .andExpect(jsonPath("$.data.currentStreakDays").value(5))
                    .andExpect(jsonPath("$.data.badges[0].code").value("FIRST_EPISODE"))
                    .andExpect(jsonPath("$.data.badges[0].earned").value(true));
        }
    }
}
