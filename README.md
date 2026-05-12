### Voucher System

A high-concurrency voucher management system designed for flash sale scenarios. This system utilizes a resilient architecture to handle massive traffic spikes, ensuring data integrity, preventing over-claiming, and maintaining low latency through distributed caching and asynchronous processing.

#### Core Architecture and Engineering Design

**1. High-Concurrency Handling**
The system prevents database bottlenecks during peak loads by using **Redis with Lua Scripts** for atomic quota management. This ensures that voucher decrements are handled in-memory with ACID-like atomicity at the cache layer, preventing race conditions and over-claiming.

**2. Asynchronous Persistence**
To decouple high-speed claiming from database writes, the system employs **RabbitMQ**. Once a voucher is successfully claimed in Redis, a message is published to the broker. A background worker consumes these messages to persist records into **PostgreSQL**, ensuring the system remains responsive even under heavy database pressure.

**3. Idempotency and Resiliency**
The system implements strict idempotency for both claiming and application processes. Using unique request keys, it prevents duplicate transactions. It also includes a compensation mechanism (Refund API) to restore voucher status in case of downstream payment failures.

#### Technology Stack

* **Java 21** with **Spring Boot 4.0.0**
* **PostgreSQL 15** (Primary Database)
* **Redis** (Distributed Cache and Quota Management)
* **RabbitMQ** (Message Broker)
* **k6** (Performance Testing)
* **Docker & Docker Compose** (Containerization)

---

#### Getting Started

**Prerequisites**

* Docker and Docker Compose
* Java 21 (for local development)
* k6 (optional, for local load testing)

**1. Environment Configuration**
Clone the repository and rename a file in the root directory `.env.example` to `.env`

**2. Build and Run with Docker Compose**
The entire stack, including the Spring Boot application and infrastructure, is containerized for a consistent environment.

```bash
docker-compose up -d --build
```

The application will be available at `http://localhost:8080`

---

#### Performance Testing

**Load Testing with k6**
The project includes a comprehensive k6 script to simulate flash sale traffic. The test follows a "Zero-Install" approach using Docker Compose profiles.

**1. Run the Load Test**
Execute the following command to start the performance test:

```bash
docker-compose --profile test up k6-test
```

**2. Test Scenario**

* **Setup Phase:** Automatically initializes a new campaign and voucher rules via Admin APIs.
* **Execution Phase:** Simulates up to 500 concurrent users competing for a limited quota.
* **Teardown Phase:** Summarizes results and validates metrics against defined thresholds.

**3. Success Criteria (Thresholds)**

* **Error Rate:** 0.00% (No internal server errors allowed).
* **Latency:** p(95) < 500ms, p(99) < 1000ms.
* **Integrity:** The total claimed vouchers must exactly match the defined quota.

---

#### API Endpoints

**Voucher Operations**

* `POST /api/v1/vouchers/claim` : Request a voucher from a specific rule.
* `POST /api/v1/vouchers/apply` : Apply a claimed voucher to an order with idempotency check.

**Admin Operations**

* `POST /api/v1/admin/campaigns` : Create and initialize new marketing campaigns.
* `POST /api/v1/admin/campaigns/{id}/publish` : Publish a campaign to make it active.

---