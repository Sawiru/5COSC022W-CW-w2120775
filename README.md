# Smart Campus API

> A RESTful API built with JAX-RS (Jersey) and Grizzly for managing Rooms and Sensors across a university smart campus. Built for **5COSC022W Client-Server Architectures (2025/26)**.

---

## Technology Stack

| Technology | Details |
|---|---|
| Language | Java 11 |
| JAX-RS Implementation | Jersey 2.41 |
| Embedded Server | Grizzly HTTP Server |
| JSON Serialization | Jackson |
| Build Tool | Maven |
| Data Storage | In-memory `ConcurrentHashMap` |

---

## Project Structure

```
src/main/java/com/smartcampus/
├── Main.java
├── SmartCampusApp.java
├── model/
│   ├── Room.java
│   ├── Sensor.java
│   └── SensorReading.java
├── resource/
│   ├── DiscoveryResource.java
│   ├── RoomResource.java
│   ├── SensorResource.java
│   └── SensorReadingResource.java
├── storage/
│   └── DataStore.java
└── exception/
    ├── RoomNotEmptyException.java
    ├── RoomNotEmptyExceptionMapper.java
    ├── LinkedResourceNotFoundException.java
    ├── LinkedResourceNotFoundExceptionMapper.java
    ├── SensorUnavailableException.java
    ├── SensorUnavailableExceptionMapper.java
    └── GlobalExceptionMapper.java
```

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1` | Discovery — returns API metadata and resource links |
| GET | `/api/v1/rooms` | Get all rooms |
| POST | `/api/v1/rooms` | Create a new room |
| GET | `/api/v1/rooms/{roomId}` | Get a specific room by ID |
| DELETE | `/api/v1/rooms/{roomId}` | Delete a room (blocked if sensors are assigned) |
| GET | `/api/v1/sensors` | Get all sensors (supports `?type=` filter) |
| POST | `/api/v1/sensors` | Register a new sensor |
| GET | `/api/v1/sensors/{sensorId}` | Get a specific sensor by ID |
| GET | `/api/v1/sensors/{sensorId}/readings` | Get all readings for a sensor |
| POST | `/api/v1/sensors/{sensorId}/readings` | Add a new reading to a sensor |

---

## How to Build and Run

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api
```

**2. Build the project**
```bash
mvn clean install
```

**3. Run the server**
```bash
mvn exec:java
```

**4. The API will be available at**
```
http://localhost:8080/api/v1
```

---

## Sample curl Commands

### 1. Get API discovery info
```bash
curl http://localhost:8080/api/v1
```

### 2. Create a room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"LIB-301\",\"name\":\"Library Quiet Study\",\"capacity\":30}"
```

### 3. Get all rooms
```bash
curl http://localhost:8080/api/v1/rooms
```

### 4. Create a sensor (linked to a room)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"CO2-001\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":400.0,\"roomId\":\"LIB-301\"}"
```

### 5. Get sensors filtered by type
```bash
curl "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 6. Add a reading to a sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d "{\"value\":450.5}"
```

### 7. Attempt to delete a room that still has sensors (triggers 409)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 8. Attempt to register a sensor with a non-existent roomId (triggers 422)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"FAKE-001\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0.0,\"roomId\":\"FAKE-999\"}"
```

---

## Error Handling

| Scenario | Exception | HTTP Status |
|---|---|---|
| Delete room with active sensors | `RoomNotEmptyException` | 409 Conflict |
| Register sensor with non-existent roomId | `LinkedResourceNotFoundException` | 422 Unprocessable Entity |
| Post reading to a sensor in MAINTENANCE | `SensorUnavailableException` | 403 Forbidden |
| Any unexpected runtime error | `GlobalExceptionMapper` | 500 Internal Server Error |

---

## Report — Question Answers

### Part 1 — Q1: JAX-RS Resource Lifecycle

By default, JAX-RS uses a **request-scoped** lifecycle, meaning a brand new instance of each resource class is instantiated for every incoming HTTP request and discarded immediately after the response is sent. This is the opposite of a **singleton**, where one shared instance handles all requests for the entire lifetime of the application.

Because of the request-scoped default, instance variables inside a resource class cannot be used to store persistent data — they are reset on every request. To maintain state across requests, the `DataStore` class uses `static ConcurrentHashMap` fields, which exist at the class level and survive for the entire application lifetime.

`ConcurrentHashMap` is chosen over a regular `HashMap` because JAX-RS handles concurrent requests on multiple threads simultaneously. A regular `HashMap` is not thread-safe — simultaneous writes from two threads can corrupt its internal structure entirely, causing data loss or unpredictable behaviour. `ConcurrentHashMap` uses internal segment locking to allow safe concurrent reads and writes without requiring manual `synchronized` blocks, effectively preventing race conditions and guaranteeing data integrity across all requests.

---

### Part 1 — Q2: What is HATEOAS?

**HATEOAS** (Hypermedia as the Engine of Application State) is the principle that API responses should include hypermedia links guiding the client to related actions and resources, rather than requiring clients to construct URLs themselves. For example, a response returning a room object would also include embedded links such as:

```json
{
  "_links": {
    "sensors": "/api/v1/rooms/LIB-301/sensors",
    "delete": "/api/v1/rooms/LIB-301"
  }
}
```

This makes the API **self-documenting** — clients discover all available actions at runtime by following the links provided, in the same way a browser navigates a website without needing to know URLs in advance.

Compared to static documentation, HATEOAS is significantly superior because static docs become outdated the moment a URL structure changes, forcing developers to update every client application manually. With hypermedia links, clients automatically adapt to URL changes since they always follow what the server provides rather than relying on hard-coded paths. This loose coupling between client and server is considered a hallmark of a mature, well-designed RESTful API.

---

### Part 2 — Q3: Returning Full Objects vs IDs in a List

When returning a list of rooms, returning **full objects** gives the client everything they need in a single request, which is convenient but costly in terms of bandwidth — especially when there are hundreds of rooms each containing many fields. A large payload also increases client-side parsing time.

Returning **only IDs** is very lightweight on the network but forces the client to make an additional `GET` request for every single room they want details about, which can result in dozens of extra round trips — a well-known anti-pattern sometimes called the N+1 problem.

The recommended approach for most APIs is to return full objects in list responses but keep them **lean** — including the resource's own fields while excluding deeply nested relationships. For example, a room list should include the room's own data but not embed the full sensor objects inside it; returning just the sensor IDs at that level is sufficient. This balances network efficiency with client convenience.

---

### Part 2 — Q4: Is DELETE Idempotent?

Yes, the `DELETE` operation is **idempotent** in this implementation. Idempotency means that sending the same request multiple times produces the same server state as sending it once.

If a client sends `DELETE /api/v1/rooms/LIB-301` and the room exists, it gets deleted and a `200 OK` is returned. If the exact same request is sent again, the room no longer exists and the server returns a `404 Not Found`. Although the HTTP response code differs between the first and subsequent calls, the **server state is identical** after both — the room is absent either way, and no data corruption or unintended side effect occurs.

This property makes `DELETE` safe to retry in unreliable network conditions where a client cannot confirm whether the first request was received, without any risk of causing unintended duplicate deletions.

---

### Part 3 — Q5: What Happens if the Wrong Content-Type is Sent to a `@Consumes` Endpoint

The `@Consumes(MediaType.APPLICATION_JSON)` annotation declares a contract to the JAX-RS runtime that a given endpoint will only accept requests carrying a `Content-Type` header of `application/json`. 

If a client sends data with a different `Content-Type` — such as `text/plain` or `application/xml` — the JAX-RS runtime intercepts the request **before it ever reaches the method body** and automatically returns an **HTTP 415 Unsupported Media Type** response. The resource method is never executed. No manual content-type checking is required inside the code — the framework enforces the declared contract entirely on its own. This keeps resource methods clean and focused purely on business logic.

---

### Part 3 — Q6: `@QueryParam` vs Path Segment for Filtering

Using `@QueryParam` for filtering — as in `GET /api/v1/sensors?type=CO2` — is considered superior to embedding the filter value directly in the URL path, such as `GET /api/v1/sensors/type/CO2`, for several reasons.

**Query parameters are optional by nature**, meaning the same single endpoint cleanly handles both the filtered and unfiltered case without requiring two separate method definitions. Path segments, by contrast, represent **resource identity and hierarchy** — they are meant to locate a specific resource, not to filter a collection.

Multiple filters also compose naturally with query parameters, for example `?type=CO2&status=ACTIVE`, whereas doing this with path segments would produce a deeply nested and confusing URL structure. Query parameters are also the widely accepted convention understood by all HTTP clients, proxies, and caching layers, making the API more predictable and easier to consume for developers.

---

### Part 4 — Q7: Sub-Resource Locator Pattern Benefits

The Sub-Resource Locator pattern works by having a method in a parent resource class **return an instance of a dedicated child resource class** rather than handling all nested paths directly in one place. In this implementation, `SensorResource` delegates anything under `/{sensorId}/readings` to a dedicated `SensorReadingResource` class.

The primary benefit is **separation of concerns** — each class has one clear, well-defined responsibility. If every nested endpoint were defined inside a single monolithic resource class, the file would grow extremely large, becoming difficult to read, test, debug, and maintain as the API evolves. By delegating to sub-resource classes, each class remains small and focused.

It also improves **reusability** — `SensorReadingResource` could in principle be instantiated from multiple parent resources if the business domain required it. This approach mirrors good object-oriented design principles and reflects how large production APIs are structured in industry, where controllers are kept thin and logic is distributed across focused, single-responsibility classes.

---

### Part 5 — Q8: Why 422 is More Semantically Accurate Than 404 for a Missing `roomId` Reference

**HTTP 404 Not Found** is intended to signal that the URL or endpoint being requested does not exist on the server. However, when a client sends a `POST` request to `/api/v1/sensors` with a `roomId` that does not exist in the system, the URL `/api/v1/sensors` is completely valid and reachable — the problem is not with the endpoint itself, but with the **content inside the request body**.

**HTTP 422 Unprocessable Entity** is far more semantically accurate in this scenario because it signals that the server successfully received and parsed the request, the JSON was syntactically well-formed, but the **payload failed a business logic validation rule** — specifically, that the referenced `roomId` does not correspond to any existing resource.

Returning a 404 here would mislead the client into believing they called a wrong or non-existent URL, when in reality the URL is perfectly correct and the issue lies entirely within their JSON payload. The 422 status code communicates precisely what went wrong, making it significantly easier for client developers to identify, diagnose, and correct their requests.

---

### Part 5 — Q9: Cybersecurity Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers presents several serious cybersecurity risks.

**1. Framework and library version disclosure** — Stack traces reveal the exact internal package and class names of the application, including the precise frameworks and libraries in use along with their version numbers. This allows an attacker to search public vulnerability databases such as the CVE (Common Vulnerabilities and Exposures) registry for known exploits targeting those specific versions, effectively handing them a roadmap to attack the system.

**2. Internal file system path exposure** — Stack traces frequently contain absolute file system paths showing exactly where the application is deployed on the server. This information can be leveraged in path traversal attacks or used to infer the server's directory structure.

**3. Application logic flow disclosure** — A stack trace shows the exact sequence of method calls that led to an error, revealing internal business logic, conditional branches, and data flow. An attacker can use this to identify weak points in the logic and craft targeted malicious inputs.

The `GlobalExceptionMapper` in this implementation addresses all of these risks by implementing `ExceptionMapper<Throwable>`, which intercepts every unexpected runtime exception before it can reach the HTTP response layer. It returns only a clean, generic `500 Internal Server Error` message with no internal details, ensuring the application's architecture, dependencies, and logic are never disclosed to external consumers.
