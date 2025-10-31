# AWS Load Balancer Deployment Guide

## Prerequisites
- AWS CLI configured with appropriate credentials
- 4 EC2 instances ready (or will be created by script)
- RabbitMQ instance running
- Security groups configured

## Architecture
```
[Client] → [ALB] → [Server1, Server2, Server3, Server4] → [RabbitMQ] → [Consumers]
```

## Quick Start

### Step 1: Set Up Infrastructure
```bash
cd deployment
chmod +x setup-alb.sh deploy-servers.sh
./setup-alb.sh
```

### Step 2: Deploy Servers
```bash
./deploy-servers.sh
```

### Step 3: Verify
- Check ALB: http://your-alb-dns-name/health
- Check target health in AWS Console

## Configuration Details

### Sticky Sessions
- **Type**: Application-based cookie
- **Cookie name**: AWSALB
- **Duration**: 86400 seconds (24 hours)

### Health Check
- **Path**: `/health`
- **Interval**: 30 seconds
- **Timeout**: 5 seconds
- **Healthy threshold**: 2
- **Unhealthy threshold**: 3

### WebSocket Support
- **Protocol**: HTTP/HTTPS with upgrade support
- **Idle timeout**: 300 seconds (5 minutes)
- **Connection draining**: 60 seconds