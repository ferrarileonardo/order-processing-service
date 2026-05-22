# Order Processing Service

Microservicio de procesamiento de pedidos desarrollado con tecnologías Telco.

## Tecnologías

- Java 17
- Spring Boot 3.2.5 + Gradle
- Spring WebFlux (reactive) — puerto 9898
- Spring Data MongoDB Reactive
- Spring Actuator + Prometheus
- Log4j2 (log4j2.yml)
- gRPC + Protobuf — puerto 9090
- Akka Typed Actors
- SMPP via Cloudhopper ch-smpp

## Requisitos previos

- Java 17
- MongoDB corriendo en localhost:27017
- Gradle (incluido via wrapper)

## Configuración (application.yml)

```yaml
mongodbDatabase: exampleDb
mongodbUri: "mongodb://127.0.0.1:27017"
apiPort: 9898
```

## Cómo correr el proyecto

**Terminal 1 — Servidor SMPP local:**
```bash
.\gradlew runSmppServer
```

**Terminal 2 — Aplicación Spring Boot:**
```bash
.\gradlew bootRun
```

## Endpoints REST

**Consultar estado de un pedido:**
GET http://localhost:9898/orders/{orderId}

**2. Total de pedidos por rango de fecha:**
GET http://localhost:9898/orders/total?from=2026-01-01T00:00:00Z&to=2026-12-31T23:59:59Z

**3. Health check:**
GET http://localhost:9898/actuator/health

**4. Métricas Prometheus:**
GET http://localhost:9898/actuator/prometheus

## Servicio gRPC

- Puerto: `9090`
- Servicio: `orderprocessing.OrderService`
- Método: `ProcessOrder`

**Request:**
```json
{
  "orderId": "ORD-001",
  "customerId": "CUST-001",
  "customerPhoneNumber": "+1234567890",
  "items": [
    { "sku": "SKU-001", "quantity": 2 }
  ]
}
```

**Response:**
```json
{
  "orderId": "ORD-001",
  "status": "PROCESSED"
}
```

## Flujo de procesamiento
Cliente
→ gRPC puerto 9090
→ OrderServiceImpl
→ OrderActor (Akka)
→ MongoDB (persiste OrderDocument)
→ SMPP (SMS: "Your order {id} has been processed")
→ Prometheus (incrementa orders_processed_total)
→ Respuesta gRPC al cliente

## Modelo de datos MongoDB

```json
{
  "_id": "ObjectId",
  "orderId": "ORD-001",
  "customerId": "CUST-001",
  "customerPhoneNumber": "+1234567890",
  "status": "PROCESSED",
  "items": ["SKU-001 x2"],
  "ts": "2026-05-22T05:51:20.497+00:00"
}
```

## Métricas Prometheus

| Métrica | Tipo | Descripción |
|---|---|---|
| `orders_processed_total` | Counter | Total de pedidos procesados |
| `grpc_server_requests_received_messages_total` | Counter | Requests gRPC recibidos |
| `http_server_requests_seconds` | Summary | Duración de requests HTTP |