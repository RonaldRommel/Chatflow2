# Health Check Endpoint

The ALB requires a health check endpoint to monitor server health.

## Implementation

Add this to your Spring Boot server:

### `ServerController.java`
```java
package com.chatflow.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServerController {
    
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
```

## Health Check Configuration

- **Path**: `/health`
- **Expected Response**: `200 OK` with body "OK"
- **Interval**: 30 seconds
- **Timeout**: 5 seconds
- **Healthy Threshold**: 2 consecutive successes
- **Unhealthy Threshold**: 3 consecutive failures

## Testing
```bash
# Test locally
curl http://localhost:8080/health

# Test via ALB
curl http://your-alb-dns.us-west-2.elb.amazonaws.com/health
```

## Monitoring

Check target health in AWS Console:
1. Go to EC2 → Load Balancers
2. Select your ALB
3. Click "Target Groups"
4. View "Targets" tab for health status