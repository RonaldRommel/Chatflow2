# ChatFlow Distributed Chat System

A scalable, distributed WebSocket-based chat system with RabbitMQ message queuing, load balancing support, and comprehensive performance monitoring.

---

## Project Overview

ChatFlow is an enterprise-grade chat infrastructure that supports:
- Real-time message delivery between users
- Message distribution through RabbitMQ queues
- Multiple chat rooms with user presence tracking
- Load balancing across multiple server instances
- Performance metrics collection and analysis

---

## Project Structure
```
chatflow/
├── server-v2/           WebSocket server with RabbitMQ integration
├── client/              Load testing client with metrics collection
├── deployment/          AWS ALB configuration and deployment scripts
├── monitoring/          Java-based monitoring tools
└── results/             Generated metrics and performance data
```

---

## Architecture
```
Client (100 threads)
    ↓ WebSocket
Server (Spring Boot + WebSocket)
    ↓ Publish
RabbitMQ (Fanout exchanges, 20 rooms)
    ↓ Consume
Consumers (100 threads, 5 per room)
    ↓ Broadcast
WebSocket Clients (in rooms)
```

### Message Flow
1. Client sends message via WebSocket
2. Server validates and publishes to RabbitMQ exchange
3. Consumer pulls from queue
4. Consumer broadcasts to all clients in room
5. Sender receives own message as acknowledgment

---

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- RabbitMQ (Docker or standalone)
- AWS CLI (for deployment)

---

## Quick Start

### 1. Start RabbitMQ
```bash
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

Access Management UI: http://localhost:15672 (guest/guest)

### 2. Build and Run Server
```bash
cd server-v2
mvn clean package
java -jar target/chatflow-server-2.0.0.jar
```

Server runs on: http://localhost:8080

### 3. Run Load Test Client
```bash
cd client
mvn clean package
mvn spring-boot:run
```

---

## Configuration

### Server Configuration (application.properties)
```properties
server.port=8080
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

### Client Configuration (ClientPool.java)
```java
private static final int CLIENT_COUNT = 100;
```

Adjust thread count based on system capacity.

---

## Performance Metrics

### Target Metrics
- Throughput: 15,000+ messages/second
- Response Time (P99): < 2000ms
- Success Rate: 100%
- Queue Depth: < 1000 messages

### Collected Metrics
- Per-message latency (send to acknowledgment)
- Throughput over time (10-second buckets)
- Message type distribution
- Room-level statistics

---

## Monitoring

### Real-time Queue Monitoring
```bash
cd monitoring
mvn clean package
java -jar target/chatflow-monitoring.jar queue-monitor localhost
```

### Server Health Monitoring
```bash
java -jar target/chatflow-monitoring.jar server-health http://localhost:8080
```

### Metrics Analysis
```bash
java -jar target/chatflow-monitoring.jar metrics-analyzer ../client/metrics.csv
```

### RabbitMQ Management Console

Access: http://localhost:15672

Monitor:
- Queue depths
- Message rates
- Consumer activity
- Connection status

---

## Output Files

After running the load test:
```
client/
├── metrics.csv              Per-message latency data
└── throughput.csv           Throughput over time (10s buckets)
```

### CSV Formats

**metrics.csv:**
```
timestamp,messageType,latencyMs,roomId
2025-10-31T12:00:00Z,TEXT,245,room5
```

**throughput.csv:**
```
timeSeconds,messagesPerSecond
0,12345.50
10,15678.20
```

---

## Testing Scenarios

### Single Server Test
```bash
# 500,000 messages, 100 client threads
mvn spring-boot:run
```

Expected:
- Runtime: 30-50 seconds
- Throughput: 10,000-20,000 msg/s
- Response time: 1-5 seconds

### Load Balanced Test (requires AWS deployment)
```bash
cd deployment
./setup-alb.sh
./deploy-servers.sh
```

Update client to use ALB endpoint:
```java
String serverUrl = "ws://your-alb-dns.us-west-2.elb.amazonaws.com/chat";
```

---

## Troubleshooting

### High Latency
- Increase consumer threads per room
- Increase RabbitMQ prefetch count
- Check queue depths (should be < 1000)

### Messages Not Received
- Verify clients registered in rooms
- Check RabbitMQ consumer count
- Ensure server/consumer running

### Connection Failures
- Check server capacity
- Verify network connectivity
- Review firewall/security group rules

### Queue Buildup
```bash
# Check queue depths
docker exec rabbitmq rabbitmqctl list_queues

# Reset if needed
docker exec rabbitmq rabbitmqctl reset
```

---

## Performance Tuning

### Increase Throughput
1. Add more consumer threads per room (5-10)
2. Increase client thread count (150-200)
3. Increase RabbitMQ prefetch (100-200)
4. Use multiple server instances with ALB

### Reduce Latency
1. Decrease queue depth
2. Optimize broadcast logic
3. Use async broadcasting
4. Increase consumer count

---

## System Requirements

### Development/Testing
- 8GB RAM minimum
- 4 CPU cores
- 10GB disk space

### Production (AWS)
- Server: t3.medium or larger
- RabbitMQ: t3.medium or larger
- Client: runs on local machine

---

## Architecture Highlights

### Queue Topology
- 20 fanout exchanges (1 per room)
- Per-server queues (auto-delete on disconnect)
- At-least-once delivery guarantee

### Threading Model
- Client: 100 threads sharing message queue
- Server: Spring Boot thread pool (default)
- Consumers: 100 threads (5 per room)
- Broadcasts: Synchronous per-session

### Connection Management
- Channel pooling (50 producer, 150 consumer)
- Connection reuse
- Graceful shutdown handling

---

## Development

### Build All Modules
```bash
mvn clean install -DskipTests
```

### Run Tests
```bash
mvn test
```

### Package for Deployment
```bash
mvn clean package
```

Creates executable JARs in `target/` directories.

---

## License

MIT License

---

## Assignment Compliance

This project fulfills CS6650 Assignment 2 requirements:
- Message queue integration with RabbitMQ
- Multi-threaded consumer implementation
- Load balancing preparation (ALB scripts)
- Comprehensive monitoring tools
- Performance metrics collection
- System tuning and optimization