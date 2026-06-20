# Beer Catalogue API

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Boot Actuator
- SpringDoc OpenAPI / Swagger UI
- H2 Database
- PostgreSQL
- Maven
- Docker / Docker Compose
- Kubernetes / Minikube

## Overview

`beer-catalogue` is a REST API for browsing and managing beers and manufacturers.

The application supports:

- CRUD operations for `Manufacturer`
- CRUD operations for `Beer`
- flexible beer search
- pagination for beer and manufacturer listings
- beer image upload and retrieval
- role-based access control
- local execution with H2
- AWS/PostgreSQL execution through a dedicated Spring profile
- containerized execution with Docker
- Kubernetes deployment with Minikube-ready manifests

Swagger UI is available at:

- `http://localhost:8080/swagger-ui.html`

The OpenAPI document is available at:

- `http://localhost:8080/v3/api-docs`

## Domain Model

### Manufacturer

A manufacturer contains:

- `id`
- `name`
- `countryOfOrigin`

### Beer

A beer contains:

- `id`
- `name`
- `abv`
- `type`
- `description`
- `manufacturer`
- optional image binary
- optional image content type

### AppUser

Security is backed by a database user model:

- `id`
- `username`
- `password`
- `role`
- optional `manufacturerId`

`manufacturerId` is used to link a manufacturer user to the manufacturer they are allowed to manage.

## Active Profiles

The application currently uses two runtime modes:

- `default`
  - local development profile by convention
  - uses the in-memory H2 database
  - no additional datasource configuration is required
- `aws`
  - uses PostgreSQL
  - datasource configuration is provided through environment variables
  - configuration file: [application-aws.yaml](/Users/sviatoslavslivinskiy/IdeaProjects/beer-catalogue/src/main/resources/application-aws.yaml)

Environment variables required for the `aws` profile:

- `SPRING_PROFILES_ACTIVE`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Local Setup

### Run with Maven

Default local mode with H2:

```bash
./mvnw spring-boot:run
```

Run against PostgreSQL / AWS profile:

In IntelliJ:

1. Open `Run | Edit Configurations`
2. Select the Spring Boot run configuration
3. In `Environment variables`, put:

```text
SPRING_PROFILES_ACTIVE=aws;SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<db>?sslmode=require;SPRING_DATASOURCE_USERNAME=<username>;SPRING_DATASOURCE_PASSWORD=<password>
```

Command-line alternative:

```bash
SPRING_PROFILES_ACTIVE=aws \
SPRING_DATASOURCE_URL='jdbc:postgresql://<host>:5432/<db>?sslmode=require' \
SPRING_DATASOURCE_USERNAME='<username>' \
SPRING_DATASOURCE_PASSWORD='<password>' \
./mvnw spring-boot:run
```

### Build and Test

```bash
./mvnw test
```

## Docker

The project is Dockerized with a multi-stage `Dockerfile`.

### Build the image

```bash
docker build -t beer-catalogue .
```

### Run with the default profile

```bash
docker run -p 8080:8080 beer-catalogue
```

### Run with Docker Compose

`compose.yaml` is configured to start the application with the `aws` profile.

Before starting Docker, open [compose.yaml](/Users/sviatoslavslivinskiy/IdeaProjects/beer-catalogue/compose.yaml) and replace the datasource values with real PostgreSQL values:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Run:

```bash
docker compose up --build
```

## API Design

The API follows a resource-oriented REST style under `/api/v1`.

### Manufacturer Endpoints

- `GET /api/v1/manufacturers`
- `GET /api/v1/manufacturers/{id}`
- `POST /api/v1/manufacturers`
- `PUT /api/v1/manufacturers/{id}`
- `DELETE /api/v1/manufacturers/{id}`

### Beer Endpoints

- `GET /api/v1/beers`
- `GET /api/v1/beers/{id}`
- `POST /api/v1/beers`
- `PUT /api/v1/beers/{id}`
- `DELETE /api/v1/beers/{id}`
- `POST /api/v1/beers/{id}/image`
- `GET /api/v1/beers/{id}/image`

### HTTP Semantics

- `200 OK` for successful reads and updates
- `201 Created` for resource creation
- `204 No Content` for deletes and image upload
- `400 Bad Request` for invalid payloads or invalid query parameters
- `403 Forbidden` for authenticated users without sufficient permissions
- `404 Not Found` for missing resources

Errors are returned as structured JSON through a global exception handler.

## Search and Pagination

Beer listing supports filtering and sorting through the existing `GET /api/v1/beers` endpoint.

Supported beer filters:

- `name`
- `type`
- `abv`
- `manufacturer`

Supported pagination parameters:

- `page`
- `size`

Supported sorting parameters:

- `sortBy`
- `direction`

Example:

```http
GET /api/v1/beers?name=punk&type=IPA&page=0&size=10&sortBy=name&direction=asc
```

Manufacturer listing also supports pagination:

```http
GET /api/v1/manufacturers?page=0&size=10
```

## Picture Upload

Each beer can store one image.

Upload endpoint:

```http
POST /api/v1/beers/{id}/image
```

Requirements:

- request type: `multipart/form-data`
- file field name: `file`
- allowed content types:
  - `image/jpeg`
  - `image/png`
  - `image/webp`

Retrieval endpoint:

```http
GET /api/v1/beers/{id}/image
```

The API returns the raw image bytes with the stored content type.

## Role-Based Access Control

Authentication uses HTTP Basic Auth backed by database users.

Current role model:

- `ADMIN`
- `MANUFACTURER`

Access rules:

- anonymous users
  - read-only access to `GET /api/v1/**`
- manufacturer users
  - can modify only their own manufacturer data
  - can modify only beers that belong to their own manufacturer
- admin users
  - can modify all resources

### How it works

- Spring Security authenticates the request from Basic Auth credentials
- the authenticated user is loaded from the `app_users` table
- authorization is enforced in the service layer through [AccessService.java](/Users/sviatoslavslivinskiy/IdeaProjects/beer-catalogue/src/main/java/com/haufe/beercatalogue/service/AccessService.java)
- manufacturer ownership is checked against `manufacturerId`

The application seeds one admin user on startup:

- username: `admin`
- password: `admin123`

Manufacturer users can be added in the database and linked to a `manufacturerId`.

## Design Decisions and Tradeoffs

### REST resource structure

- Beer and manufacturer management is exposed as standard REST resources.
- Beer image handling is implemented as a sub-resource: `/beers/{id}/image`.

This keeps the main beer JSON payload clean and avoids embedding binary data in regular CRUD responses.

### Flexible beer search

- Search is implemented as query parameters on the main beer listing endpoint.
- JPA `Specification` is used to build dynamic queries.

Tradeoff:

- this avoids creating many repository methods for all filter combinations
- exact ABV matching is simple and sufficient for the assignment, but an ABV range could be a future improvement

### Pagination

- Pagination is implemented on listing endpoints using Spring Data `Page`
- the API currently returns Spring `Page` directly for simplicity

Tradeoff:

- very little code
- but a custom paged DTO would provide a more stable external response contract

### Role-based access

- ownership checks are enforced in the service layer instead of complex expression-based security

Tradeoff:

- logic is explicit and easy to review
- security rules are slightly more imperative, but simpler for this assignment

### Image storage

- Beer images are stored in PostgreSQL as binary data for simplicity

Tradeoff:

- easy to implement and self-contained
- not ideal for production-scale file storage

Recommended future improvement:

- move images to S3
- keep only object metadata / object key in the database

### Profiles and database choice

- H2 is used for local default execution because it keeps setup friction low
- PostgreSQL is used for the `aws` profile because it matches the target deployment architecture

## AWS-Hosted Database (PostgreSQL)

The `aws` profile is designed to connect to PostgreSQL, including AWS RDS.

The application relies on:

- PostgreSQL JDBC driver in [pom.xml](/Users/sviatoslavslivinskiy/IdeaProjects/beer-catalogue/pom.xml)
- `application-aws.yaml` for profile-specific JPA settings
- environment variables for datasource credentials

Schema handling:

- `spring.jpa.hibernate.ddl-auto=update`

This allows Hibernate to create missing tables and update the schema on startup.

## Cloud Deployment

Kubernetes deployment files are provided under [kubernetes](/Users/sviatoslavslivinskiy/IdeaProjects/beer-catalogue/kubernetes).

Included resources:

- namespace
- secret template
- deployment
- service
- kustomization file

The provided deployment is Minikube-friendly and uses:

- a single replica
- `NodePort` service exposure
- health probes on `/actuator/health`
- environment-driven PostgreSQL configuration

Detailed Kubernetes execution steps are documented in:

- [kubernetes/README.md](/Users/sviatoslavslivinskiy/IdeaProjects/beer-catalogue/kubernetes/README.md)

### Secret handling note

Before applying the Kubernetes manifests, open [kubernetes/secret.yaml](/Users/sviatoslavslivinskiy/IdeaProjects/beer-catalogue/kubernetes/secret.yaml) and replace the datasource values with real PostgreSQL values:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Testing

The project currently uses both unit and integration tests.

### Unit tests

- service layer tests isolate business logic with mocked repositories and collaborators

### Integration tests

- controller integration tests run the Spring context
- requests are executed through `MockMvc`
- H2 is used during test execution
- end-to-end API behavior is verified for:
  - CRUD flows
  - validation
  - security
  - search
  - pagination
  - image upload and retrieval

## Useful Links

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Actuator health: `http://localhost:8080/actuator/health`

## Future Improvements

The current solution is intentionally pragmatic and assignment-focused. The following improvements would make it more production-ready:

- Deploy the application to AWS EKS and expose it publicly.
  - This would move the deployment from local Minikube to a managed Kubernetes platform and make the API accessible on the internet in a controlled way.

- Store beer images in S3 instead of PostgreSQL.
  - Object storage is a better fit for binary files than a relational database. This would reduce database size, simplify backups, and improve scalability. The database would then keep only image metadata or the S3 object key.

- Introduce token-based authentication and authorization.
  - The current Basic Auth setup is sufficient for the assignment, but token-based security, for example JWT or OAuth2, is a better fit for real client applications and distributed deployments.

- Add a load balancer.
  - A load balancer would allow traffic to be distributed across multiple application instances and would provide a proper public entry point in cloud deployment scenarios.

- Create database read replicas.
  - Read replicas would improve scalability for read-heavy workloads such as catalogue browsing and search, while reducing pressure on the primary PostgreSQL instance.

- Introduce in-memory caching.
  - Caching frequently requested data, such as beer listings, manufacturer details, or image metadata, would reduce repeated database reads and improve response times for common read operations.
