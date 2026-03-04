# Business Tracer Spring Boot Starter

Business Tracer is a lightweight distributed business link tracing component based on Spring Boot. It allows developers to trace the complete lifecycle of a business object (like an Order) across multiple services and disparate technical traces by using a unified **Business ID**.

Unlike traditional APM (TraceId-based), Business Tracer aggregates logs based on **Business Identity**, solving the problem of fragmented logs in long-running business processes (e.g., Order Create -> Pay -> Ship).

## Features

- **Business ID Centric**: Aggregates logs by Business ID (e.g., Order No) across different requests and services.
- **Node & Detail Tracking**:
    - **Node**: High-level step defined by `@BusinessTrace` on methods.
    - **Detail**: Fine-grained steps recorded via `BusinessTracer.record()` inside nodes.
- **Distributed Propagation**: Automatically propagates Business ID and context via HTTP headers (`X-Business-Id`) between microservices.
- **MDC Integration**: Automatically injects `businessId` and `traceId` into SLF4J MDC for correlation with standard logs.
- **Persistent Storage**: Stores trace data in MySQL using MyBatis-Plus for long-term retention and analysis.

## Quick Start

### 1. Requirements

- Java 8+
- Spring Boot 2.x
- MySQL Database

### 2. Installation

Install the starter to your local Maven repository:

```bash
mvn clean install
```

### 3. Dependency

Add the dependency to your project's `pom.xml`:

```xml
<dependency>
    <groupId>com.bananice</groupId>
    <artifactId>business-tracer-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 4. Database Setup

Execute the initialization script to create the necessary table:

`sql/init.sql`:

```sql
CREATE TABLE IF NOT EXISTS `business_trace_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `business_id` varchar(64) NOT NULL,
  `trace_id` varchar(64) DEFAULT NULL,
  `node_id` varchar(64) NOT NULL,
  `parent_node_id` varchar(64) DEFAULT NULL,
  `log_type` varchar(16) NOT NULL, -- NODE or DETAIL
  `content` text,
  `group_id` varchar(64) DEFAULT NULL,
  `app_name` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_business_id` (`business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 5. Configuration

Configure your database connection in `application.yml` or `application.properties`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_db?useSSL=false
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  application:
    name: your-service-name
```

## Usage

### 1. Annotation Mode (Defining Nodes)

Use `@BusinessTrace` on your service methods to define a "Node" in the business flow. Use SpEL to extract the Business ID.

```java
@Service
public class OrderService {

    @BusinessTrace(key = "#order.orderId", operation = "Create Order", groupId = "ORDER_FLOW")
    public void createOrder(Order order) {
        // Business logic...
    }
}
```

### 2. Programmatic Mode (Recording Details)

Inside a traced method (Node), use `BusinessTracer.record` to add detailed logs.

```java
@Service
public class OrderService {

    @BusinessTrace(key = "#orderId", operation = "Process Payment")
    public void pay(String orderId) {
        // ...
        BusinessTracer.record("Payment validation successful");
        // ...
        BusinessTracer.record("Payment gateway response received");
    }
}
```

### 3. Distributed Tracing

When making HTTP calls to downstream services, the component automatically handles context propagation if you use standard mechanisms (interceptor logic is provided). Ensure downstream services also include this starter.

The following headers are propagated:
- `X-Business-Id`
- `X-Trace-Id`
- `X-Parent-Node-Id`

## Architecture

The project follows a Domain-Driven Design (DDD) structure:

- **API**: Public annotations and static helpers.
- **Domain**: Core entities (`LogRecord`) and repository interfaces.
- **Infrastructure**: Implementation of persistence (MyBatis-Plus), Context (ThreadLocal/MDC), and AOP.
- **Config**: Spring Boot auto-configuration.

## License

MIT
