# Event Registration Microservices System

A robust, microservices-based application built for managing event registrations, payments, and organizers. Built with Spring Boot 3, Spring Security (JWT), PostgreSQL, and Docker.



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
---

## 🏛️ Architecture

The system consists of **3 Core Microservices**, an API Gateway, and a Service Registry (Eureka):

1. **Auth + User Service** (`:8081`) - Handles JWT generation, role-based access, and user profiles.
2. **Event Service** (`:8082`) - Handles CRUD operations, status management, and event filtering.
3. **Registration Service** (`:8083`) - Handles user registrations, capacity validations, Razorpay payments, and PDF receipt generation.

---

## 🚀 Tech Stack

* **Language:** Java 17
* **Framework:** Spring Boot 3.2.x, Spring Cloud, Spring Security
* **Authentication:** Stateless JWT (JSON Web Tokens)
* **Database:** PostgreSQL (Containerized)
* **API Documentation:** Swagger UI (OpenAPI 3)
* **Deployment:** Docker & Docker Compose
* **Testing:** JUnit 5, Mockito
* **Payment Gateway:** Razorpay Webhooks
* **Other Tools:** Lombok, Maven, OpenFeign

---

## 👥 User Roles & Access Control

* **ADMIN (`ROLE_ADMIN`)** - Full privileges. Can create Organizer and Registrant accounts via specific `/admin` endpoints.
* **ORGANIZER (`ROLE_ORGANIZER`)** - Can create, update, delete, and close their *own* events.
* **REGISTRANT (`ROLE_REGISTRANT`)** - Can browse events, register, make payments, and view their receipts.

---

## 🐳 Docker Setup & Running the Application

Ensure you have **Docker** and **Docker Compose** installed on your machine.

### 1. Build the Microservices
Navigate to the root directory and run the following command to build the Docker images for all services:
```bash
docker-compose build
```

### 2. Start the Infrastructure
Start the PostgreSQL databases, Eureka Service Registry, and the API Gateway along with the microservices:
```bash
docker-compose up -d
```

### 3. Accessing the Application
Once the containers are healthy (wait ~40 seconds for Eureka to register the services):
* **API Gateway:** `http://localhost:8080`
* **Eureka Dashboard:** `http://localhost:8761`

---

## 📖 API Documentation (Swagger)

All services dynamically generate OpenAPI definitions. You can view the full interactive API documentation automatically deployed with the containers:

* **Auth Service Docs:** `http://localhost:8081/swagger-ui.html`
* **Event Service Docs:** `http://localhost:8082/swagger-ui.html`
* **Registration Service Docs:** `http://localhost:8083/swagger-ui.html`

---

## 🛡️ Key Validations Implemented
* **Capacity Enforcement:** The registration API checks event `maxCapacity`.
* **Duplicate Prevention:** Users cannot register twice for the same event.
* **Ownership Checking:** Organizers can only edit and delete events they created.
* **Event Status Enforcement:** Registrations are strictly blocked if an event is set to `CLOSED`.
* **Webhook Resilience:** Razorpay `payment.failed` webhooks correctly mark registrations as failed to free up ticket capacity for other users.


## Features & Robustness
- **Global Exception Handling**: All services return structured JSON error responses.
- **Extensible Payments**: Supported modes: `RAZORPAY`, `MANUAL` (Mock).
- **Ownership Validation**: Organizers manage only their own events.
- **Cross-Service Validation**: Feign clients used for inter-service consistency.
