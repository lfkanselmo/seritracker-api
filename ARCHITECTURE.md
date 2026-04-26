# SeriesTracker — Documento de Arquitectura

## Stack Tecnológico

| Capa | Tecnología | Versión |
|------|-----------|---------|
| Frontend | Angular + Angular Material | v21 |
| Backend | Java + Spring Boot | v3.5 |
| Base de datos | PostgreSQL | v16 |
| ORM | Spring Data JPA + Hibernate | — |
| Documentación API | Swagger / OpenAPI 3 | — |
| Autenticación | Spring Security + JWT | — |
| Build tool backend | Maven | v3.9 |

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
    │   │   ├── Series.java
    │   │   ├── User.java
    │   │   └── UserSeries.java
    │   ├── port/
    │   │   ├── in/
    │   │   │   ├── CreateSeriesUseCase.java
    │   │   │   ├── UpdateSeriesUseCase.java
    │   │   │   ├── DeleteSeriesUseCase.java
    │   │   │   ├── SearchSeriesUseCase.java
    │   │   │   └── AuthUseCase.java
    │   │   └── out/
    │   │       ├── SeriesRepository.java
    │   │       ├── UserRepository.java
    │   │       └── TmdbClient.java
    │   └── exception/
    │       ├── SeriesNotFoundException.java
    │       └── DuplicateSeriesException.java
    │
    ├── application/
    │   └── service/
    │       ├── SeriesService.java
    │       └── AuthService.java
    │
    └── infrastructure/
        ├── adapter/
        │   ├── in/
        │   │   └── rest/
        │   │       ├── SeriesController.java
        │   │       ├── AuthController.java
        │   │       └── dto/
        │   │           ├── request/
        │   │           │   ├── CreateSeriesRequest.java
        │   │           │   └── UpdateSeriesRequest.java
        │   │           └── response/
        │   │               ├── SeriesResponse.java
        │   │               ├── SeriesListResponse.java
        │   │               └── ApiResponse.java
        │   └── out/
        │       ├── persistence/
        │       │   ├── SeriesRepositoryAdapter.java
        │       │   ├── UserRepositoryAdapter.java
        │       │   ├── entity/
        │       │   │   ├── SeriesEntity.java
        │       │   │   ├── UserEntity.java
        │       │   │   └── UserSeriesEntity.java
        │       │   └── mapper/
        │       │       ├── SeriesMapper.java
        │       │       └── UserMapper.java
        │       └── tmdb/
        │           ├── TmdbClientAdapter.java
        │           └── dto/
        │               └── TmdbSeriesResponse.java
        └── config/
            ├── SecurityConfig.java
            ├── CorsConfig.java
            └── SwaggerConfig.java
```

---

## Modelo de Base de Datos

```sql
-- Usuarios
users (
  id            BIGSERIAL PRIMARY KEY,
  email         VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  name          VARCHAR(100),
  created_at    TIMESTAMPTZ DEFAULT NOW()
)

-- Cache de series de TMDB
series_cache (
  tmdb_id        INTEGER PRIMARY KEY,
  title          VARCHAR(255) NOT NULL,
  poster_url     TEXT,
  genres         TEXT[],
  network        VARCHAR(100),
  total_episodes INTEGER DEFAULT 0,
  next_air_date  DATE,
  last_synced_at TIMESTAMPTZ DEFAULT NOW()
)

-- Relación usuario <-> serie
user_series (
  id               BIGSERIAL PRIMARY KEY,
  user_id          BIGINT REFERENCES users(id),
  tmdb_id          INTEGER REFERENCES series_cache(tmdb_id),
  status           VARCHAR(20) NOT NULL DEFAULT 'WANT_TO_WATCH',
  rating           INTEGER CHECK (rating BETWEEN 1 AND 10),
  watched_episodes INTEGER DEFAULT 0,
  notes            TEXT,
  created_at       TIMESTAMPTZ DEFAULT NOW(),
  updated_at       TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(user_id, tmdb_id)
)

-- Notificaciones enviadas
notifications (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT REFERENCES users(id),
  tmdb_id      INTEGER REFERENCES series_cache(tmdb_id),
  episode_code VARCHAR(20),
  air_date     DATE,
  sent_at      TIMESTAMPTZ DEFAULT NOW()
)
```

---

## Endpoints REST

```
# Series
GET    /api/v1/series                    ← lista todas las series del usuario
GET    /api/v1/series/{id}               ← detalle de una serie
POST   /api/v1/series                    ← agregar serie a la lista
PATCH  /api/v1/series/{id}/status        ← cambiar estado
PATCH  /api/v1/series/{id}/rating        ← calificar
PATCH  /api/v1/series/{id}/episodes      ← actualizar episodios vistos
DELETE /api/v1/series/{id}               ← eliminar de la lista

# Autenticación
POST   /api/v1/auth/register
POST   /api/v1/auth/login

# TMDB
GET    /api/v1/tmdb/search?q=query       ← buscar series
GET    /api/v1/tmdb/series/{tmdbId}      ← detalle de serie en TMDB
```

---

## Patrones de Diseño

| Patrón | Dónde | Por qué |
|--------|-------|---------|
| Repository | port/out + persistence/ | Abstrae acceso a datos |
| DTO | rest/dto/ | Desacopla API del dominio |
| Mapper | persistence/mapper/ | Convierte entre capas |
| Use Case | port/in/ | Define contratos claros |
| Adapter | adapter/in y adapter/out | Implementa los puertos |
| Builder | Entidades de dominio | Construcción limpia (Lombok) |
| Exception Handler | GlobalExceptionHandler | Centraliza errores |

---

## Principios SOLID

### S — Single Responsibility
Cada clase tiene una sola razón para cambiar.
- `SeriesService` → orquesta el caso de uso
- `TmdbClientAdapter` → habla con TMDB
- `SeriesValidator` → valida reglas de negocio

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

### D — Dependency Inversion
Siempre depender de interfaces (puertos), nunca de implementaciones concretas.

---

## Nomenclatura

### Clases
```
# Dominio
Series.java                   → modelo de dominio
CreateSeriesUseCase.java       → puerto de entrada
SeriesRepository.java          → puerto de salida
TmdbClient.java                → puerto cliente externo

# Aplicación
SeriesService.java             → implementa casos de uso

# Infraestructura
SeriesController.java          → REST controller
SeriesRepositoryAdapter.java   → implementa puerto de BD
SeriesEntity.java              → entidad JPA
SeriesMapper.java              → mapper entre capas

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
users, user_series, series_cache
user_id, tmdb_id, watched_episodes, created_at
```

### Commits
```
feat: add series status update endpoint
fix: correct episode count validation
refactor: extract tmdb mapping to dedicated mapper
docs: add swagger annotations to series controller
test: add unit tests for SeriesService
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

## Estructura de Tests

```
src/test/java/com/seritracker/
├── domain/
│   └── model/
│       └── SeriesTest.java              ← dominio puro
├── application/
│   └── service/
│       └── SeriesServiceTest.java       ← unitarios con mocks
└── infrastructure/
    ├── adapter/in/rest/
    │   └── SeriesControllerTest.java    ← integración
    └── adapter/out/persistence/
        └── SeriesRepositoryTest.java    ← repositorio
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
| H2 | Tests de repositorio sin PostgreSQL |

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
```
