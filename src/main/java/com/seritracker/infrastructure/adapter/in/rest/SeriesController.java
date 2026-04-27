package com.seritracker.infrastructure.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.seritracker.domain.model.SeriesStatus;
import com.seritracker.domain.model.UserSeries;
import com.seritracker.domain.port.in.CreateSeriesUseCase;
import com.seritracker.domain.port.in.DeleteSeriesUseCase;
import com.seritracker.domain.port.in.SearchSeriesUseCase;
import com.seritracker.domain.port.in.UpdateSeriesUseCase;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.CreateSeriesRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.UpdateEpisodesRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.UpdateRatingRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.request.UpdateStatusRequest;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import com.seritracker.infrastructure.adapter.in.rest.dto.response.SeriesResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/series")
@RequiredArgsConstructor
@Tag(name = "Series", description = "Gestión de series del usuario")
public class SeriesController {

    // Usamos los puertos, nunca el servicio directamente
    private final CreateSeriesUseCase createSeriesUseCase;
    private final UpdateSeriesUseCase updateSeriesUseCase;
    private final DeleteSeriesUseCase deleteSeriesUseCase;
    private final SearchSeriesUseCase searchSeriesUseCase;

    @Operation(summary = "Listar todas las series del usuario")
    @GetMapping
    public ApiResponse<List<SeriesResponse>> listAll(
            @RequestParam Long userId,
            @RequestParam(required = false) String status) {

        List<UserSeries> result = (status != null)
                ? searchSeriesUseCase.listByStatus(userId, SeriesStatus.valueOf(status))
                : searchSeriesUseCase.listAllByUser(userId);

        return ApiResponse.ok(result.stream()
                .map(SeriesResponse::from)
                .toList());
    }

    @Operation(summary = "Obtener detalle de una serie")
    @GetMapping("/{id}")
    public ApiResponse<SeriesResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(
                SeriesResponse.from(searchSeriesUseCase.getById(id))
        );
    }

    @Operation(summary = "Agregar una serie a la lista")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SeriesResponse> create(
            @RequestParam Long userId,
            @Valid @RequestBody CreateSeriesRequest request) {

        UserSeries created = createSeriesUseCase.createSeries(
                userId,
                request.getTmdbId(),
                request.getStatus() != null ? request.getStatus() : "WANT_TO_WATCH"
        );

        return ApiResponse.created(SeriesResponse.from(created));
    }

    @Operation(summary = "Actualizar estado de una serie")
    @PatchMapping("/{id}/status")
    public ApiResponse<SeriesResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {

        return ApiResponse.ok(SeriesResponse.from(
                updateSeriesUseCase.updateStatus(id, SeriesStatus.valueOf(request.getStatus()))
        ));
    }

    @Operation(summary = "Calificar una serie")
    @PatchMapping("/{id}/rating")
    public ApiResponse<SeriesResponse> updateRating(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRatingRequest request) {

        return ApiResponse.ok(SeriesResponse.from(
                updateSeriesUseCase.updateRating(id, request.getRating())
        ));
    }

    @Operation(summary = "Actualizar episodios vistos")
    @PatchMapping("/{id}/episodes")
    public ApiResponse<SeriesResponse> updateEpisodes(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEpisodesRequest request) {

        return ApiResponse.ok(SeriesResponse.from(
                updateSeriesUseCase.updateWatchedEpisodes(id, request.getWatchedEpisodes())
        ));
    }

    @Operation(summary = "Eliminar una serie de la lista")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deleteSeriesUseCase.deleteSeries(id);
        return ApiResponse.noContent();
    }
}