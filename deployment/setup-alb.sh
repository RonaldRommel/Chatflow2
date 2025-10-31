#!/bin/bash

# AWS Load Balancer Setup Script for ChatFlow
# This script creates an ALB with target groups and health checks

set -e

echo "🚀 ChatFlow Load Balancer Setup"
echo "================================"

# Configuration
REGION="us-west-2"
VPC_ID=""  # Will be detected or you can set manually
ALB_NAME="chatflow-alb"
TARGET_GROUP_NAME="chatflow-servers"
SECURITY_GROUP_NAME="chatflow-alb-sg"
HEALTH_CHECK_PATH="/health"
PORT=8080

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}Step 1: Detecting VPC${NC}"
if [ -z "$VPC_ID" ]; then
    VPC_ID=$(aws ec2 describe-vpcs \
        --region $REGION \
        --filters "Name=isDefault,Values=true" \
        --query 'Vpcs[0].VpcId' \
        --output text)
fi
echo -e "${GREEN}✓ Using VPC: $VPC_ID${NC}"

echo -e "\n${YELLOW}Step 2: Getting Subnets${NC}"
SUBNET_IDS=$(aws ec2 describe-subnets \
    --region $REGION \
    --filters "Name=vpc-id,Values=$VPC_ID" \
    --query 'Subnets[*].SubnetId' \
    --output text | tr '\t' ' ')
echo -e "${GREEN}✓ Found subnets: $SUBNET_IDS${NC}"

echo -e "\n${YELLOW}Step 3: Creating Security Group${NC}"
SG_ID=$(aws ec2 create-security-group \
    --region $REGION \
    --group-name $SECURITY_GROUP_NAME \
    --description "Security group for ChatFlow ALB" \
    --vpc-id $VPC_ID \
    --query 'GroupId' \
    --output text 2>/dev/null || \
    aws ec2 describe-security-groups \
        --region $REGION \
        --filters "Name=group-name,Values=$SECURITY_GROUP_NAME" \
        --query 'SecurityGroups[0].GroupId' \
        --output text)

# Add inbound rules
aws ec2 authorize-security-group-ingress \
    --region $REGION \
    --group-id $SG_ID \
    --protocol tcp \
    --port 80 \
    --cidr 0.0.0.0/0 2>/dev/null || true

aws ec2 authorize-security-group-ingress \
    --region $REGION \
    --group-id $SG_ID \
    --protocol tcp \
    --port 8080 \
    --cidr 0.0.0.0/0 2>/dev/null || true

echo -e "${GREEN}✓ Security Group: $SG_ID${NC}"

echo -e "\n${YELLOW}Step 4: Creating Target Group${NC}"
TG_ARN=$(aws elbv2 create-target-group \
    --region $REGION \
    --name $TARGET_GROUP_NAME \
    --protocol HTTP \
    --port $PORT \
    --vpc-id $VPC_ID \
    --health-check-protocol HTTP \
    --health-check-path $HEALTH_CHECK_PATH \
    --health-check-interval-seconds 30 \
    --health-check-timeout-seconds 5 \
    --healthy-threshold-count 2 \
    --unhealthy-threshold-count 3 \
    --query 'TargetGroups[0].TargetGroupArn' \
    --output text 2>/dev/null || \
    aws elbv2 describe-target-groups \
        --region $REGION \
        --names $TARGET_GROUP_NAME \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text)

echo -e "${GREEN}✓ Target Group ARN: $TG_ARN${NC}"

echo -e "\n${YELLOW}Step 5: Configuring Sticky Sessions${NC}"
aws elbv2 modify-target-group-attributes \
    --region $REGION \
    --target-group-arn $TG_ARN \
    --attributes \
        Key=stickiness.enabled,Value=true \
        Key=stickiness.type,Value=app_cookie \
        Key=stickiness.app_cookie.cookie_name,Value=AWSALB \
        Key=stickiness.app_cookie.duration_seconds,Value=86400 \
        Key=deregistration_delay.timeout_seconds,Value=60

echo -e "${GREEN}✓ Sticky sessions configured${NC}"

echo -e "\n${YELLOW}Step 6: Creating Application Load Balancer${NC}"
ALB_ARN=$(aws elbv2 create-load-balancer \
    --region $REGION \
    --name $ALB_NAME \
    --subnets $SUBNET_IDS \
    --security-groups $SG_ID \
    --scheme internet-facing \
    --type application \
    --ip-address-type ipv4 \
    --query 'LoadBalancers[0].LoadBalancerArn' \
    --output text 2>/dev/null || \
    aws elbv2 describe-load-balancers \
        --region $REGION \
        --names $ALB_NAME \
        --query 'LoadBalancers[0].LoadBalancerArn' \
        --output text)

echo -e "${GREEN}✓ ALB ARN: $ALB_ARN${NC}"

# Get ALB DNS name
ALB_DNS=$(aws elbv2 describe-load-balancers \
    --region $REGION \
    --load-balancer-arns $ALB_ARN \
    --query 'LoadBalancers[0].DNSName' \
    --output text)

echo -e "${GREEN}✓ ALB DNS: $ALB_DNS${NC}"

echo -e "\n${YELLOW}Step 7: Configuring ALB Attributes${NC}"
aws elbv2 modify-load-balancer-attributes \
    --region $REGION \
    --load-balancer-arn $ALB_ARN \
    --attributes \
        Key=idle_timeout.timeout_seconds,Value=300 \
        Key=routing.http2.enabled,Value=true

echo -e "${GREEN}✓ ALB attributes configured${NC}"

echo -e "\n${YELLOW}Step 8: Creating Listener${NC}"
LISTENER_ARN=$(aws elbv2 create-listener \
    --region $REGION \
    --load-balancer-arn $ALB_ARN \
    --protocol HTTP \
    --port 80 \
    --default-actions Type=forward,TargetGroupArn=$TG_ARN \
    --query 'Listeners[0].ListenerArn' \
    --output text 2>/dev/null || \
    aws elbv2 describe-listeners \
        --region $REGION \
        --load-balancer-arn $ALB_ARN \
        --query 'Listeners[0].ListenerArn' \
        --output text)

echo -e "${GREEN}✓ Listener created${NC}"

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}✅ Load Balancer Setup Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "📋 Configuration Summary:"
echo "  ALB DNS: $ALB_DNS"
echo "  Target Group: $TARGET_GROUP_NAME"
echo "  Health Check: $HEALTH_CHECK_PATH"
echo "  Sticky Sessions: Enabled (24h)"
echo ""
echo "🔗 Access your load balancer:"
echo "  http://$ALB_DNS/health"
echo ""
echo "📝 Next Steps:"
echo "  1. Launch 4 EC2 instances for servers"
echo "  2. Run ./deploy-servers.sh to register targets"
echo "  3. Deploy your Spring Boot application"
echo ""
echo "💾 Saving configuration..."

# Save config
cat > alb-config.json <<EOF
{
  "region": "$REGION",
  "vpc_id": "$VPC_ID",
  "alb_arn": "$ALB_ARN",
  "alb_dns": "$ALB_DNS",
  "target_group_arn": "$TG_ARN",
  "security_group_id": "$SG_ID",
  "listener_arn": "$LISTENER_ARN"
}
EOF

echo -e "${GREEN}✓ Configuration saved to alb-config.json${NC}"