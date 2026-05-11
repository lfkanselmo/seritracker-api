package com.seritracker.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tmdb_id", nullable = false)
    private Integer tmdbId;

    @Column(name = "series_title", nullable = false)
    private String seriesTitle;

    @Column(name = "episode_code")
    private String episodeCode;

    @Column(name = "air_date", nullable = false)
    private LocalDate airDate;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private Boolean read;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
        read   = false;
    }
}