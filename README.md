# OfflinePay Mesh

> **Digital Payments, Even Without Internet.**

OfflinePay Mesh is a Spring Boot prototype that simulates how a digital payment could move through disconnected devices before reaching a connected bridge and backend. It is a demonstration of store-and-forward payment concepts, not a real UPI, banking, or production payment system.

## 1. Project Overview

The project provides a small web dashboard for creating encrypted demo payment packets, observing their movement through virtual mesh devices, uploading them through a simulated bridge, and viewing the resulting transaction ledger.

The current implementation runs the sender simulation, mesh simulation, bridge ingestion, settlement services, and dashboard backend in one Spring Boot application.

## 2. Problem Statement

Digital payments normally depend on a continuous internet connection. Users may temporarily lose connectivity in remote areas, crowded venues, basements, or during network outages. A payment created while disconnected needs a safe way to wait, travel through nearby devices, and reach the backend later.

## 3. Proposed Solution

OfflinePay Mesh models an encrypted payment packet moving through a virtual device mesh. A packet is injected into an offline device, propagated through simulated gossip rounds, and eventually collected by a virtual bridge device with internet access. The bridge submits the packet for backend validation and settlement.

This is a simulation: the devices are Java objects inside the application and do not represent real phones or a real payment network.

## 4. How It Works

The implemented flow is:

```text
User creates payment
        |
        v
Encrypted payment packet is created
        |
        v
Packet enters a virtual device
        |
        v
Mesh gossip propagates packet copies and reduces TTL
        |
        v
Connected bridge uploads held packets
        |
        v
Bridge ingestion claims the ciphertext hash and decrypts the packet
        |
        v
Freshness, payment, PIN and balance validation
        |
        v
Settlement updates the account and transaction ledger
```

The sender simulation creates a `PaymentInstruction`, hashes the PIN inside that instruction, and encrypts it using the hybrid encryption service. The packet is wrapped in a `MeshPacket` with a packet ID, creation time, ciphertext, and TTL.

During ingestion, the backend:

- claims the ciphertext hash using the in-memory idempotency service;
- rejects duplicate deliveries within the configured idempotency window;
- decrypts the packet;
- validates the instruction and packet freshness;
- checks the sender PIN and balance;
- records a `SETTLED` or `REJECTED` transaction; and
- atomically updates sender and receiver balances for successful settlement.

## 5. Features

- Offline payment simulation through virtual devices
- Encrypted payment packets
- PIN verification
- Virtual mesh devices
- Gossip propagation with packet TTL handling
- Bridge ingestion
- Transaction settlement
- JVM-local idempotency and duplicate protection
- Account balance management
- Transaction history
- Responsive web dashboard
- Simple mesh visualization
- Payment progress and settlement status tracking
- Invalid payment rejection
- Transaction search, status filtering, and detail modal
- Mesh gossip, bridge upload, and reset controls

## 6. Technology Stack

- Java 17
- Spring Boot 3.3.5
- Spring MVC with embedded Tomcat
- Spring Data JPA and Hibernate
- H2 in-memory database
- Thymeleaf template rendering
- HTML, CSS, and vanilla JavaScript
- Maven and the Maven Wrapper
- Jakarta Bean Validation
- JUnit 5 and Spring Boot Test
- Java cryptography using RSA-2048 with OAEP-SHA256 and AES-256-GCM hybrid encryption

## 7. System Architecture

```text
Browser dashboard
        |
        | HTML page and REST/JSON requests
        v
Spring Boot controllers
        |
        +--> DemoService ------------> HybridCryptoService
        |
        +--> MeshSimulatorService ---> VirtualDevice objects
        |          |
        |          +--> gossip and TTL propagation
        |          +--> bridge packet collection
        |
        +--> BridgeIngestionService -> IdempotencyService
        |                                  |
        |                                  +--> decrypt and validate
        |                                  +--> SettlementService
        |
        v
Spring Data JPA / Hibernate
        |
        v
H2 in-memory accounts and transaction ledger
```

The main implementation areas are:

- `ApiController`: REST API for sending packets, mesh operations, bridge ingestion, accounts, and transactions.
- `DemoService`: seeds four demo accounts and simulates encrypted packet creation.
- `MeshSimulatorService`: manages five virtual devices, gossip rounds, TTL changes, and bridge uploads.
- `BridgeIngestionService`: handles duplicate protection, decryption, freshness checks, and ingestion results.
- `SettlementService`: validates PIN and balance, records transactions, and updates account balances transactionally.
- `HybridCryptoService`: performs the RSA-OAEP and AES-GCM packet encryption/decryption.
- `dashboard.html`: the four-view dashboard built with server-rendered HTML, CSS, and vanilla JavaScript.

## 8. Main API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/` | Serves the web dashboard. |
| `GET` | `/api/server-key` | Returns server public-key metadata used by the encryption demonstration. |
| `POST` | `/api/demo/send` | Creates and injects an encrypted demo payment packet into a selected virtual device. |
| `GET` | `/api/mesh/state` | Returns virtual device state, held packet IDs, and idempotency cache size. |
| `POST` | `/api/mesh/gossip` | Runs one simulated gossip round and returns transfer counts. |
| `POST` | `/api/mesh/flush` | Uploads packets held by bridge devices and returns ingestion outcomes. |
| `POST` | `/api/mesh/reset` | Clears virtual device packets and the idempotency cache. |
| `POST` | `/api/bridge/ingest` | Accepts a `MeshPacket` from a bridge and processes it through ingestion and settlement. |
| `GET` | `/api/accounts` | Returns the seeded demo accounts and current balances. |
| `GET` | `/api/transactions` | Returns the latest transaction records, newest first. |

The application also exposes the H2 console at `/h2-console` when running locally. This is a development convenience, not a production database interface.

## 9. Running Locally

### Requirements

- JDK 17 or newer
- Windows PowerShell or Command Prompt

The Java version is configured in `pom.xml` with `<java.version>17</java.version>`.

### Start the application on Windows

From the project directory, run:

```powershell
.\mvnw.cmd spring-boot:run
```

Open the dashboard at:

```text
http://localhost:8080
```

### Build and test

```powershell
.\mvnw.cmd -DskipTests package
.\mvnw.cmd test
```

The local H2 console uses:

```text
URL:      http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:upimesh
User:     sa
Password: empty
```

The H2 database is configured with `create-drop`, so its data is recreated when the application starts and discarded when it stops.

## 10. Demo Flow

1. Open the dashboard.
2. Review the four demo accounts and virtual mesh devices.
3. Open **Send Payment** and choose a sender, receiver, amount, PIN, TTL, and starting device.
4. Send a valid payment using the demo PIN `1234`.
5. Open **Mesh Network** and run a gossip round.
6. Run **Upload Bridge** to submit bridge-held packets.
7. Confirm the real `SETTLED` result and updated balances.
8. Send another payment with an invalid PIN.
9. Run gossip and bridge upload again, then confirm the `REJECTED` result.
10. Open **Transactions** to inspect the ledger and transaction details.
11. Return to **Mesh Network** to inspect device packet state or reset the simulation.

## 11. Screenshots

Screenshots can be added later at these paths:

![Dashboard](docs/dashboard.png)

![Send Payment](docs/payment.png)

![Transactions](docs/transactions.png)

![Mesh Network](docs/mesh.png)

These files are placeholders only; no screenshots are included in the current repository.

## 12. Limitations

- The mesh is simulated with in-memory Java `VirtualDevice` objects.
- Gossip treats the virtual devices as mutually reachable for demonstration purposes; it is not real Bluetooth or Wi-Fi Direct communication.
- The bridge is simulated by the `/api/mesh/flush` operation.
- The project is not connected to real UPI infrastructure, banks, or payment networks.
- H2 is an in-memory database and is reset on application restart.
- Idempotency is JVM-local and is not distributed across application instances.
- The RSA server key pair is generated again on every application startup.
- Demo accounts use the fixed PIN `1234`.
- The demo UI and APIs do not implement user authentication or authorization.
- The project does not solve real offline double-spending, device trust, or production key management.
- The current sender flow is a server-side simulation of what a real offline client would do.

## 13. Future Scope

These are future improvements only and are not implemented in the current project:

- Real device-to-device transport such as BLE.
- Durable cloud storage and a production-grade database.
- Distributed idempotency and reliable packet reconciliation.
- Stronger user authentication and device authorization.
- Hardware-backed keys and authenticated bridge communication.
- Integration with real payment infrastructure after appropriate compliance and security work.

## 14. Project Status

OfflinePay Mesh is a working prototype/demo for explaining encrypted store-and-forward payment concepts. It is suitable for local demonstrations and experimentation, but it is not production-ready financial software.
