# Event Registration Microservices System

An industry-grade microservices system built with Spring Boot, Spring Cloud, PostgreSQL, and Docker. 

## Features
- **Centralized Gateway**: Single entry point with JWT validation.
- **Service Discovery**: Eureka server for dynamic routing.
- **Normalized DBs**: 3 separate PostgreSQL databases with 7-table industry-standard schema.
- **Razorpay Integration**: End-to-end payment flow in registration service.
- **Concurrency Control**: JPA Optimistic Locking on event capacity and registrations.

## Services
| Service | Port | Description |
| :--- | :--- | :--- |
| `service-registry` | 8761 | Eureka Server |
| `api-gateway` | 8080 | Routing & Global JWT Auth |
| `auth-service` | 8081 | User Management & Login |
| `event-service` | 8082 | Event CRUD & Filtering |
| `registration-service` | 8083 | Registration & Payment (Razorpay) |

## Quick Start (Docker)
1. Ensure Docker and Docker Compose are installed.
2. (Optional) Create a `.env` file in the root directory and add:
   ```
   NGROK_AUTHTOKEN=your_authtoken_here
   ```
3. Run `docker compose up --build -d`.
   - If using ngrok, find your public URL in the logs or at `http://localhost:4040`.
4. Eureka Dashboard: `http://localhost:8761`.
5. API Gateway: `http://localhost:8080`.

## API Documentation (Swagger UI)
Access the documentation for each service through the gateway:
- **Auth Service**: `http://localhost:8080/auth/swagger-ui.html`
- **Event Service**: `http://localhost:8080/events/swagger-ui.html`
- **Registration Service**: `http://localhost:8080/registration/swagger-ui.html`

## Features & Robustness
- **Global Exception Handling**: All services return structured JSON error responses.
- **Extensible Payments**: Supported modes: `RAZORPAY`, `MANUAL` (Mock).
- **Ownership Validation**: Organizers manage only their own events.
- **Cross-Service Validation**: Feign clients used for inter-service consistency.

## Tech Stack
- Java 17, Spring Boot 3.x, Spring Cloud
- PostgreSQL, JWT, Razorpay SDK
- Docker, Docker Compose
