package com.seritracker.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_series",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tmdb_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSeriesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tmdb_id", nullable = false)
    private Integer tmdbId;

    @Column(nullable = false)
    private String title;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(nullable = false)
    private String status;

    @Column
    private Integer rating;

    @Column(name = "watched_episodes", nullable = false)
    private Integer watchedEpisodes;

    @Column(name = "total_episodes", nullable = false)
    private Integer totalEpisodes;

    @Column
    private String network;

    @Column
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}