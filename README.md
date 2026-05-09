# Spring Boot Full-Stack Template

Plantilla full-stack lista para usar: Spring Boot 3 · Spring Security 6 · JWT · JPA · Flyway · WebSocket (STOMP) · Thymeleaf · Bootstrap 5 · Roles/Permisos.

---

## Tabla de contenidos

1. [Stack tecnológico](#stack-tecnológico)
2. [Arranque rápido](#arranque-rápido)
3. [Credenciales por defecto](#credenciales-por-defecto)
4. [Perfiles de configuración](#perfiles-de-configuración)
5. [Estructura de paquetes](#estructura-de-paquetes)
6. [Modelo de datos (ER)](#modelo-de-datos-er)
7. [Endpoints](#endpoints)
8. [WebSocket](#websocket)
9. [Roles y permisos](#roles-y-permisos)
10. [Variables de entorno](#variables-de-entorno)
11. [Docker](#docker)
12. [DevContainer](#devcontainer)
13. [Tests](#tests)
14. [Migraciones Flyway](#migraciones-flyway)

---

## Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Java 21 |
| Framework principal | Spring Boot 3.3.5 |
| Seguridad | Spring Security 6 + JWT (jjwt 0.12.6) |
| Persistencia | Spring Data JPA + Hibernate |
| Migraciones | Flyway |
| BD producción | MySQL 8 |
| BD desarrollo | H2 en memoria (modo MySQL) |
| Plantillas | Thymeleaf + thymeleaf-extras-springsecurity6 |
| Frontend | Bootstrap 5 (CDN) |
| Tiempo real | WebSocket STOMP |
| Utilidades | Lombok · MapStruct · Spring Cache |
| Monitorización | Spring Boot Actuator |
| Build | Maven 3 |
| Contenedores | Docker + Docker Compose |
| Entorno de desarrollo | DevContainer (VS Code / Cursor) |

---

## Arranque rápido

### Opción A — Con Docker (recomendada)

Requiere tener [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y en ejecución.

```bash
# Clonar
git clone <repo-url>
cd PBL4---WEB

# Copiar variables de entorno y ajustar JWT_SECRET
cp .env.example .env

# Arrancar app + MySQL juntos
docker compose up --build
```

La aplicación arranca en **http://localhost:8080**. La primera vez tarda unos minutos (descarga imágenes y compila el proyecto).

Para detener: `Ctrl+C` y luego `docker compose down`.

---

### Opción B — Sin Docker, directo con Maven

Requisitos previos:

- Java 21+
- Maven 3.9+ (o usar el wrapper `./mvnw`)

### Perfil `dev` (H2 en memoria, sin MySQL)

```bash
# Clonar
git clone <repo-url>
cd PBL4---WEB

# Arrancar con perfil dev (base de datos H2, no requiere MySQL)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

La aplicación arranca en **http://localhost:8080**.

### Perfil `prod` (MySQL real)

1. Crea la base de datos:

```sql
CREATE DATABASE template_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Configura las variables de entorno (ver sección [Variables de entorno](#variables-de-entorno)).

3. Arranca:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### Build de producción (JAR)

```bash
./mvnw clean package -DskipTests
java -jar target/springboot-fullstack-template-1.0.0.jar --spring.profiles.active=prod
```

---

## Credenciales por defecto

El `DataInitializer` crea el usuario administrador en el primer arranque:

| Campo | Valor |
|-------|-------|
| **Username** | `admin` |
| **Password** | `admin123` |
| **Email** | `admin@template.local` |
| **Roles** | `ROLE_ADMIN` + `ROLE_USER` |

> Cámbialo inmediatamente en producción.

### Consola H2 (solo perfil `dev`)

URL: **http://localhost:8080/h2-console**

| Parámetro | Valor |
|-----------|-------|
| JDBC URL | `jdbc:h2:mem:templatedb` |
| Usuario | `sa` |
| Contraseña | *(vacía)* |

---

## Perfiles de configuración

| Perfil | Base de datos | Uso |
|--------|--------------|-----|
| `dev` (por defecto) | H2 en memoria | Desarrollo local, sin MySQL |
| `prod` | MySQL 8 | Producción |

El perfil activo se controla con `spring.profiles.active` en `application.properties` o con la variable de entorno `SPRING_PROFILES_ACTIVE`.

---

## Estructura de paquetes

```
src/main/java/com/template/
├── config/
│   ├── DataInitializer.java      # Crea el usuario admin al arrancar
│   ├── JpaConfig.java            # Habilita auditoría JPA (@CreatedDate, etc.)
│   └── WebSocketConfig.java      # Configura broker STOMP y endpoints WS
│
├── controller/
│   ├── AuthApiController.java    # POST /api/auth/login, /api/auth/register
│   ├── AuthController.java       # Vistas MVC login/register (Thymeleaf)
│   ├── DashboardController.java  # Vista principal autenticada
│   ├── ErrorViewController.java  # Mapeo de errores 403/404/500
│   ├── PostApiController.java    # REST CRUD /api/v1/posts
│   └── WebSocketController.java  # Mensajes STOMP /app/chat, /app/private
│
├── dto/
│   ├── AuthDTOs.java             # LoginRequest, RegisterRequest, JwtResponse
│   └── PostDTOs.java             # CreatePostRequest, PostResponse
│
├── entity/
│   ├── User.java                 # Entidad usuario con auditoría
│   ├── Role.java                 # Entidad rol (enum ERole)
│   ├── Post.java                 # Entidad post con tags y comentarios
│   ├── Tag.java                  # Etiqueta reutilizable
│   └── Comment.java              # Comentario vinculado a post y usuario
│
├── exception/
│   ├── GlobalExceptionHandler.java   # @RestControllerAdvice → JSON
│   ├── MvcExceptionHandler.java      # @ControllerAdvice → vistas Thymeleaf
│   ├── BadRequestException.java
│   ├── ResourceNotFoundException.java
│   └── UnauthorizedException.java
│
├── repository/
│   ├── UserRepository.java
│   ├── PostRepository.java
│   ├── RoleRepository.java
│   └── TagRepository.java
│
├── security/
│   ├── SecurityConfig.java           # Cadena de filtros, reglas de acceso
│   ├── JwtUtils.java                 # Generación y validación de tokens JWT
│   ├── JwtAuthenticationFilter.java  # Filtro que inyecta el contexto de seguridad
│   └── UserDetailsServiceImpl.java   # Carga UserDetails desde la BD
│
├── service/
│   ├── PostService.java          # Interfaz
│   ├── UserService.java          # Interfaz
│   └── impl/
│       ├── PostServiceImpl.java  # Lógica de negocio + PostPublishedEvent
│       └── UserServiceImpl.java  # Registro, búsqueda de usuarios
│
└── TemplateApplication.java      # Punto de entrada (@SpringBootApplication)

src/main/resources/
├── application.properties         # Config base
├── application-dev.properties     # Sobreescritura para perfil dev (H2)
├── application-prod.properties    # Sobreescritura para perfil prod (MySQL)
├── db/migration/
│   ├── V1__init_schema.sql        # Esquema completo de tablas
│   └── V2__seed_data.sql          # Roles y tags por defecto
├── static/
│   ├── css/theme.css
│   └── js/app.js, notifications.js
└── templates/
    ├── fragments/layout.html      # Layout Thymeleaf compartido
    ├── auth/login.html
    ├── auth/register.html
    ├── user/dashboard.html
    ├── user/post-detail.html
    ├── user/post-form.html
    ├── user/search-results.html
    ├── admin/panel.html
    └── error/403.html, 404.html, 500.html
```

---

## Modelo de datos (ER)

```
┌─────────────────┐       ┌──────────────┐
│      users      │       │    roles     │
├─────────────────┤       ├──────────────┤
│ id (PK)         │◄──┐   │ id (PK)      │
│ username        │   │   │ name (enum)  │
│ email           │   │   └──────────────┘
│ password        │   │          ▲
│ full_name       │   │          │ (ManyToMany)
│ enabled         │   └──────────┤
│ account_non_locked│         user_roles
│ created_at      │       ┌─────────────┐
│ updated_at      │       │ user_id(FK) │
└────────┬────────┘       │ role_id(FK) │
         │                └─────────────┘
         │ (OneToMany)
         ▼
┌─────────────────┐       ┌──────────────┐
│      posts      │       │     tags     │
├─────────────────┤       ├──────────────┤
│ id (PK)         │       │ id (PK)      │
│ title           │       │ name         │
│ content (TEXT)  │       └──────────────┘
│ published       │              ▲
│ author_id (FK)  │              │ (ManyToMany)
│ created_at      │◄─────────────┤
│ updated_at      │          post_tags
└────────┬────────┘       ┌─────────────┐
         │                │ post_id(FK) │
         │ (OneToMany)    │ tag_id (FK) │
         ▼                └─────────────┘
┌─────────────────┐
│    comments     │
├─────────────────┤
│ id (PK)         │
│ content (TEXT)  │
│ post_id (FK)    │
│ author_id (FK)  │
│ created_at      │
└─────────────────┘
```

---

## Endpoints

### Vistas MVC (Thymeleaf)

| Método | Ruta | Acceso | Descripción |
|--------|------|--------|-------------|
| GET | `/` | Público | Página principal |
| GET | `/auth/login` | Público | Formulario de login |
| GET | `/auth/register` | Público | Formulario de registro |
| POST | `/auth/login` | Público | Procesa login (Spring Security) |
| POST | `/auth/register` | Público | Procesa registro |
| GET | `/auth/logout` | Autenticado | Cierra sesión |
| GET | `/dashboard` | Autenticado | Panel de usuario |
| GET | `/admin/panel` | `ROLE_ADMIN` | Panel de administración |

### API REST (`/api/v1`)

Todos los endpoints REST devuelven `application/json`.  
Los endpoints protegidos requieren cabecera `Authorization: Bearer <token>`.

#### Autenticación

| Método | Ruta | Body | Descripción |
|--------|------|------|-------------|
| POST | `/api/auth/login` | `{ "usernameOrEmail", "password" }` | Obtiene JWT |
| POST | `/api/auth/register` | `{ "username", "email", "password", "fullName" }` | Registra usuario |

Respuesta de `/api/auth/login`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "username": "admin",
  "roles": ["ROLE_ADMIN", "ROLE_USER"]
}
```

#### Posts

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| GET | `/api/v1/posts?page=0&size=10&sort=createdAt` | No | Lista posts publicados (paginado) |
| GET | `/api/v1/posts/{id}` | No | Obtiene post por ID |
| POST | `/api/v1/posts` | Sí | Crea post |
| PUT | `/api/v1/posts/{id}` | Sí (autor o admin) | Actualiza post |
| DELETE | `/api/v1/posts/{id}` | Sí (autor o admin) | Elimina post |
| GET | `/api/v1/posts/search?q=texto&page=0` | No | Búsqueda de posts |

Body de creación/actualización:

```json
{
  "title": "Mi primer post",
  "content": "Contenido del post...",
  "published": true,
  "tagNames": ["spring-boot", "java"]
}
```

#### Actuator

| Ruta | Descripción |
|------|-------------|
| `/actuator/health` | Estado de la aplicación (público) |
| `/actuator/info` | Información de la app (autenticado) |
| `/actuator/metrics` | Métricas (autenticado) |

---

## WebSocket

Endpoint de conexión STOMP: `ws://localhost:8080/ws`

### Destinos de suscripción (cliente → suscribirse)

| Destino | Descripción |
|---------|-------------|
| `/topic/chat` | Canal de chat público (broadcast) |
| `/topic/notifications` | Notificaciones del sistema (nuevos posts, etc.) |
| `/user/queue/messages` | Mensajes privados para el usuario autenticado |

### Destinos de envío (cliente → servidor)

| Destino | Descripción |
|---------|-------------|
| `/app/chat` | Enviar mensaje al chat público |
| `/app/private` | Enviar mensaje privado |

### Ejemplo con JavaScript (SockJS + STOMP)

```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, frame => {
    // Suscribirse al chat
    stompClient.subscribe('/topic/chat', message => {
        const msg = JSON.parse(message.body);
        console.log(`${msg.sender}: ${msg.content}`);
    });

    // Enviar mensaje
    stompClient.send('/app/chat', {}, JSON.stringify({
        content: 'Hola mundo!',
        type: 'CHAT'
    }));
});
```

---

## Roles y permisos

| Rol | Descripción | Acceso |
|-----|-------------|--------|
| `ROLE_USER` | Usuario estándar | Dashboard, crear/editar/borrar sus propios posts |
| `ROLE_MODERATOR` | Moderador | Todo lo anterior + `/moderator/**` |
| `ROLE_ADMIN` | Administrador | Acceso total, incluido `/admin/**` |

Las restricciones se aplican en dos niveles: reglas de URL en `SecurityConfig` y anotaciones `@PreAuthorize` en los servicios.

---

## Variables de entorno

En producción **no** incluyas secretos en `application.properties`. Usa variables de entorno:

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Perfil activo | `prod` |
| `DB_URL` | URL JDBC de la BD | `jdbc:mysql://localhost:3306/template_db` |
| `DB_USERNAME` | Usuario de BD | `appuser` |
| `DB_PASSWORD` | Contraseña de BD | `s3cr3t` |
| `JWT_SECRET` | Secreto HMAC para firmar tokens (≥256 bits) | `MiClaveSuperSecretaDe64Chars...` |
| `JWT_EXPIRATION_MS` | Duración del token en ms | `86400000` (24 h) |

Referéncialas en `application-prod.properties`:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
app.jwt.secret=${JWT_SECRET}
app.jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}
```

---

## Docker

El proyecto incluye configuración completa para ejecutarse en contenedores.

### Archivos

| Archivo | Descripción |
|---------|-------------|
| `Dockerfile` | Build multi-stage: Maven compila, JRE Alpine ejecuta |
| `docker-compose.yml` | Orquesta los servicios `app` y `db` |
| `.dockerignore` | Excluye `target/`, `.git/`, IDEs y logs del contexto de build |
| `.env.example` | Plantilla de variables de entorno |
| `.env` | Variables reales (no subir al repo, está en `.gitignore`) |

### Servicios

| Servicio | Imagen | Puerto | Descripción |
|----------|--------|--------|-------------|
| `app` | Dockerfile local | 8080 | Aplicación Spring Boot |
| `db` | `mysql:8.0` | 3306 | Base de datos MySQL |

### Comandos útiles

```bash
# Arrancar en segundo plano
docker compose up -d --build

# Ver logs de la app
docker compose logs -f app

# Detener y eliminar contenedores (conserva el volumen de datos)
docker compose down

# Detener y eliminar contenedores + volumen de datos (reset completo)
docker compose down -v

# Reconstruir solo la imagen de la app
docker compose build app
```

### Variables de entorno para Docker

Copia `.env.example` a `.env` y ajusta los valores antes de arrancar:

| Variable | Descripción |
|----------|-------------|
| `MYSQL_ROOT_PASSWORD` | Contraseña de root de MySQL |
| `SPRING_PROFILES_ACTIVE` | Perfil Spring activo (`prod`) |
| `JWT_SECRET` | Clave HMAC ≥256 bits para firmar tokens JWT |
| `JWT_EXPIRATION_MS` | Duración del token de acceso en ms (por defecto 24h) |
| `JWT_REFRESH_EXPIRATION_MS` | Duración del refresh token en ms (por defecto 7 días) |
| `JAVA_OPTS` | Opciones de la JVM (por defecto `-Xms256m -Xmx512m`) |

> Genera un JWT_SECRET seguro con: `openssl rand -hex 32`

---

## DevContainer

El proyecto incluye configuración para [Dev Containers](https://containers.dev/), compatible con VS Code y Cursor.

### Requisitos

- Docker Desktop
- VS Code con la extensión [Dev Containers](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers) (o Cursor)

### Uso

1. Abre la carpeta del proyecto en VS Code / Cursor
2. Cuando aparezca la notificación "Reopen in Container", acéptala (o usa `Ctrl+Shift+P` → "Dev Containers: Reopen in Container")
3. El contenedor se construye automáticamente con Java 21 y todas las extensiones configuradas
4. Una vez dentro, la app se puede arrancar normalmente con `./mvnw spring-boot:run`

### Extensiones incluidas

- `vscjava.vscode-java-pack` — Soporte completo Java
- `pivotal.vscode-spring-boot` — Herramientas Spring Boot
- `redhat.vscode-xml` — Soporte XML/pom.xml
- `rangav.vscode-thunder-client` — Cliente REST integrado
- `mtxr.sqltools` + driver MySQL — Explorador de base de datos
- `eamodio.gitlens` — Git avanzado

---

## Tests

```bash
# Ejecutar todos los tests
./mvnw test

# Solo un test concreto
./mvnw test -Dtest=PostServiceImplTest
./mvnw test -Dtest=AuthApiControllerTest
./mvnw test -Dtest=JwtUtilsTest
```

Los tests usan H2 en memoria — no requieren MySQL.

---

## Migraciones Flyway

| Versión | Fichero | Contenido |
|---------|---------|-----------|
| V1 | `V1__init_schema.sql` | Esquema completo: tablas, índices, FK |
| V2 | `V2__seed_data.sql` | Roles por defecto + tags iniciales |

Para añadir una nueva migración, crea `V3__descripcion.sql` en `src/main/resources/db/migration/`. Flyway la aplicará automáticamente al arrancar.

---

## Licencia

Proyecto de plantilla educativa (PBL4). Libre para usar y modificar.
