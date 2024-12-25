#!/bin/bash

# ./docker.build.push.sh
# AWS_ACCOUNT=123456789012 AWS_REGION=us-east-1 AWS_REPOSITORY_NAME=prototype.tinstafl.io/v1/yeet.stream ./docker.build.push.sh

COLOR_RESET="\033[0m"
COLOR_GREEN="\033[0;32m"
COLOR_RED="\033[0;31m"
COLOR_BLUE="\033[0;34m"

AWS_ACCOUNT="${AWS_ACCOUNT:-000000000000}"
AWS_REGION="${AWS_REGION:-us-west-2}"
AWS_REPOSITORY_NAME="${AWS_REPOSITORY_NAME:-prototype.tinstafl.io/v1/yeet.stream}"

print_info() {
  echo -e "${COLOR_BLUE}[INFO] $1${COLOR_RESET}"
}

print_success() {
  echo -e "${COLOR_GREEN}[SUCCESS] $1${COLOR_RESET}"
}

print_error() {
  echo -e "${COLOR_RED}[ERROR] $1${COLOR_RESET}"
}

print_info "Checking if ECR repository exists: ${AWS_REPOSITORY_NAME}..."
if aws ecr describe-repositories --repository-names "${AWS_REPOSITORY_NAME}" --region "${AWS_REGION}" --no-cli-pager > /dev/null 2>&1; then
  print_info "ECR repository ${AWS_REPOSITORY_NAME} already exists. Skipping repository creation."
else
  print_info "ECR repository ${AWS_REPOSITORY_NAME} does not exist. Creating repository..."
  if aws ecr create-repository --repository-name "${AWS_REPOSITORY_NAME}" --region "${AWS_REGION}" --no-cli-pager; then
    print_success "ECR repository created successfully."
  else
    print_error "Failed to create ECR repository. Exiting."
    exit 1
  fi
fi

print_info "Building Docker image for repository ${AWS_REPOSITORY_NAME}..."
if docker buildx build \
  --platform linux/amd64 \
  --provenance=false \
  -t "${AWS_ACCOUNT}.dkr.ecr.${AWS_REGION}.amazonaws.com/${AWS_REPOSITORY_NAME}:v1" \
  -t "${AWS_ACCOUNT}.dkr.ecr.${AWS_REGION}.amazonaws.com/${AWS_REPOSITORY_NAME}:latest" \
  -f Dockerfile .; then
  print_success "Docker image built successfully."
else
  print_error "Docker image build failed. Exiting."
  exit 1
fi

print_info "Authenticating Docker to AWS ECR..."
if aws ecr get-login-password --region "${AWS_REGION}" | docker login --username AWS --password-stdin "${AWS_ACCOUNT}.dkr.ecr.${AWS_REGION}.amazonaws.com"; then
  print_success "Docker authentication successful."
else
  print_error "Docker authentication failed. Exiting."
  exit 1
fi

print_info "Pushing Docker images to ECR..."
if docker push "${AWS_ACCOUNT}.dkr.ecr.${AWS_REGION}.amazonaws.com/${AWS_REPOSITORY_NAME}:v1" && \
   docker push "${AWS_ACCOUNT}.dkr.ecr.${AWS_REGION}.amazonaws.com/${AWS_REPOSITORY_NAME}:latest"; then
  print_success "Docker images pushed to ECR successfully."
else
  print_error "Failed to push Docker images to ECR. Exiting."
  exit 1
fi
