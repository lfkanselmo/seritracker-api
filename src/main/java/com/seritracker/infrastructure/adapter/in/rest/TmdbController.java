package com.seritracker.infrastructure.adapter.in.rest;

import com.seritracker.domain.model.Series;
import com.seritracker.domain.port.out.TmdbClient;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.TmdbSeriesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tmdb")
@RequiredArgsConstructor
@Tag(name = "TMDB", description = "Búsqueda de series en The Movie Database")
public class TmdbController {

    private final TmdbClient tmdbClient;

    @Operation(summary = "Buscar series por nombre")
    @GetMapping("/search")
    public ApiResponse<List<TmdbSeriesResponse>> search(@RequestParam String q) {
        List<Series> results = tmdbClient.searchSeries(q);
        return ApiResponse.ok(results.stream()
                .map(TmdbSeriesResponse::from)
                .toList());
    }

    @Operation(summary = "Obtener detalle de una serie por tmdbId")
    @GetMapping("/series/{tmdbId}")
    public ApiResponse<TmdbSeriesResponse> getDetails(@PathVariable Integer tmdbId) {
        Series series = tmdbClient.getSeriesDetails(tmdbId);
        return ApiResponse.ok(TmdbSeriesResponse.from(series));
    }
}