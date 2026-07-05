package com.seritracker.infrastructure.adapter.in.rest.dto.response;

import com.seritracker.domain.model.EpisodeInfo;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SeasonEpisodesResponse {

    List<EpisodeResponse> episodes;

    public static SeasonEpisodesResponse from(List<EpisodeInfo> domain) {
        return SeasonEpisodesResponse.builder()
                .episodes(domain.stream().map(EpisodeResponse::from).toList())
                .build();
    }
}
