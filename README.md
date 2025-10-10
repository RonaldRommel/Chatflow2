# ChatFlow Load Test Client

This project provides a **WebSocket-based load testing client** for the ChatFlow server. It simulates multiple clients sending messages concurrently, tracks latency, retries failed messages, and generates throughput and latency statistics in CSV and chart form.

---

## Project Structure

The project uses Maven and is divided into three main folders:


* **server**: Contains the ChatFlow server code.
* **client-part1**: Optional initial client setup or part 1 of the assignment.
* **client-part2**: Load testing client with full asynchronous message sending and metrics collection.
* **results**: Contains generated CSV and throughput chart outputs.
Each module has been built individually using Maven and can be run independently.

---

## Features

* Simulates multiple WebSocket clients (connections) sending messages concurrently.
* Tracks per-message latency and success/failure metrics.
* Handles retries for pending messages with exponential backoff.
* Generates CSV metrics and throughput charts.
* Configurable number of messages, threads, and connections.

---

## Prerequisites

* Java 17 or higher
* Maven or Gradle (for dependencies)
* ChatFlow server running on a reachable IP/hostname
* Internet/network access to the ChatFlow server

Dependencies used in the project:

* `org.java-websocket:Java-WebSocket`
* `com.fasterxml.jackson.core:jackson-databind`
* `com.fasterxml.jackson.datatype:jackson-datatype-jsr310`
* Spring Framework (`@Component` annotation used)

---

## Setup

1. **Clone the repository**

```bash
git clone <repository-url>
cd <repository-folder>
```

2. **Build each module separately**

```bash
cd server
./mvnw clean install

cd ../client-part1
./mvnw clean install

cd ../client-part2
./mvnw clean install
```

3. **Ensure directories exist for output files**

```bash
mkdir -p ./results
```

---

## Configuration

You can configure the following parameters in `LoadTestClient`:

| Parameter         | Description                                    | Default |
| ----------------- | ---------------------------------------------- | ------- |
| `SERVER_URL`      | WebSocket server URL (set dynamically in code) | ""      |
| `TOTAL_MESSAGES`  | Total number of messages to send               | 32000   |
| `INITIAL_THREADS` | Number of concurrent sending threads           | 10      |
| `POOL_SIZE`       | Number of WebSocket connections                | 20      |

**Example:** `runLoadTest("127.0.0.1")` will connect to `ws://127.0.0.1:8080/chat/`.

---
## Running the Chat Application

### Step 1: Start the server

* Navigate to the `server` module.
* Run the Spring Boot application `WebSocketServerApplication.java`.
* The server listens on **port 8080**.
* It can be hosted locally or on a remote server.

```bash
cd server
./mvnw spring-boot:run
```

### Step 2: Start the client

* Once the server is running, navigate to the client module (`client-part1` or `client-part2`).
* Run the client application `ClientApplication2.java` or `ClientApplication.java`.
* Pass the server IP address or `localhost` as an argument.

```bash
cd client-part2
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server=127.0.0.1"
```

* The client will connect to **port 8080** on the server and start sending messages.
* Metrics such as latency and throughput will be logged and saved in the `results` folder.

> Make sure the server is running before starting the client to avoid connection errors.


## Output

1. **CSV metrics**: `./results/latency_metrics.csv`
   Columns: `clientTimestamp,messageType,latency,status,roomId`

2. **Throughput chart**: `./results/throughput_chart.png`

3. **Console output**:

    * Total messages attempted
    * Successful and failed messages
    * Runtime and throughput

---

## Notes

* Each message is retried up to 5 times if pending.
* The test may temporarily spike messages/sec above predicted throughput due to asynchronous sending.
* Adjust `POOL_SIZE` and `INITIAL_THREADS` to simulate different load patterns.

---

## License

MIT License
