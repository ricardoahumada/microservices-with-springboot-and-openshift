
# DDD in a Microservices E-Commerce Application

> “We started with a well-structured monolith. Now, business demands independent scaling, deployment, and team autonomy — so we split along our DDD Bounded Contexts.”


## Evolution from Monolith

Remember our monolith’s package structure?

```
com.bookstore.ordermanagement
com.bookstore.inventory
com.bookstore.payment
com.bookstore.notifications
```

Each becomes its own **deployable service**, with:
- Independent codebase
- Independent database
- Independent deployment & scaling
- Communication via APIs or events (async preferred)


## 1. Service Boundaries = Bounded Contexts

Each microservice owns its domain and data:

| Service             | Responsibility                          | Tech Stack (Example)      |
|---------------------|------------------------------------------|----------------------------|
| `order-service`     | Manage orders, enforce invariants        | Java + Spring Boot + PostgreSQL |
| `inventory-service` | Track stock, handle reservations         | Java + Spring Boot + PostgreSQL |
| `payment-service`   | Process payments, manage payment status  | Node.js + MongoDB          |
| `notification-service` | Send emails, SMS on domain events     | Python + Redis + SendGrid  |

> **DDD wins again**: Because we modeled Bounded Contexts clearly in the monolith, splitting was straightforward.


## 2. Ubiquitous Language — Still Critical!

Same terms, now crossing service boundaries:

- `OrderConfirmed`, `StockReserved`, `PaymentFailed`
- `BookId`, `CustomerId`, `OrderId` — still shared (but now via contracts)

> Business language remains the backbone — even more important now to avoid translation errors between services.


## 3. Example: Order Service (Java)

Structure inside `order-service`:

```
src/
 └── main/java/com/bookstore/order/
     ├── domain/          → Order, LineItem, OrderConfirmedEvent
     ├── application/     → PlaceOrderCommandHandler
     ├── infrastructure/  → REST controllers, DB repositories, event publishers
     └── client/          → Feign clients or async event listeners for other services
```

### Aggregate Root: `Order.java` (Same as Monolith!)

```java
public class Order {
    private final OrderId id;
    private final CustomerId customerId;
    private List<LineItem> lineItems;
    private Money total;
    private OrderStatus status;

    public Order(OrderId id, CustomerId customerId) { ... }

    public void addItem(BookId bookId, Quantity quantity, Money unitPrice) { ... }

    public void confirm() {
        if (lineItems.isEmpty()) throw new IllegalStateException(...);
        this.status = OrderStatus.CONFIRMED;
        DomainEvents.add(new OrderConfirmedEvent(this.id)); // Still local event!
    }
}
```

> **Key insight**: The domain model doesn’t change! DDD protects you from infrastructure churn.


## 4. Cross-Service Communication — Events over HTTP

In the monolith, we used Spring’s `ApplicationEventPublisher`.

In microservices, we use **asynchronous messaging** — e.g., Kafka, RabbitMQ, or AWS SNS/SQS.

### Publishing Domain Events (Infrastructure Layer)

```java
@Component
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener
    public void handle(OrderConfirmedEvent event) {
        kafkaTemplate.send("order-events", event.orderId().value(), event);
    }
}
```

> Domain layer still pure — events are published *after* transaction commits.


## 5. Handling Events in Other Services

### Inventory Service — Listens to `OrderConfirmedEvent`

```java
@KafkaListener(topics = "order-events")
public void handleOrderConfirmed(OrderConfirmedEvent event) {
    // 1. Look up order items (via OrderService API or denormalized data)
    List<OrderItem> items = orderClient.getOrderItems(event.orderId());

    // 2. Confirm stock reservation
    inventoryService.confirmReservation(event.orderId(), items);

    // 3. Publish: StockConfirmedEvent (if needed)
}
```

> **Challenge**: How does `inventory-service` know what books are in the order?
> → Options: Call `order-service` API, or include item details in the event (denormalization).


## 6. Application Service — Now with Distributed Coordination

```java
@Service
@Transactional
public class PlaceOrderCommandHandler {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient; // HTTP or async?
    private final PaymentClient paymentClient;

    public OrderId execute(PlaceOrderCommand command) {
        Order order = new Order(command.orderId(), command.customerId());
        command.items().forEach(item -> order.addItem(...));

        // ❗ Option 1: Synchronous call — risky!
        if (!inventoryClient.reserveStock(command.items())) {
            throw new OutOfStockException();
        }

        order.confirm(); // raises OrderConfirmedEvent
        orderRepository.save(order);

        // ❗ Option 2: Trigger payment asynchronously via event
        // PaymentService listens to OrderConfirmedEvent

        return order.getId();
    }
}
```

> **Distributed complexity alert!**
> - Synchronous calls (`inventoryClient.reserveStock`) can cause failures and latency.
> - Better: Reserve stock asynchronously via `StockReservationRequested` event → eventual consistency.


## 7. Event-Driven Flow (Recommended)

Instead of synchronous calls, embrace eventual consistency:

1. `OrderService`: Order created → `OrderPlacedEvent`
2. `InventoryService`: Listens → reserves stock → emits `StockReservedEvent`
3. `OrderService`: Listens → confirms order → emits `OrderConfirmedEvent`
4. `PaymentService`: Listens → initiates payment → emits `PaymentProcessedEvent`
5. `NotificationService`: Listens → sends email

> This is **choreography** — services react to events, no central orchestrator.

Alternatively, use **orchestration** (e.g., with a Saga pattern in `OrderService` coordinating steps and rollback compensations).


## 8. Database Per Service — Critical!

Each service owns its data:

| Service             | Database Tables / Collections             |
|---------------------|-------------------------------------------|
| `order-service`     | `orders`, `order_items`                   |
| `inventory-service` | `book_stock`, `reservations`              |
| `payment-service`   | `payments`, `transactions`                |
| `notification-service` | `notifications`, `delivery_status`     |

> No direct DB access across services! Communication only via APIs or events.


## 9. Shared Data? Use Contracts, Not Shared DB

In monolith, we had `shared.kernel` with `BookId`, `CustomerId`.

In microservices, share via:

- **API Contracts** (OpenAPI/Swagger)
- **Event Schemas** (AsyncAPI, Avro, JSON Schema)
- **Shared Library (Optional)** — e.g., `bookstore-domain-contracts.jar` with DTOs and events — but beware of coupling!

> Avoid shared databases — that’s a “distributed monolith” anti-pattern.


## 10. Package Structure per Microservice

Each service is its own project:

```
order-service/
├── src/main/java/com/bookstore/order/
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── OrderServiceApplication.java
├── pom.xml
└── Dockerfile

inventory-service/
├── src/...
```

> Deploy independently: `docker build -t order-service:1.2 .`, `kubectl apply -f order-deployment.yaml`


## 11. Trade-offs: Microservices vs Monolith

| Aspect                  | Monolith (DDD)                          | Microservices (DDD)                          |
|-------------------------|------------------------------------------|-----------------------------------------------|
| Deployment              | Single artifact                          | Independent per service                       |
| Scaling                 | Scale entire app                         | Scale services independently                  |
| Team autonomy           | Low — shared codebase                    | High — teams own services end-to-end          |
| Complexity              | Low (local calls, single DB)             | High (network, eventual consistency, tracing) |
| Debugging               | Easy — single log, debugger              | Hard — distributed tracing needed (e.g., Zipkin) |
| Transactions            | ACID within context                      | Sagas, eventual consistency                   |
| Dev environment         | Run one app                              | Need Docker Compose / Kubernetes locally      |
| Time to market (early)  | Faster                                   | Slower (infra setup, contracts, observability) |
| Evolution path          | Hard to split later                      | Easy to split — if DDD boundaries are clean   |

> **Rule of thumb**: Start with DDD monolith → split when pain points emerge (team size, scaling needs, deployment bottlenecks).


## 12. Testing Differences

- **Monolith**: Easy unit + integration tests. Can test full flows in one JVM.
- **Microservices**:
  - Unit tests per service
  - Contract tests (Pact) between services
  - End-to-end tests require Docker/K8s setup
  - Test doubles (e.g., WireMock) for external service calls


## 13. Observability — Now Mandatory

In microservices, you need:

- **Distributed Tracing** (Jaeger, Zipkin)
- **Centralized Logging** (ELK, Loki)
- **Metrics & Dashboards** (Prometheus + Grafana)
- **Health Checks & Circuit Breakers** (Resilience4j, Hystrix)

> Without these, you’re flying blind.


## 14. When Does This Make Sense?

Split into microservices when:

- Teams are > 5–7 per service area → need autonomy.
- Services have different scaling needs (e.g., inventory vs notifications).
- Need independent deployment cycles (e.g., marketing changes in notifications shouldn’t require order service redeploy).
- Regulatory or compliance boundaries (e.g., payments must be isolated).

> 🚫 Don’t start here — start with DDD monolith, split when needed.


## Summary: Key Differences

| Concept                 | Monolith                                | Microservices                             |
|-------------------------|------------------------------------------|--------------------------------------------|
| **Code Location**       | One codebase, modules as packages        | Separate repos/projects per service        |
| **Data**                | Shared DB, tables per context            | Dedicated DB per service                   |
| **Communication**       | In-process calls, local events           | HTTP APIs or async messaging (Kafka, etc.) |
| **Consistency**         | Strong (ACID within context)             | Eventual (Sagas, compensating actions)     |
| **Deployment**          | Single unit                              | Independent per service                    |
| **Complexity**          | Low (early), manageable with DDD         | High — network, observability, contracts   |
| **DDD Value**           | Prevents “big ball of mud”               | Enables clean splits and team ownership    |
| **Evolution Path**      | Can split into microservices later       | Hard to merge back into monolith 😅         |
