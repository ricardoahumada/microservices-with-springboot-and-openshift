# DDD in a Monolithic E-Commerce Application

Imagine you're building an **online bookstore** as a single deployable application (a monolith), but you want to apply DDD principles to manage complexity and keep the code maintainable.

Even though it’s a monolith, the system is divided into **well-defined modules** that reflect **Bounded Contexts** and use core DDD patterns like aggregates, domain events, repositories, and ubiquitous language.



## 1. Bounded Contexts (Packaged by Module)

The monolith is organized into packages representing Bounded Contexts:

```
com.bookstore.ordermanagement
com.bookstore.inventory
com.bookstore.payment
com.bookstore.notifications
```

Each package is **independent in responsibility**, with its own domain model — even though they all live in the same codebase and database.



## 2. Ubiquitous Language

Team uses consistent terms:
- `Order`, `LineItem`, `BookId`, `Reservation`, `PaymentConfirmed`

These names appear in code, documentation, and conversations with business stakeholders.



## 3. Example: Order Management Context (Java)

Let’s look at the `ordermanagement` context.

### Aggregate Root: `Order.java`

```java
package com.bookstore.ordermanagement.domain;

public class Order {
    private final OrderId id;
    private final CustomerId customerId;
    private List<LineItem> lineItems;
    private Money total;
    private OrderStatus status;

    // Constructor enforces invariants
    public Order(OrderId id, CustomerId customerId) {
        if (id == null || customerId == null) {
            throw new IllegalArgumentException("Order must have ID and customer");
        }
        this.id = id;
        this.customerId = customerId;
        this.lineItems = new ArrayList<>();
        this.status = OrderStatus.DRAFT;
    }

    // Business method – aggregate guards consistency
    public void addItem(BookId bookId, Quantity quantity, Money unitPrice) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot add items to non-draft order");
        }
        LineItem item = new LineItem(bookId, quantity, unitPrice);
        this.lineItems.add(item);
        recalculateTotal();
    }

    public void confirm() {
        if (lineItems.isEmpty()) {
            throw new IllegalStateException("Cannot confirm empty order");
        }
        this.status = OrderStatus.CONFIRMED;
        // Raise event for other parts of the system
        DomainEvents.raise(new OrderConfirmedEvent(this.id));
    }

    private void recalculateTotal() {
        this.total = lineItems.stream()
                .map(item -> item.unitPrice().multiply(item.quantity().value()))
                .reduce(Money.ZERO, Money::add);
    }

    // Getters...
}
```

### Value Objects

```java
// Immutable value object
public class Money {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public Money add(Money other) { /* ... */ }
    public boolean equals(Object o) { /* by value */ }
}

public class OrderId {
    private final String value;
    // wrapper for type safety
}
```

### Domain Event

```java
package com.bookstore.ordermanagement.events;

public class OrderConfirmedEvent implements DomainEvent {
    private final OrderId orderId;

    public OrderConfirmedEvent(OrderId orderId) {
        this.orderId = orderId;
    }

    // getter...
}
```

### Repository Interface (in domain layer)

```java
package com.bookstore.ordermanagement.domain;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId id);
}
```

> Note: This interface belongs to the **domain layer**. Implementation lives in infrastructure.



## 4. Application Service (Orchestrates Use Case)

```java
package com.bookstore.ordermanagement.application;

@Service
public class PlaceOrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService; // from another context
    private final PaymentGateway paymentGateway;

    public PlaceOrderCommand(OrderId orderId, CustomerId customerId, List<Item> items) {
        // Validate input
        Order order = new Order(orderId, customerId);
        items.forEach(i -> order.addItem(i.bookId(), i.quantity(), i.price()));

        // Confirm only if inventory reserved
        if (inventoryService.reserveStock(items)) {
            order.confirm();  // raises OrderConfirmedEvent
            orderRepository.save(order);

            // Trigger payment asynchronously
            paymentGateway.processPayment(order.total(), order.customerId());
        } else {
            throw new OutOfStockException();
        }
    }
}
```



## 5. Cross-Context Communication via Events

When `OrderConfirmedEvent` is raised, other contexts react:

```java
// In notifications context
@EventListener
public void handle(OrderConfirmedEvent event) {
    notificationService.sendEmail(
        lookupEmail(event.customerId()),
        "Your order is confirmed!"
    );
}

// In inventory context
@EventListener
public void handle(OrderConfirmedEvent event) {
    inventoryService.confirmReservation(event.orderId());
}
```

Events are published locally (e.g., using Spring’s `ApplicationEventPublisher`) — no Kafka needed yet.



## 6. Package Structure (Monolith with DDD)

```
src/
 └── main/java/com/bookstore/
     ├── ordermanagement/
     │   ├── domain/          → Order, LineItem, Money, Domain Events
     │   ├── application/     → PlaceOrderService
     │   ├── infrastructure/  → JPA implementations, Spring controllers
     │
     ├── inventory/
     │   ├── domain/          → BookStock, Reservation
     │   ├── application/     → Stock reservation logic
     │
     ├── payment/
     │   ├── domain/          → Payment, PaymentStatus
     │
     ├── shared/
     │   ├── kernel/          → Shared VO: BookId, CustomerId (optional, carefully used)
     │
     └── config/              → Spring config, event listeners
```

> The **database can be shared** (e.g., one PostgreSQL schema), but tables are scoped per context to reduce coupling.



### Why This Works in a Monolith

- **Clear boundaries**: Each Bounded Context is isolated in its package.
- **No cyclic dependencies**: Upper layers depend on lower ones (via interfaces).
- **Easy to evolve**: You can later extract `ordermanagement` into a microservice — the aggregate and events are already well-defined.
- **Testable**: Domain logic has no framework dependencies.



### When Might You Split Into Microservices?

Later, if:
- The team grows and needs independent deployment.
- Inventory needs real-time stock syncing across warehouses.
- Orders become too large a bottleneck.

Then you split along DDD boundaries:
- `order-service`
- `inventory-service`
- `payment-service`

But because you used DDD from the start, the **seams are already clean**.



### Summary

- **Microservices are not need** to use DDD.
- This Java-based monolith shows how DDD helps to:
    - Model complex business rules clearly.
    - Enforce invariants with aggregates.
    - Decouple logic using domain events.
    - Prepare for scale — without over-engineering early.

> **Best practice**: Use DDD to build a *well-structured monolith first*. Let business needs — not hype — decide if and when to go distributed.