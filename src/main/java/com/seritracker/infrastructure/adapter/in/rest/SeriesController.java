package com.seritracker.infrastructure.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.seritracker.domain.model.PageRequest;
import com.seritracker.domain.model.PageResult;
import com.seritracker.domain.model.SeriesSortBy;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.SortDirection;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.model.EpisodeInfo;
import com.seritracker.domain.model.SeriesEpisodesSummary;
import com.seritracker.domain.port.in.CreateSeriesUseCase;
import com.seritracker.domain.port.in.DeleteSeriesUseCase;
import com.seritracker.domain.port.in.EpisodeTrackingUseCase;
import com.seritracker.domain.port.in.SearchSeriesUseCase;
import com.seritracker.domain.port.in.UpdateSeriesUseCase;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.CreateSeriesRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.MarkEpisodeRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.MarkSeasonRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.UpdateNotesRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.UpdateRatingRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.UpdateStatusRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.PageResponse;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.SeasonEpisodesResponse;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.SeasonsSummaryResponse;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.SeriesResponse;
import com.seritracker.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/series")
@RequiredArgsConstructor
@Validated
@Tag(name = "Series", description = "Gestión de series del usuario")
public class SeriesController {

    private final CreateSeriesUseCase createSeriesUseCase;
    private final UpdateSeriesUseCase updateSeriesUseCase;
    private final DeleteSeriesUseCase deleteSeriesUseCase;
    private final SearchSeriesUseCase searchSeriesUseCase;
    private final EpisodeTrackingUseCase episodeTrackingUseCase;

    @Operation(summary = "Listar las series del usuario (paginado, con búsqueda y orden opcionales)")
    @GetMapping
    public ApiResponse<PageResponse<SeriesResponse>> listAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) SeriesStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SeriesSortBy sortBy,
            @RequestParam(required = false, defaultValue = "DESC") SortDirection sortDir,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        Long userId = principal.getId();
        PageRequest pageRequest = PageRequest.of(
                page, size, search,
                sortBy != null ? sortBy.getFieldName() : null,
                sortBy != null ? sortDir : null
        );
        PageResult<UserSeries> result = (status != null)
                ? searchSeriesUseCase.listByStatus(userId, status, pageRequest)
                : searchSeriesUseCase.listAllByUser(userId, pageRequest);

        return ApiResponse.ok(PageResponse.from(result, SeriesResponse::from));
    }

    @Operation(summary = "Obtener detalle de una serie")
    @GetMapping("/{id}")
    public ApiResponse<SeriesResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ApiResponse.ok(
                SeriesResponse.from(searchSeriesUseCase.getById(principal.getId(), id))
        );
    }

    @Operation(summary = "Agregar una serie a la lista")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SeriesResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateSeriesRequest request) {

        UserSeries created = createSeriesUseCase.createSeries(
                principal.getId(),
                request.getTmdbId(),
                request.getStatus() != null ? request.getStatus() : SeriesStatus.WANT_TO_WATCH
        );

        return ApiResponse.created(SeriesResponse.from(created));
    }

    @Operation(summary = "Actualizar estado de una serie")
    @PatchMapping("/{id}/status")
    public ApiResponse<SeriesResponse> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {

        return ApiResponse.ok(SeriesResponse.from(
                updateSeriesUseCase.updateStatus(principal.getId(), id, request.getStatus())
        ));
    }

    @Operation(summary = "Calificar una serie")
    @PatchMapping("/{id}/rating")
    public ApiResponse<SeriesResponse> updateRating(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRatingRequest request) {

        return ApiResponse.ok(SeriesResponse.from(
                updateSeriesUseCase.updateRating(principal.getId(), id, request.getRating())
        ));
    }

    @Operation(summary = "Obtener resumen de temporadas y próximo episodio a ver")
    @GetMapping("/{id}/seasons")
    public ApiResponse<SeasonsSummaryResponse> getSeasonsSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {

        SeriesEpisodesSummary summary = episodeTrackingUseCase.getSeasonsSummary(principal.getId(), id);
        return ApiResponse.ok(SeasonsSummaryResponse.from(summary));
    }

    @Operation(summary = "Obtener episodios de una temporada con su estado de visto")
    @GetMapping("/{id}/seasons/{seasonNumber}/episodes")
    public ApiResponse<SeasonEpisodesResponse> getSeasonEpisodes(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Integer seasonNumber) {

        List<EpisodeInfo> episodes = episodeTrackingUseCase.getSeasonEpisodes(principal.getId(), id, seasonNumber);
        return ApiResponse.ok(SeasonEpisodesResponse.from(episodes));
    }

    @Operation(summary = "Marcar un episodio como visto/no visto")
    @PatchMapping("/{id}/seasons/{seasonNumber}/episodes/{episodeNumber}")
    public ApiResponse<SeriesResponse> markEpisode(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Integer seasonNumber,
            @PathVariable Integer episodeNumber,
            @Valid @RequestBody MarkEpisodeRequest request) {

        return ApiResponse.ok(SeriesResponse.from(
                episodeTrackingUseCase.markEpisode(principal.getId(), id, seasonNumber, episodeNumber, request.getWatched())
        ));
    }

    @Operation(summary = "Marcar una temporada completa como vista")
    @PatchMapping("/{id}/seasons/{seasonNumber}/watch-all")
    public ApiResponse<SeriesResponse> markSeasonWatched(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Integer seasonNumber,
            @Valid @RequestBody MarkSeasonRequest request) {

        return ApiResponse.ok(SeriesResponse.from(
                episodeTrackingUseCase.markEpisodesWatched(principal.getId(), id, seasonNumber, request.getEpisodeNumbers())
        ));
    }

    @Operation(summary = "Actualizar notas de una serie")
    @PatchMapping("/{id}/notes")
    public ApiResponse<SeriesResponse> updateNotes(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateNotesRequest request) {

        return ApiResponse.ok(SeriesResponse.from(
                updateSeriesUseCase.updateNotes(principal.getId(), id, request.getNotes())
        ));
    }

    @Operation(summary = "Eliminar una serie de la lista")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        deleteSeriesUseCase.deleteSeries(principal.getId(), id);
        return ApiResponse.noContent();
    }
}