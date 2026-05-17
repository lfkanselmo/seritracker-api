# SeriesTracker API

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
| Tests | JUnit 5 + Mockito + AssertJ | — |

---

## Requisitos Previos

- Java 21 LTS
- Maven 3.9+
- PostgreSQL 16

---

## Configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/TuUsuario/seritracker-api.git
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
LOG_LEVEL=DEBUG
```

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
# Autenticación
POST   /api/v1/auth/register             ← Registrar usuario
POST   /api/v1/auth/login                ← Iniciar sesión → devuelve JWT

# Series
GET    /api/v1/series?userId={id}        ← Listar series del usuario
GET    /api/v1/series/{id}               ← Detalle de una serie
POST   /api/v1/series?userId={id}        ← Agregar serie a la lista
PATCH  /api/v1/series/{id}/status        ← Cambiar estado
PATCH  /api/v1/series/{id}/rating        ← Calificar serie
PATCH  /api/v1/series/{id}/episodes      ← Actualizar episodios vistos
DELETE /api/v1/series/{id}               ← Eliminar de la lista

# TMDB
GET    /api/v1/tmdb/search?q=query       ← Buscar series en TMDB
GET    /api/v1/tmdb/series/{tmdbId}      ← Detalle de serie en TMDB

# Notificaciones
GET    /api/v1/notifications?userId={id} ← Notificaciones no leídas
PATCH  /api/v1/notifications/{id}/read   ← Marcar como leída
POST   /api/v1/notifications/check       ← Disparar verificación manual
```

### Formato de respuesta estándar

```json
{
  "success": true,
  "data": {},
  "message": "OK",
  "timestamp": "2026-04-26T10:00:00"
}
```

### Autenticación

Todos los endpoints de `/api/v1/series` requieren JWT. Incluye el token en el header:

```
Authorization: Bearer <token>
```

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
│   ├── model/          ← Series, User, UserSeries, Notification
│   ├── port/
│   │   ├── in/         ← Casos de uso (interfaces)
│   │   └── out/        ← Puertos de salida (interfaces)
│   └── exception/      ← Excepciones de dominio
├── application/
│   └── service/        ← Implementación de casos de uso
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/    ← Controllers + DTOs
    │   └── out/
    │       ├── persistence/  ← Repositorios JPA
    │       └── tmdb/         ← Cliente TMDB
    ├── config/         ← Security, CORS, Swagger
    ├── security/       ← JWT Filter, UserDetails
    └── logging/        ← MDC Filter
```

---

## Base de Datos

Las migraciones se gestionan con **Flyway** y se ejecutan automáticamente:

| Versión | Descripción |
|---------|-------------|
| V1 | Tablas `users` y `user_series` |
| V2 | Campo `role` en `users` + usuario inicial |
| V3 | Tabla `notifications` |

### Estados de series

| Estado | Descripción |
|--------|-------------|
| `WATCHING` | Viendo actualmente |
| `WANT_TO_WATCH` | En lista de pendientes |
| `COMPLETED` | Terminada |
| `ABANDONED` | Abandonada |

---

## Notificaciones

El sistema revisa automáticamente **todos los días a las 8am** los próximos episodios de las series con estado `WATCHING`. Si un episodio emite hoy o mañana, genera una notificación para el usuario.

Para disparar la verificación manualmente:

```bash
curl -X POST http://localhost:8080/api/v1/notifications/check
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
| Instrucciones | 95% |
| Branches | 82% |
| Tests totales | 82 |

### Estructura de tests

```
src/test/java/com/seritracker/
├── application/service/
│   ├── SeriesServiceTest.java      ← 14 tests
│   └── AuthServiceTest.java        ← 6 tests
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── SeriesControllerTest.java   ← 12 tests
    │   ├── AuthControllerTest.java     ← 8 tests
    │   └── TmdbControllerTest.java     ← 4 tests
    ├── adapter/out/persistence/
    │   ├── UserSeriesRepositoryAdapterTest.java
    │   └── mapper/UserSeriesMapperTest.java
    ├── security/
    │   ├── JwtServiceTest.java
    │   └── JwtAuthFilterTest.java
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
| `DB_PASSWORD` | Contraseña de PostgreSQL | — |
| `TMDB_TOKEN` | Read Access Token de TMDB (ver instrucciones arriba) | — |
| `JWT_SECRET` | Clave hex de 256 bits para firmar JWT (ver instrucciones arriba) | — |
| `JWT_EXPIRATION` | Duración del JWT en milisegundos | `86400000` (24h) |
| `PORT` | Puerto del servidor | `8080` |
| `LOG_LEVEL` | Nivel de log para `com.seritracker` | `INFO` |

> ⚠️ Las variables `DB_PASSWORD`, `TMDB_TOKEN` y `JWT_SECRET` son obligatorias y nunca deben tener valores por defecto en producción.

---

## Proyectos Relacionados

- **Frontend:** [seritracker-web](https://github.com/TuUsuario/seritracker-web) — Angular 21
