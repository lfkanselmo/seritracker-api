# SeriesTracker — Documento de Arquitectura

## Stack Tecnológico

| Capa | Tecnología | Versión |
|------|-----------|---------|
| Frontend | Angular (zoneless) + Angular Material | v21 |
| Backend | Java + Spring Boot | v3.5 |
| Base de datos | PostgreSQL | v16 |
| ORM | Spring Data JPA + Hibernate | — |
| Documentación API | Swagger / OpenAPI 3 | — |
| Autenticación | Spring Security + JWT (claims: `userId`, `role`) | — |
| Rate limiting | Filtro propio en memoria (`POST /auth/login`) | — |
| Cliente HTTP saliente | `RestClient` (Spring 6) — TMDB | — |
| Logging | SLF4J + Logback | — |
| Build tool backend | Maven | v3.9 |
| Tests backend | JUnit 5 + Mockito + AssertJ + embedded-postgres | — |

La identidad visual del frontend ("Ambilight") vive en
[`seritracker-web/DESIGN_SYSTEM.md`](../seritracker-web/DESIGN_SYSTEM.md) —
este documento cubre solo decisiones de arquitectura de software, no de UI.

---

## Arquitectura: Hexagonal (Ports & Adapters)

### Regla de Oro
```
domain/        → no conoce a nadie
application/   → conoce solo a domain
infrastructure → conoce a application y domain
```
Si en algún momento ves un `import` de `infrastructure` dentro de `domain`, es una violación de arquitectura.

---

## Estructura de Carpetas — Backend

```
seritracker-api/
└── src/main/java/com/seritracker/
    │
    ├── domain/
    │   ├── model/
    │   │   ├── Series.java           ← resultado de búsqueda en TMDB (transitorio, no se persiste)
    │   │   ├── SeriesStatus.java     ← enum: WATCHING, WANT_TO_WATCH, COMPLETED, ABANDONED
    │   │   ├── User.java
    │   │   ├── UserSeries.java       ← la serie DENTRO de la lista de un usuario (sí se persiste)
    │   │   ├── Notification.java
    │   │   ├── PageRequest.java      ← page/size, agnóstico de Spring Data
    │   │   └── PageResult.java       ← content/page/size/totalElements/totalPages
    │   ├── port/
    │   │   ├── in/
    │   │   │   ├── CreateSeriesUseCase.java
    │   │   │   ├── UpdateSeriesUseCase.java
    │   │   │   ├── DeleteSeriesUseCase.java
    │   │   │   ├── SearchSeriesUseCase.java
    │   │   │   ├── NotificationUseCase.java
    │   │   │   └── CheckUpcomingEpisodesUseCase.java
    │   │   └── out/
    │   │       ├── UserRepository.java
    │   │       ├── UserSeriesRepository.java
    │   │       ├── NotificationRepository.java
    │   │       └── TmdbClient.java
    │   └── exception/
    │       ├── SeriesNotFoundException.java
    │       ├── DuplicateSeriesException.java
    │       └── NotificationNotFoundException.java
    │
    ├── application/
    │   └── service/
    │       ├── SeriesService.java
    │       ├── AuthService.java             ← depende del puerto UserRepository, no de JPA
    │       ├── NotificationService.java     ← CRUD de notificaciones (listar, marcar leída)
    │       └── EpisodeCheckService.java     ← decide qué notificar (próximos episodios)
    │
    └── infrastructure/
        ├── adapter/
        │   ├── in/
        │   │   └── rest/
        │   │       ├── SeriesController.java
        │   │       ├── AuthController.java
        │   │       ├── NotificationController.java
        │   │       ├── TmdbController.java
        │   │       └── dto/
        │   │           ├── request/
        │   │           │   ├── CreateSeriesRequest.java
        │   │           │   ├── UpdateStatusRequest.java
        │   │           │   ├── UpdateRatingRequest.java
        │   │           │   ├── UpdateEpisodesRequest.java
        │   │           │   ├── LoginRequest.java
        │   │           │   └── RegisterRequest.java
        │   │           └── response/
        │   │               ├── SeriesResponse.java
        │   │               ├── NotificationResponse.java
        │   │               ├── AuthResponse.java
        │   │               ├── PageResponse.java   ← envoltorio genérico de paginación
        │   │               └── ApiResponse.java
        │   └── out/
        │       ├── persistence/
        │       │   ├── UserRepositoryAdapter.java
        │       │   ├── UserSeriesRepositoryAdapter.java
        │       │   ├── NotificationRepositoryAdapter.java
        │       │   ├── entity/
        │       │   │   ├── UserEntity.java
        │       │   │   ├── UserSeriesEntity.java   ← tiene @Version (locking optimista)
        │       │   │   └── NotificationEntity.java
        │       │   └── mapper/
        │       │       ├── UserMapper.java
        │       │       ├── UserSeriesMapper.java
        │       │       └── NotificationMapper.java
        │       └── tmdb/
        │           ├── TmdbClientAdapter.java       ← usa RestClient, no RestTemplate
        │           └── dto/
        │               ├── TmdbSearchResponse.java
        │               └── TmdbSeriesResponse.java
        ├── config/
        │   ├── SecurityConfig.java    ← CORS, CSRF off, stateless, permitAll de auth/tmdb/swagger
        │   ├── SwaggerConfig.java
        │   └── GlobalExceptionHandler.java
        ├── security/
        │   ├── JwtService.java              ← genera/valida token, embebe userId + role como claims
        │   ├── JwtAuthFilter.java
        │   ├── UserDetailsServiceImpl.java
        │   ├── UserPrincipal.java           ← lo que llega a los controllers vía @AuthenticationPrincipal
        │   ├── LoginRateLimiter.java         ← contador en memoria por IP
        │   └── LoginRateLimitFilter.java     ← responde 429 antes de llegar al controller
        └── logging/
            └── MdcFilter.java             ← Inyecta requestId en cada request
```

---

## Modelo de Base de Datos

No existe una tabla `series_cache` separada — se evaluó y se descartó: los
metadatos de TMDB (`title`, `poster_url`, `network`, `total_episodes`) se
guardan **desnormalizados directamente en `user_series`**, tomados en el
momento de agregar la serie. Es más simple para el tamaño actual del
proyecto y evita tener que sincronizar una tabla de caché; el costo es que
si una serie cambia de título en TMDB, no se refleja retroactivamente en
las filas ya guardadas.

```sql
-- Usuarios
users (
  id            BIGSERIAL PRIMARY KEY,
  email         VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  name          VARCHAR(100),
  role          VARCHAR(20) NOT NULL DEFAULT 'USER',
  created_at    TIMESTAMPTZ DEFAULT NOW()
)

-- Serie dentro de la lista de un usuario — metadatos de TMDB desnormalizados
user_series (
  id               BIGSERIAL PRIMARY KEY,
  user_id          BIGINT REFERENCES users(id) ON DELETE CASCADE,
  tmdb_id          INTEGER NOT NULL,
  title            VARCHAR(255) NOT NULL,
  poster_url       TEXT,
  status           VARCHAR(20) NOT NULL DEFAULT 'WANT_TO_WATCH',
  rating           INTEGER CHECK (rating BETWEEN 1 AND 10),
  watched_episodes INTEGER NOT NULL DEFAULT 0,
  total_episodes   INTEGER NOT NULL DEFAULT 0,
  network          VARCHAR(100),
  notes            TEXT,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  version          BIGINT NOT NULL DEFAULT 0,   -- @Version JPA, locking optimista
  UNIQUE(user_id, tmdb_id)
)

-- Notificaciones enviadas
notifications (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT REFERENCES users(id) ON DELETE CASCADE,
  tmdb_id      INTEGER NOT NULL,
  series_title VARCHAR(255) NOT NULL,
  episode_code VARCHAR(20),
  air_date     DATE NOT NULL,
  sent_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  read         BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE(user_id, tmdb_id, episode_code)
)
```

Ver `src/main/resources/db/migration/` (V1–V5) para el historial exacto —
incluye la reversión de un usuario admin sembrado con credenciales
conocidas (V4) y la adición de `version` para locking optimista (V5).

---

## Endpoints REST

```
# Series — requieren JWT; userId sale de @AuthenticationPrincipal, nunca de un parámetro
GET    /api/v1/series?status=&page=&size=  ← lista paginada, filtro opcional por estado
GET    /api/v1/series/{id}                 ← detalle de una serie
POST   /api/v1/series                      ← agregar serie a la lista
PATCH  /api/v1/series/{id}/status          ← cambiar estado
PATCH  /api/v1/series/{id}/rating          ← calificar
PATCH  /api/v1/series/{id}/episodes        ← actualizar episodios vistos
DELETE /api/v1/series/{id}                 ← eliminar de la lista

# Notificaciones — requieren JWT
GET    /api/v1/notifications?page=&size=   ← no leídas, paginado
PATCH  /api/v1/notifications/{id}/read     ← marcar como leída
POST   /api/v1/notifications/check         ← disparar verificación manual

# Autenticación — públicos; /login con rate limiting propio
POST   /api/v1/auth/register
POST   /api/v1/auth/login

# TMDB — públicos
GET    /api/v1/tmdb/search?q=query       ← buscar series
GET    /api/v1/tmdb/series/{tmdbId}      ← detalle de serie en TMDB
```

Ver `SecurityConfig.filterChain()` para la lista exacta de rutas públicas
(`permitAll`) — todo lo que no está ahí requiere `Authorization: Bearer <token>`.

---

## Patrones de Diseño

| Patrón | Dónde | Por qué |
|--------|-------|---------|
| Repository | port/out (`UserRepository`, `UserSeriesRepository`, `NotificationRepository`) + persistence/ | Abstrae acceso a datos |
| DTO | rest/dto/ | Desacopla API del dominio |
| Mapper | persistence/mapper/ | Convierte entre capas |
| Use Case | port/in/ | Define contratos claros |
| Adapter | adapter/in y adapter/out | Implementa los puertos |
| Builder | Entidades y modelos de dominio | Construcción limpia (Lombok) |
| Exception Handler | GlobalExceptionHandler | Centraliza errores |
| Filter Chain | `JwtAuthFilter`, `LoginRateLimitFilter` | Autenticación y rate limiting antes del controller |
| Value Object | `PageRequest`, `PageResult` (dominio, agnósticos de Spring Data) | Paginación sin acoplar el dominio a JPA |

---

## Principios SOLID

### S — Single Responsibility
Cada clase tiene una sola razón para cambiar.
- `SeriesService` → orquesta el caso de uso
- `TmdbClientAdapter` → habla con TMDB
- `NotificationService` → CRUD de notificaciones (listar, marcar leída)
- `EpisodeCheckService` → decide qué notificar (próximos episodios) — separado de `NotificationService` a propósito, son dos razones de cambio distintas
- `LoginRateLimiter` → solo cuenta intentos; `LoginRateLimitFilter` → solo decide bloquear la request

### O — Open/Closed
Los puertos están cerrados para modificación, los adaptadores abiertos para extensión.

### L — Liskov Substitution
Cualquier implementación de un puerto puede reemplazar a otra sin romper el sistema.

### I — Interface Segregation
Interfaces pequeñas y enfocadas:
- `CreateSeriesUseCase`
- `UpdateSeriesUseCase`
- `DeleteSeriesUseCase`
- `SearchSeriesUseCase`
- `NotificationUseCase`
- `CheckUpcomingEpisodesUseCase`

`AuthService` es la excepción deliberada — no tiene puerto `in` propio
porque solo lo consume `AuthController` y no hay una segunda implementación
previsible; se evitó la interfaz "por las dudas".

### D — Dependency Inversion
Siempre depender de interfaces (puertos), nunca de implementaciones concretas.

---

## Nomenclatura

### Clases
```
# Dominio
UserSeries.java                → modelo de dominio persistido
CreateSeriesUseCase.java       → puerto de entrada
UserSeriesRepository.java      → puerto de salida
TmdbClient.java                → puerto cliente externo

# Aplicación
SeriesService.java             → implementa casos de uso

# Infraestructura
SeriesController.java              → REST controller
UserSeriesRepositoryAdapter.java   → implementa puerto de BD
UserSeriesEntity.java              → entidad JPA
UserSeriesMapper.java              → mapper entre capas

# DTOs
CreateSeriesRequest.java       → [Accion][Entidad]Request
SeriesResponse.java            → [Entidad]Response
```

### Métodos
```java
// Consultas
getSeriesById(Long id)
findByStatus(SeriesStatus status)
listAllByUser(Long userId)
existsByTmdbId(Integer tmdbId)

// Comandos
createSeries(CreateSeriesRequest request)
updateWatchedEpisodes(Long id, Integer episodes)
deleteSeriesFromList(Long userId, Long seriesId)

// Booleanos
isSeriesInUserList(Long userId, Integer tmdbId)
hasNextEpisode(Long seriesId)
```

### Base de Datos
```
snake_case siempre
users, user_series, notifications
user_id, tmdb_id, watched_episodes, created_at, version
```

### Commits
```
feat: add series status update endpoint
fix: correct episode count validation
refactor: extract tmdb mapping to dedicated mapper
docs: add swagger annotations to series controller
test: add unit tests for SeriesService
logs: add request tracing to SeriesService
```

---

## Reglas de Limpieza de Código

- Máximo **20 líneas** por método
- Máximo **3 niveles** de indentación
- Máximo **200 líneas** por clase
- Sin lógica de negocio en Controllers ni en Entities
- Sin comentarios que expliquen *qué* — el código se explica solo
- Comentarios solo para explicar *por qué* de decisiones no obvias

---

## Respuesta Estándar de la API

```json
{
  "success": true,
  "data": {},
  "message": "OK",
  "timestamp": "2026-04-26T10:00:00"
}
```

---

## Estrategia de Logging

### Niveles por capa

| Capa | Niveles permitidos | Cuándo usarlos |
|------|--------------------|----------------|
| `domain/` | ❌ Sin logs | El dominio no conoce infraestructura |
| `application/service/` | `INFO`, `WARN`, `ERROR` | Flujos de negocio, errores de dominio |
| `infrastructure/adapter/in/rest/` | `DEBUG` | Entrada de requests, parámetros |
| `infrastructure/adapter/out/persistence/` | `DEBUG` | Queries, operaciones BD |
| `infrastructure/adapter/out/tmdb/` | `INFO`, `WARN`, `ERROR` | Llamadas externas, fallos de API |
| `infrastructure/config/` | `INFO` | Arranque, configuración |
| `infrastructure/security/` | `WARN`, `ERROR` | Intentos fallidos, tokens inválidos |

### Qué se loguea

```
✅ Inicio y fin de casos de uso con userId
✅ Creación, actualización y eliminación de recursos
✅ Llamadas a APIs externas (TMDB) con tmdbId
✅ Errores de negocio — serie no encontrada, duplicados
✅ Intentos de autenticación fallidos
✅ Tiempo de respuesta de llamadas externas
✅ Arranque de la aplicación
```

### Qué NUNCA se loguea

```
❌ Passwords en ningún formato
❌ Tokens JWT completos — solo los primeros 10 caracteres si es necesario
❌ Datos personales — email solo en DEBUG, nunca en INFO/WARN/ERROR
❌ Stack traces completos en INFO/WARN — solo en ERROR
❌ Respuestas completas de APIs externas
```

### Formato de mensajes

```java
// ✅ BIEN — acción + contexto + identificador
log.info("Creating series tmdbId={} for userId={}", tmdbId, userId);
log.info("Series id={} status updated to {}", id, status);
log.warn("Series tmdbId={} not found in TMDB API", tmdbId);
log.error("Failed to delete series id={}", id, exception);

// ❌ MAL — vago, sin contexto
log.info("Creating series");
log.info("Done");
log.error("Error: " + exception.getMessage()); // concatenación en lugar de parámetros
```

### MDC — Trazabilidad por request

Cada request HTTP recibe un `requestId` único generado en `MdcFilter`.
Este ID aparece en todos los logs del mismo request, permitiendo trazar el flujo completo:

```
2026-04-29 10:00:01 INFO  [req-abc123] SeriesService       - Creating series tmdbId=1396 for userId=2
2026-04-29 10:00:01 DEBUG [req-abc123] TmdbClientAdapter   - Fetching details for tmdbId=1396
2026-04-29 10:00:01 INFO  [req-abc123] SeriesService       - Series id=5 created successfully for userId=2
```

El `MdcFilter` vive en `infrastructure/logging/` y se registra como `@Component`.

### Configuración por perfil

```
dev  → consola con colores, nivel DEBUG para com.seritracker
prod → archivo rotativo diario, nivel INFO, formato JSON
```

Configurado en `src/main/resources/logback-spring.xml`.

### Tipo de log por situación

| Situación | Nivel |
|-----------|-------|
| Operación completada exitosamente | `INFO` |
| Recurso no encontrado (esperado) | `WARN` |
| Serie duplicada (esperado) | `WARN` |
| Fallo en llamada a TMDB API | `ERROR` |
| Token JWT inválido | `WARN` |
| Error inesperado del sistema | `ERROR` |
| Arranque/parada de la app | `INFO` |
| Detalles de request entrante | `DEBUG` |

---

## Estructura de Tests

105 tests en total. Ver el desglose completo en
[`seritracker-api/README.md`](./README.md#estructura-de-tests) — acá solo
el resumen por capa:

```
src/test/java/com/seritracker/
├── application/service/     ← unitarios con mocks (SeriesService, AuthService)
├── domain/exception/        ← dominio puro
├── integration/
│   └── PostgresIntegrationTest.java   ← Flyway + JPA contra Postgres real
└── infrastructure/
    ├── adapter/in/rest/      ← MockMvc — controllers, DTOs, paginación
    ├── adapter/out/persistence/
    │   ├── UserSeriesRepositoryAdapterTest.java   ← H2
    │   └── UserSeriesOptimisticLockingTest.java   ← Postgres real, concurrencia
    ├── adapter/out/tmdb/     ← MockWebServer, sin llamadas reales a TMDB
    ├── security/             ← JWT, filtros, rate limiting
    └── logging/
```

### Patrón AAA
```java
@Test
void should[Resultado]_when[Condicion]() {
    // Arrange
    // Act
    // Assert
}
```

### Herramientas
| Herramienta | Para qué |
|-------------|----------|
| JUnit 5 | Framework base |
| Mockito | Mocking de dependencias |
| AssertJ | Assertions fluidas |
| H2 | Tests de repositorio livianos, sin PostgreSQL |
| [`embedded-postgres`](https://github.com/zonkyio/embedded-postgres) | Postgres real embebido para Flyway y locking optimista — no requiere Docker |
| MockWebServer (OkHttp) | Simula la API de TMDB sin red real |

---

## Checklist antes de cada commit

```
✅ ¿La clase tiene una sola responsabilidad?
✅ ¿Los métodos tienen menos de 20 líneas?
✅ ¿Se depende de interfaces, no de implementaciones?
✅ ¿Los DTOs nunca llegan al dominio?
✅ ¿Las entidades JPA nunca salen de persistence/?
✅ ¿Hay al menos un test por cada método de servicio?
✅ ¿El nombre del método describe exactamente lo que hace?
✅ ¿El commit sigue el formato feat/fix/refactor?
✅ ¿Los logs no contienen passwords, tokens ni datos personales?
✅ ¿Se usó el nivel de log correcto según la tabla de niveles?
✅ ¿Los mensajes de log incluyen contexto (userId, id, tmdbId)?
✅ ¿El userId sale de @AuthenticationPrincipal, nunca de @RequestParam o la URL?
✅ ¿Un secreto o credencial nuevo tiene default inseguro en application.yaml? (no debería)
✅ ¿Un endpoint nuevo que devuelve listas está paginado?
```
