#!/bin/bash

# Deploy ChatFlow Servers to EC2 and Register with ALB
# Assumes you have 4 EC2 instances already running

set -e

echo "🚀 ChatFlow Server Deployment"
echo "=============================="

# Load ALB configuration
if [ ! -f "alb-config.json" ]; then
    echo "❌ Error: alb-config.json not found. Run setup-alb.sh first!"
    exit 1
fi

REGION=$(jq -r '.region' alb-config.json)
TG_ARN=$(jq -r '.target_group_arn' alb-config.json)

echo "📋 Configuration:"
echo "  Region: $REGION"
echo "  Target Group: $TG_ARN"
echo ""

# Server configuration
SERVER_PORT=8080
JAR_FILE="../server-v2/target/chatflow-server.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "⚠️  JAR file not found. Building..."
    cd ../server-v2
    mvn clean package -DskipTests
    cd ../deployment
    echo "✅ Build complete"
fi

echo ""
echo "🔍 Finding EC2 instances tagged as chatflow-server..."

# Get EC2 instance IDs (assumes instances are tagged with Name=chatflow-server*)
INSTANCE_IDS=$(aws ec2 describe-instances \
    --region $REGION \
    --filters "Name=tag:Name,Values=chatflow-server*" "Name=instance-state-name,Values=running" \
    --query 'Reservations[*].Instances[*].InstanceId' \
    --output text)

if [ -z "$INSTANCE_IDS" ]; then
    echo "❌ No running instances found with tag 'chatflow-server*'"
    echo ""
    echo "To create instances, run:"
    echo "  aws ec2 run-instances \\"
    echo "    --region $REGION \\"
    echo "    --image-id ami-xxxxxxxxx \\"
    echo "    --instance-type t3.medium \\"
    echo "    --key-name your-key \\"
    echo "    --security-group-ids sg-xxxxx \\"
    echo "    --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=chatflow-server-1}]'"
    exit 1
fi

echo "✅ Found instances: $INSTANCE_IDS"
echo ""

# Register instances with target group
echo "📝 Registering instances with target group..."

for INSTANCE_ID in $INSTANCE_IDS; do
    echo "  Registering $INSTANCE_ID..."
    aws elbv2 register-targets \
        --region $REGION \
        --target-group-arn $TG_ARN \
        --targets Id=$INSTANCE_ID,Port=$SERVER_PORT
done

echo "✅ All instances registered"
echo ""

# Deploy JAR to each instance
echo "🚀 Deploying application to servers..."

INSTANCE_IPS=$(aws ec2 describe-instances \
    --region $REGION \
    --instance-ids $INSTANCE_IDS \
    --query 'Reservations[*].Instances[*].PublicIpAddress' \
    --output text)

for IP in $INSTANCE_IPS; do
    echo ""
    echo "📦 Deploying to $IP..."

    # Copy JAR
    scp -i ~/.ssh/your-key.pem \
        -o StrictHostKeyChecking=no \
        $JAR_FILE \
        ec2-user@$IP:~/chatflow-server.jar

    # Create systemd service
    ssh -i ~/.ssh/your-key.pem \
        -o StrictHostKeyChecking=no \
        ec2-user@$IP << 'EOF'
# Install Java if not present
if ! command -v java &> /dev/null; then
    sudo yum install -y java-17-amazon-corretto
fi

# Create systemd service
sudo tee /etc/systemd/system/chatflow.service > /dev/null << 'SERVICE'
[Unit]
Description=ChatFlow Server
After=network.target

[Service]
User=ec2-user
WorkingDirectory=/home/ec2-user
ExecStart=/usr/bin/java -jar /home/ec2-user/chatflow-server.jar
SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
SERVICE

# Reload and start
sudo systemctl daemon-reload
sudo systemctl enable chatflow
sudo systemctl restart chatflow

echo "✅ Service started"
EOF

    echo "✅ Deployed to $IP"
done

echo ""
echo "⏳ Waiting for health checks..."
sleep 10

# Check target health
echo ""
echo "🏥 Target Health Status:"
aws elbv2 describe-target-health \
    --region $REGION \
    --target-group-arn $TG_ARN \
    --query 'TargetHealthDescriptions[*].[Target.Id,TargetHealth.State,TargetHealth.Description]' \
    --output table

echo ""
echo "✅ Deployment Complete!"
echo ""
echo "🔗 Test your load balancer:"
ALB_DNS=$(jq -r '.alb_dns' alb-config.json)
echo "  http://$ALB_DNS/health"
echo ""