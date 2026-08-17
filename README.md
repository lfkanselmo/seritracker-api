# SeriesTracker API

[![CI](https://github.com/lfkanselmo/seritracker-api/actions/workflows/ci.yml/badge.svg)](https://github.com/lfkanselmo/seritracker-api/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/java-21%20LTS-437291?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)

REST API para el seguimiento de series de televisión. Construida con Java 21 y Spring Boot 3.5 siguiendo arquitectura hexagonal (Ports & Adapters).

---

## Stack Tecnológico

| Capa | Tecnología | Versión |
|------|-----------|---------|
| Lenguaje | Java | 21 LTS |
| Framework | Spring Boot | 3.5 |
| Base de datos | PostgreSQL | 16 |
| ORM | Spring Data JPA + Hibernate | — |
| Migraciones | Flyway | — |
| Autenticación | Spring Security + JWT | — |
| Documentación | Swagger / OpenAPI 3 | — |
| Logging | SLF4J + Logback + MDC | — |
| Build | Maven | 3.9 |
| Tests | JUnit 5 + Mockito + AssertJ + embedded-postgres | — |
| Rate limiting | Filtro propio en memoria (login) | — |

---

## Requisitos Previos

- Java 21 LTS
- Maven 3.9+
- PostgreSQL 16

---

## Configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/lfkanselmo/seritracker-api.git
cd seritracker-api
```

### 2. Crear la base de datos

```sql
CREATE DATABASE seritracker;
```

### 3. Configurar variables de entorno

Crea el archivo `.env.local` en la raíz del proyecto — este archivo **nunca debe subirse a Git** (ya está en `.gitignore`):

```properties
DB_URL=jdbc:postgresql://localhost:5432/seritracker
DB_USERNAME=postgres
DB_PASSWORD=<tu_password_de_postgresql>
TMDB_TOKEN=<tu_token_de_tmdb>
JWT_SECRET=<tu_clave_secreta_generada>
JWT_EXPIRATION=86400000
CORS_ALLOWED_ORIGINS=http://localhost:4200
LOGIN_MAX_ATTEMPTS=5
LOGIN_WINDOW_MINUTES=15
LOG_LEVEL=DEBUG
```

> ⚠️ `JWT_SECRET` no tiene valor por defecto en `application.yaml` — si no está
> seteada, la app **no arranca** (falla rápido en vez de firmar tokens con un
> secreto débil conocido). Si usas IntelliJ, configura las variables en la
> Run Configuration (plugin EnvFile o manualmente) — `.env.local` por sí solo
> no las inyecta si no corrés con un wrapper que lo lea.

#### Cómo obtener el token de TMDB

1. Crea una cuenta gratuita en [themoviedb.org](https://www.themoviedb.org/signup)
2. Ve a **Configuración → API** en tu perfil
3. Solicita una API key — selecciona **Developer** y completa el formulario
4. Copia el **API Read Access Token** (el token largo que empieza con `eyJ...`)

#### Cómo generar el JWT Secret

El secret debe ser una cadena hexadecimal de 256 bits (64 caracteres hex). Puedes generarla con:

```bash
# En Linux/Mac
openssl rand -hex 32

# En PowerShell (Windows)
-join ((1..32) | ForEach-Object { '{0:x2}' -f (Get-Random -Max 256) })
```

> ⚠️ Nunca uses el mismo secret en desarrollo y producción. Nunca lo compartas ni lo subas a Git.

### 4. Ejecutar la aplicación

```bash
mvn spring-boot:run
```

Las migraciones de Flyway se ejecutan automáticamente al arrancar. La API queda disponible en `http://localhost:8080`.

---

## Documentación de la API

Una vez corriendo, accede a Swagger en:

```
http://localhost:8080/swagger-ui/index.html
```

### Endpoints disponibles

```
# Autenticación — públicos, con rate limiting en /login
POST   /api/v1/auth/register                          ← Registrar usuario
POST   /api/v1/auth/login                              ← Iniciar sesión → devuelve JWT

# Series — todos requieren JWT; userId sale del token, nunca de un parámetro
GET    /api/v1/series?status={status}&page={n}&size={n}  ← Listar series del usuario (paginado, filtro opcional por estado)
GET    /api/v1/series/{id}                             ← Detalle de una serie
POST   /api/v1/series                                  ← Agregar serie a la lista
PATCH  /api/v1/series/{id}/status                      ← Cambiar estado
PATCH  /api/v1/series/{id}/rating                      ← Calificar serie
PATCH  /api/v1/series/{id}/episodes                    ← Actualizar episodios vistos
DELETE /api/v1/series/{id}                             ← Eliminar de la lista

# TMDB — públicos
GET    /api/v1/tmdb/search?q=query                     ← Buscar series en TMDB
GET    /api/v1/tmdb/series/{tmdbId}                    ← Detalle de serie en TMDB

# Notificaciones — todos requieren JWT
GET    /api/v1/notifications?page={n}&size={n}         ← Notificaciones no leídas (paginado)
PATCH  /api/v1/notifications/{id}/read                 ← Marcar como leída
POST   /api/v1/notifications/check                     ← Disparar verificación manual
```

`page` es 0-indexed. `size` por defecto es 20 en series y 50 en notificaciones, con un máximo de 100 en ambos.

### Formato de respuesta estándar

```json
{
  "success": true,
  "data": {},
  "message": "OK",
  "timestamp": "2026-04-26T10:00:00"
}
```

Los endpoints paginados (`/series`, `/notifications`) envuelven una página
adicional dentro de `data`:

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1
  },
  "message": "OK",
  "timestamp": "2026-04-26T10:00:00"
}
```

### Autenticación

Todos los endpoints de `/api/v1/series` y `/api/v1/notifications` requieren
JWT. El `userId` se extrae del claim del token — nunca de un `@RequestParam`
ni de la URL, así que no hay forma de pedir datos de otro usuario cambiando
un parámetro. Incluye el token en el header:

```
Authorization: Bearer <token>
```

`POST /api/v1/auth/login` tiene rate limiting propio (`login.rate-limit.*`,
ver Variables de Entorno): tras `LOGIN_MAX_ATTEMPTS` intentos fallidos desde
la misma clave en `LOGIN_WINDOW_MINUTES`, responde `429` hasta que expire la
ventana.

---

## Arquitectura

El proyecto sigue **arquitectura hexagonal (Ports & Adapters)** con tres capas:

```
domain/        → modelos y puertos — cero dependencias externas
application/   → casos de uso — orquesta el dominio
infrastructure → adaptadores — Spring, JPA, REST, TMDB
```

### Regla de oro
Un `import` de `infrastructure` dentro de `domain` es una violación de arquitectura.

### Estructura de carpetas

```
src/main/java/com/seritracker/
├── domain/
│   ├── model/          ← User, UserSeries, SeriesStatus, Notification, PageRequest, PageResult
│   ├── port/
│   │   ├── in/         ← Casos de uso (interfaces)
│   │   └── out/        ← Puertos de salida (interfaces)
│   └── exception/      ← Excepciones de dominio
├── application/
│   └── service/        ← AuthService, SeriesService, NotificationService, EpisodeCheckService
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/    ← Controllers + DTOs (request/response, incluye PageResponse)
    │   └── out/
    │       ├── persistence/  ← Repositorios JPA (User, UserSeries, Notification)
    │       └── tmdb/         ← Cliente TMDB (RestClient)
    ├── config/         ← Security, CORS, Swagger
    ├── security/       ← JWT (Service/Filter), UserDetails, rate limiting de login
    └── logging/        ← MDC Filter
```

`NotificationService` y `EpisodeCheckService` están separados a propósito:
el primero orquesta CRUD de notificaciones (listar, marcar como leída), el
segundo solo decide *qué* notificar (revisar próximos episodios) — cada uno
con una sola razón para cambiar.

---

## Base de Datos

Las migraciones se gestionan con **Flyway** y se ejecutan automáticamente:

| Versión | Descripción |
|---------|-------------|
| V1 | Tablas `users` y `user_series` |
| V2 | Campo `role` en `users` + usuario inicial |
| V3 | Tabla `notifications` |
| V4 | Elimina el usuario admin sembrado en V2 (no se debe sembrar un usuario con credenciales conocidas) |
| V5 | Columna `version` en `user_series` para locking optimista (`@Version` de JPA) |

### Estados de series

`SeriesStatus` es un enum de dominio (no un `String` suelto) — el (de)serializador
de Jackson rechaza cualquier valor que no sea uno de estos cuatro.

| Estado | Descripción |
|--------|-------------|
| `WATCHING` | Viendo actualmente |
| `WANT_TO_WATCH` | En lista de pendientes |
| `COMPLETED` | Terminada |
| `ABANDONED` | Abandonada |

### Concurrencia — locking optimista

`user_series` tiene una columna `version` (`@Version` de JPA). Dos updates
concurrentes sobre la misma fila (p. ej. marcar episodios vistos desde dos
pestañas) hacen que el segundo falle con `OptimisticLockException` en vez de
pisar silenciosamente el cambio del primero. Cubierto por
`UserSeriesOptimisticLockingTest`.

---

## Notificaciones

El sistema revisa automáticamente **todos los días a las 8am** los próximos episodios de las series con estado `WATCHING`. Si un episodio emite hoy o mañana, genera una notificación para el usuario.

Para disparar la verificación manualmente (requiere JWT — el endpoint ya no es público):

```bash
curl -X POST http://localhost:8080/api/v1/notifications/check \
  -H "Authorization: Bearer <token>"
```

---

## Logging

Los logs incluyen un `requestId` único por request para trazabilidad completa:

```
10:00:01 INFO  [req-abc123] SeriesService - Creating series tmdbId=1396 for userId=2
10:00:01 DEBUG [req-abc123] TmdbClientAdapter - Fetching details for tmdbId=1396
10:00:01 INFO  [req-abc123] SeriesService - Series id=5 created successfully
```

**Perfiles:**
- `dev` → consola con colores, nivel DEBUG
- `prod` → archivo rotativo diario, nivel INFO

---

## Tests

No hay Docker/Testcontainers en todos los entornos de desarrollo de este
proyecto, así que las pruebas que necesitan Postgres real usan
[`embedded-postgres`](https://github.com/zonkyio/embedded-postgres) (arranca
un Postgres real embebido, sin contenedor). Los tests de repositorio más
livianos siguen usando H2.

```bash
# Ejecutar todos los tests
mvn test

# Ejecutar con reporte de cobertura
mvn test jacoco:report
# Reporte en: target/site/jacoco/index.html
```

### Cobertura actual

| Métrica | Resultado |
|---------|-----------|
| Instrucciones | 73% |
| Branches | 72% |
| Líneas | 75% |
| Métodos | 74% |
| Tests totales | 105 |

### Estructura de tests

```
src/test/java/com/seritracker/
├── application/service/
│   ├── SeriesServiceTest.java
│   └── AuthServiceTest.java
├── domain/exception/
│   └── ExceptionTest.java
├── integration/
│   └── PostgresIntegrationTest.java        ← Flyway + JPA contra Postgres real (embedded-postgres)
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── SeriesControllerTest.java
    │   ├── AuthControllerTest.java
    │   ├── TmdbControllerTest.java
    │   └── dto/response/ApiResponseTest.java
    ├── adapter/out/persistence/
    │   ├── UserSeriesRepositoryAdapterTest.java
    │   ├── UserSeriesOptimisticLockingTest.java  ← locking optimista contra Postgres real
    │   ├── entity/UserEntityTest.java
    │   ├── entity/UserSeriesEntityTest.java
    │   └── mapper/UserSeriesMapperTest.java
    ├── adapter/out/tmdb/
    │   └── TmdbClientAdapterTest.java            ← MockWebServer, sin llamadas reales a TMDB
    ├── security/
    │   ├── JwtServiceTest.java
    │   ├── JwtAuthFilterTest.java
    │   ├── LoginRateLimiterTest.java
    │   └── LoginRateLimitFilterTest.java
    └── logging/MdcFilterTest.java
```

---

## Build de Producción

```bash
mvn clean package -DskipTests
```

El JAR se genera en `target/seritracker-api-0.0.1-SNAPSHOT.jar`.

Para ejecutar en producción:

```bash
java -jar target/seritracker-api-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --DB_URL=jdbc:postgresql://<host>:5432/seritracker \
  --DB_USERNAME=<usuario> \
  --DB_PASSWORD=<password> \
  --TMDB_TOKEN=<tu_token_tmdb> \
  --JWT_SECRET=<tu_secret_generado>
```

> ⚠️ Nunca incluyas valores reales de credenciales en comandos que vayas a documentar o compartir.

---

## Variables de Entorno

| Variable | Descripción | Default |
|----------|-------------|---------|
| `DB_URL` | URL de conexión a PostgreSQL | `jdbc:postgresql://localhost:5432/seritracker` |
| `DB_USERNAME` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `0000` (solo para dev local; sobrescribir siempre) |
| `TMDB_TOKEN` | Read Access Token de TMDB (ver instrucciones arriba) | vacío |
| `JWT_SECRET` | Clave hex de 256 bits para firmar JWT (ver instrucciones arriba) | **ninguno — la app no arranca sin esta variable** |
| `JWT_EXPIRATION` | Duración del JWT en milisegundos | `86400000` (24h) |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos por CORS | `http://localhost:4200` |
| `LOGIN_MAX_ATTEMPTS` | Intentos fallidos de login antes de bloquear la IP | `5` |
| `LOGIN_WINDOW_MINUTES` | Ventana de tiempo del rate limit de login | `15` |
| `PORT` | Puerto del servidor | `8080` |
| `LOG_LEVEL` | Nivel de log para `com.seritracker` | `INFO` |

> ⚠️ `DB_PASSWORD` y `TMDB_TOKEN` tienen defaults neutros solo para que el
> proyecto arranque en un entorno de desarrollo sin configurar nada; nunca
> deben quedar así en producción. `JWT_SECRET` deliberadamente **no** tiene
> default — si faltara, firmar tokens con un secreto conocido sería un
> agujero de seguridad silencioso, así que la app falla al arrancar en vez
> de arrancar insegura.

---

## Proyectos Relacionados

- **Frontend:** [seritracker-web](https://github.com/lfkanselmo/seritracker-web) — Angular 21
