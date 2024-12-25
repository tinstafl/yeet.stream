#!/bin/bash

# ./deployment.eks.sh
# AWS_ACCOUNT=123456789012 AWS_REGION=us-west-2 AWS_REPOSITORY_NAME=prototype.tinstafl.io/v1/yeet.stream NAMESPACE_SERVICE_ACCOUNT=default-id-release-yeet-msk-sa KAFKA_URL=boot-00000000.c2.kafka-serverless.us-west-2.amazonaws.com:9098 ./deployment.eks.sh

COLOR_RESET="\033[0m"
COLOR_GREEN="\033[0;32m"
COLOR_RED="\033[0;31m"
COLOR_BLUE="\033[0;34m"

AWS_ACCOUNT="${AWS_ACCOUNT:-000000000000}"
AWS_REGION="${AWS_REGION:-us-west-2}"
AWS_REPOSITORY_NAME="${AWS_REPOSITORY_NAME:-prototype.tinstafl.io/v1/yeet.stream}"
NAMESPACE_SERVICE_ACCOUNT="${NAMESPACE_SERVICE_ACCOUNT:-default-id-release-yeet-msk-sa}"
WIKIMEDIA_API_USER_AGENT="${WIKIMEDIA_API_USER_AGENT:-tinstafl/eks/msk-ingestion <hi@tinstafl.io>}"
KAFKA_URL="${KAFKA_URL:-boot-0000000.c2.kafka-serverless.us-west-2.amazonaws.com:9098}"
KAFKA_SECURITY_PROTOCOL="${KAFKA_SECURITY_PROTOCOL:-SASL_SSL}"
KAFKA_SASL_MECHANISM="${KAFKA_SASL_MECHANISM:-AWS_MSK_IAM}"
KAFKA_JAAS_CONFIG="${KAFKA_JAAS_CONFIG:-software.amazon.msk.auth.iam.IAMLoginModule required;}"
KAFKA_SASL_CALLBACK_HANDLER="${KAFKA_SASL_CALLBACK_HANDLER:-software.amazon.msk.auth.iam.IAMClientCallbackHandler}"

print_info() {
  echo -e "${COLOR_BLUE}[INFO] $1${COLOR_RESET}"
}

print_success() {
  echo -e "${COLOR_GREEN}[SUCCESS] $1${COLOR_RESET}"
}

print_error() {
  echo -e "${COLOR_RED}[ERROR] $1${COLOR_RESET}"
}

YAML_FILE="deployment.eks.yaml"

print_info "Generating Kubernetes YAML file: ${YAML_FILE}..."

cat > "$YAML_FILE" <<EOL
apiVersion: apps/v1
kind: Deployment
metadata:
  name: yeet-stream
  namespace: api
  labels:
    app: yeet-stream
spec:
  replicas: 1
  selector:
    matchLabels:
      app: yeet-stream
  template:
    metadata:
      labels:
        app: yeet-stream
    spec:
      serviceAccountName: ${NAMESPACE_SERVICE_ACCOUNT}
      restartPolicy: Always
      containers:
        - name: yeet-stream
          image: ${AWS_ACCOUNT}.dkr.ecr.${AWS_REGION}.amazonaws.com/${AWS_REPOSITORY_NAME}:v1
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          env:
            - name: INFRA
              value: aws-eks/production
            - name: AWS_REGION
              value: ${AWS_REGION}
            - name: WIKIMEDIA_API_USER_AGENT
              value: ${WIKIMEDIA_API_USER_AGENT}
            - name: OTEL_SERVICE_NAME
              value: "yeet.stream"
            - name: OTEL_EXPORTER_OTLP_TRACES_ENDPOINT
              value: "http://k8s-monitoring-grafana-agent.monitoring.svc.cluster.local:4318/v1/traces"
            - name: OTEL_EXPORTER_OTLP_METRICS_ENDPOINT
              value: "http://k8s-monitoring-grafana-agent.monitoring.svc.cluster.local:4318/v1/metrics"
            - name: OTEL_EXPORTER_OTLP_LOGS_ENDPOINT
              value: "http://k8s-monitoring-grafana-agent.monitoring.svc.cluster.local:4318/v1/logs"
            - name: OTEL_LOGS_EXPORTER
              value: "otlp"
            - name: KAFKA_URL
              value: ${KAFKA_URL}
            - name: KAFKA_SECURITY_PROTOCOL
              value: ${KAFKA_SECURITY_PROTOCOL}
            - name: KAFKA_SASL_MECHANISM
              value: ${KAFKA_SASL_MECHANISM}
            - name: KAFKA_JAAS_CONFIG
              value: ${KAFKA_JAAS_CONFIG}
            - name: KAFKA_SASL_CALLBACK_HANDLER
              value: ${KAFKA_SASL_CALLBACK_HANDLER}
            - name: KAFKA_TOPIC_PARTITIONS
              value: "3"
            - name: KAFKA_TOPIC_REPLICAS
              value: "2"
---
apiVersion: v1
kind: Service
metadata:
  labels:
    app: yeet-stream
  name: yeet-stream
  namespace: api
spec:
  ports:
    - name: 'http'
      port: 8080
      targetPort: 8080
  selector:
    app: yeet-stream
  type: ClusterIP
EOL

if [ -f "$YAML_FILE" ]; then
  print_success "Kubernetes YAML file '${YAML_FILE}' generated successfully."
else
  print_error "Failed to generate Kubernetes YAML file. Exiting."
  exit 1
fi
