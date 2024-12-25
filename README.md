# yeet.stream

## EKS Deployment and Kafka Integration

This README provides instructions on deploying a service to an Amazon EKS (Elastic Kubernetes Service) cluster. The service interacts with Kafka and uses AWS ECR (Elastic Container Registry) for Docker image storage.

## Prerequisites

Ensure you have the following tools installed and configured:

- **AWS CLI** (with proper IAM permissions)
- **Docker** (with `docker buildx` support for multi-platform builds)
- **kubectl** (configured to access your EKS cluster)

## Step-by-Step Instructions

### 1. Configure AWS ECR and Docker

##### **Option 1: Manually Build and Push the Docker Image**

1. **Build the Docker Image for Linux/AMD64**

   To build the Docker image with compatibility across multiple platforms (such as AMD64 architecture), use the following `docker buildx` command:

   ```bash
   docker buildx build -t <aws_account_id>.dkr.ecr.<region>.amazonaws.com/<repository>:<version> --platform linux/amd64 .
   ```

    - Replace `<aws_account_id>` with your AWS account ID.
    - Replace `<region>` with your desired AWS region (e.g., `us-west-2`).

   This will build the Docker image and tag it with the URL of your ECR repository.

2. **Authenticate Docker to AWS ECR**

   Before pushing the image to ECR, authenticate Docker to your AWS account's ECR registry using the following command:

   ```bash
   aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <aws_account_id>.dkr.ecr.<region>.amazonaws.com
   ```

   This command retrieves a login password for Docker from AWS and pipes it into the `docker login` command to authenticate Docker with your ECR repository.

3. **Push the Docker Image to ECR**

   Once you’ve successfully built and authenticated, push the image to ECR using:

   ```bash
   docker push <aws_account_id>.dkr.ecr.<region>.amazonaws.com/<repository>:<version>
   ```

   This command uploads your Docker image to the specified ECR repository.

##### **Option 2: Use the `docker.build.push.sh` Script**

For a more automated approach, you can use the provided `docker.build.push.sh` script to handle both the build and push processes in one step.

1. **Download and configure the `docker.build.push.sh` script**  
   Ensure the script is configured to use your AWS account ID, region, and repository. You can modify the script or pass these values as environment variables.

2. **Run the script**  
   Execute the following command in your terminal to automatically build and push the Docker image:

   ```bash
   AWS_ACCOUNT=123456789012 AWS_REGION=us-west-2 AWS_REPOSITORY_NAME=prototype.tinstafl.io/v1/yeet.stream ./docker.build.push.sh
   ```

   This script will:
    - Authenticate Docker to AWS ECR.
    - Build the Docker image for the `linux/amd64` platform.
    - Push the image to the specified ECR repository.

   Make sure the script has executable permissions. If not, run:

   ```bash
   chmod +x docker.build.push.sh
   ```

   This method simplifies the process and ensures consistency across different environments.

### 2. Configure Kubernetes Deployment Using Script Inputs

To automate the creation of your Kubernetes deployment configuration, you'll need to retrieve a few essential values from your environment. Once you have these values, use them as inputs when running the script. The script will generate the `deployment.eks.yaml` file for you.

#### 2.1 Define Required Environment Variables

Before running the script, you'll need to gather and set the following environment variables:

- **`AWS_ACCOUNT`**: Your AWS account ID.
- **`AWS_REGION`**: The AWS region where your ECR repository and Kafka cluster are hosted.
- **`AWS_REPOSITORY_NAME`**: The name of the ECR repository where your Docker image is stored.
- **`NAMESPACE_SERVICE_ACCOUNT`**: The service account with the appropriate permissions to access Kafka (e.g., `<synthesizer-name>-<id>-<druid-release>-yeet-msk-sa`).
- **`WIKIMEDIA_API_USER_AGENT`**: The custom user-agent string for your service (e.g., `tinstafl/eks/msk-ingestion <you@email.com>`).
- **`KAFKA_URL`**: The Kafka bootstrap broker URL retrieved from your Kafka cluster (e.g., `boot-00000000.c2.kafka-serverless.us-west-2.amazonaws.com:9098`).

These values should either be exported as environment variables or passed directly when running the script. Here’s how you can retrieve each one:

#### 2.2 Retrieve Kafka Bootstrap Broker

To interact with your Kafka service, you need the Kafka bootstrap broker from your Kafka cluster. Run the following AWS CLI command:

```bash
aws kafka get-bootstrap-brokers --cluster-arn <arn>
```

Replace `<arn>` with the ARN of your Kafka cluster. This will return a list of Kafka broker endpoints. Copy one of these endpoints (e.g., `boot-00000000.c2.kafka-serverless.us-west-2.amazonaws.com:9098`).

Save this value as it will be used for the `KAFKA_URL` environment variable.

#### 2.3 Identify the Service Account for Kafka Access

Next, identify the service account that has the appropriate permissions to access Kafka. List the service accounts in the `api` namespace using the following command:

```bash
kubectl get sa -n api
```

Look for a service account with a name similar to:

```
<synthesizer-name>-<id>-<druid-release>-yeet-msk-sa
```

Once you've identified the correct service account, copy its name and set it as the `NAMESPACE_SERVICE_ACCOUNT` variable.

#### 2.4 Set the Wikimedia API User-Agent

To comply with Wikimedia's guidelines and avoid request blocking, you should set a custom user-agent string. The user-agent string should identify your service and include your email address. Example:

```bash
WIKIMEDIA_API_USER_AGENT="tinstafl/eks/msk-ingestion <you@email.com>"
```

### Summary of Environment Variables

When using the script, you only need to update the **dynamic fields** that are specific to your setup. These include:

- **`AWS_ACCOUNT`**: Your AWS account ID.
- **`AWS_REGION`**: The AWS region where your resources are located.
- **`AWS_REPOSITORY_NAME`**: The name of your ECR repository.
- **`NAMESPACE_SERVICE_ACCOUNT`**: The service account with the required permissions to access Kafka.
- **`WIKIMEDIA_API_USER_AGENT`**: A custom user-agent string for Wikimedia compliance.
- **`KAFKA_URL`**: The Kafka bootstrap broker URL retrieved from your Kafka cluster.

These are the variables you must update for your specific setup.

Other Kafka-related environment variables (the ones starting with **`KAFKA_`**) should generally **remain the same** unless you have specific requirements or are familiar with the Kafka configuration. These settings are typically pre-configured for use with AWS MSK and do not need to be changed unless you know exactly what you are doing:

- **`KAFKA_SECURITY_PROTOCOL`**
- **`KAFKA_SASL_MECHANISM`**
- **`KAFKA_JAAS_CONFIG`**
- **`KAFKA_SASL_CALLBACK_HANDLER`**

Here’s a summary of the environment variables:

| Variable                          | Description                                                                                                                                                                                    |
|-----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`AWS_ACCOUNT`**                 | (Required) Your AWS account ID.                                                                                                                                                                |
| **`AWS_REGION`**                  | (Required) AWS region where your resources are located (e.g., `us-west-2`).                                                                                                                    |
| **`AWS_REPOSITORY_NAME`**         | (Required) The name of your ECR repository (e.g., `prototype.tinstafl.io/v1/yeet.stream`).                                                                                                     |
| **`NAMESPACE_SERVICE_ACCOUNT`**   | (Required) The service account with permissions to access Kafka (e.g., `<synthesizer-name>-<id>-<druid-release>-yeet-msk-sa`).                                                                 |
| **`WIKIMEDIA_API_USER_AGENT`**    | (Required) Custom user-agent string for Wikimedia compliance (e.g., `tinstafl/eks/msk-ingestion <you@email.com>`).                                                                             |
| **`KAFKA_URL`**                   | (Required) Kafka bootstrap broker URL (e.g., `boot-00000000.c2.kafka-serverless.us-west-2.amazonaws.com:9098`).                                                                                |
| **`KAFKA_SECURITY_PROTOCOL`**     | (Optional) The security protocol to use for Kafka (e.g., `SASL_SSL`). Default: `SASL_SSL`.                                                                                                     |
| **`KAFKA_SASL_MECHANISM`**        | (Optional) The SASL mechanism for authentication (e.g., `AWS_MSK_IAM`). Default: `AWS_MSK_IAM`.                                                                                                |
| **`KAFKA_JAAS_CONFIG`**           | (Optional) The JAAS configuration string (e.g., `software.amazon.msk.auth.iam.IAMLoginModule required;`). Default: `software.amazon.msk.auth.iam.IAMLoginModule required;`.                    |
| **`KAFKA_SASL_CALLBACK_HANDLER`** | (Optional) The SASL callback handler for IAM authentication (e.g., `software.amazon.msk.auth.iam.IAMClientCallbackHandler`). Default: `software.amazon.msk.auth.iam.IAMClientCallbackHandler`. |

### Key Notes

- **Dynamic Fields**:
    - You must update **`AWS_ACCOUNT`**, **`AWS_REGION`**, **`AWS_REPOSITORY_NAME`**, **`NAMESPACE_SERVICE_ACCOUNT`**, **`WIKIMEDIA_API_USER_AGENT`**, and **`KAFKA_URL`** to match your environment.

- **Kafka-Specific Fields**:
    - **`KAFKA_SECURITY_PROTOCOL`**, **`KAFKA_SASL_MECHANISM`**, **`KAFKA_JAAS_CONFIG`**, and **`KAFKA_SASL_CALLBACK_HANDLER`** should be left as is unless you need to make specific changes for a different Kafka setup or have special security requirements. These are pre-configured for AWS MSK and are typically used in most default scenarios.

### 3. Deploy the Application to EKS

With all values updated in the `deployment.eks.yaml` file, you're ready to deploy the service to your EKS cluster.

#### 3.1 Update `kubeconfig` to Access EKS

Make sure your `kubectl` configuration is updated to access the correct EKS cluster.

```bash
aws eks update-kubeconfig --region <region> --name <cluster>
```

Replace `<region>` with your AWS region and `<cluster>` with the name of your EKS cluster.

#### 3.2 Apply the Kubernetes Deployment

Now you can apply the updated deployment file to your EKS cluster:

```bash
kubectl apply -f deployment.eks.yaml
```

This will deploy your service with all the correct configurations for connecting to Kafka and interacting with Wikimedia.

### 4. Access the Application

Once your deployment is live, you can access the application and monitor its logs.

#### 4.1 Port Forward to Access the Service

To access the service locally, use port forwarding:

```bash
kubectl port-forward -n api service/yeet-stream 8080:8080
```

This forwards the service's port 8080 to your local machine, allowing you to interact with the application via `http://localhost:8080`.

#### 4.2 Monitor Logs

You can view the logs of the `yeet-stream` deployment to monitor its activity and debug any issues:

```bash
kubectl logs deployment.apps/yeet-stream -n api --follow
```

This command streams the logs in real-time, allowing you to monitor the application’s output.

#### 4.3 Health & Status Endpoints

You can monitor the health and gather information about the application using the following actuator endpoints:

- **Health Check**  
  To check the health status of the application:

  ```shell
  curl -v localhost:8080/actuator/health
  ```

  A healthy response would look like this:

  ```json
  {
    "status": "UP"
  }
  ```

- **Application Info**  
  To retrieve metadata about the application:

  ```shell
  curl -v localhost:8080/actuator/info
  ```

  This will provide additional information like build version, environment properties, etc.

#### 4.4 Interacting with the Application

To emit a specific set of event streams, run the following `curl` command:

```shell
curl -v -H "Content-Type:application/json" \
  "localhost:8080/v1/wikimedia/emit/streams?total=25" \
  -d '{
        "streams": [
          "eventgate-main.test.event", 
          "mediawiki.page-create", 
          "mediawiki.page-delete", 
          "mediawiki.page-links-change", 
          "mediawiki.page-move", 
          "mediawiki.page-properties-change", 
          "mediawiki.page-undelete", 
          "mediawiki.page_change.v1", 
          "mediawiki.recentchange", 
          "mediawiki.revision-create", 
          "mediawiki.revision-tags-change", 
          "mediawiki.revision-visibility-change", 
          "page-create", 
          "page-delete", 
          "page-links-change", 
          "page-move", 
          "page-properties-change", 
          "page-undelete", 
          "recentchange", 
          "revision-create", 
          "test"
        ]
      }'
```

This will trigger the emission of up to 25 events from the specified streams and send them to the MSK topic `wikimedia.streams`

### 5. Integrate Apache Druid with Managed Streaming Kafka (MSK)

Follow the steps below to integrate Apache Druid with AWS Managed Streaming for Apache Kafka (MSK) for real-time data ingestion.

#### 5.1 Port Forward Apache Druid Router

To access the Druid router locally, use the following `kubectl` command to set up port forwarding. This will forward the Druid router port to your local machine, allowing you to access the Druid UI.

```shell
kubectl port-forward service/<synthesizer-name>-<id>-<druid-release>-yeet-router 9999:9088 -n druid
```

This command forwards port `9088` on the Druid router service to port `9999` on your local machine.

#### 5.2 Open Druid UI

Once port forwarding is established, open the Druid UI in your browser by navigating to:

```
https://localhost:9999
```

#### 5.3 Login with Admin Credentials

To log in to the Druid UI:

1. Use the admin credentials.
2. **Password**: Retrieve the password from AWS Secrets Manager.

    - **Secret Name**: `admin/<synthesizer-name>-<id>-<druid-release>-yeet-admin`
    - Once you retrieve the secret, use the `password` field to log in.

#### 5.4 Load Data via Streaming Ingestion

After logging in:

1. In the Druid UI, navigate to **Load Data**.
2. Select the **Streaming** option.

#### 5.5 Configure Connection to MSK

Configure the connection to your AWS Managed Streaming Kafka (MSK) cluster by adding the following connection settings.

1. In the **Streaming Ingestion** setup, provide the following JSON configuration for the Kafka connection:

```json
{
    "bootstrap.servers": "boot-00000000.c2.kafka-serverless.us-west-2.amazonaws.com:9098",
    "security.protocol": "SASL_SSL",
    "sasl.mechanism": "AWS_MSK_IAM",
    "sasl.jaas.config": "software.amazon.msk.auth.iam.IAMLoginModule required;",
    "sasl.client.callback.handler.class": "software.amazon.msk.auth.iam.IAMClientCallbackHandler",
    "aws.region": "us-west-2",
    "aws.credentials.provider": "com.amazonaws.auth.DefaultAWSCredentialsProviderChain"
}
```

- **`bootstrap.servers`**: The address of your MSK cluster's bootstrap servers.
- **`security.protocol`**: Set to `SASL_SSL` for secure communication with MSK.
- **`sasl.mechanism`**: Use `AWS_MSK_IAM` for IAM-based authentication with MSK.
- **`sasl.jaas.config`**: This configures the IAM login module for authentication.
- **`sasl.client.callback.handler.class`**: 
- **`aws.region`**: The AWS region where your MSK cluster is hosted (e.g., `us-west-2`).
- **`aws.credentials.provider`**: Specifies the AWS credentials provider chain, typically fetching credentials from environment variables, EC2 instance roles, or AWS configuration files.

#### 5.6 Complete the Default Setup

- Once the connection is configured, you can complete the default setup for streaming ingestion.
- You may need to select or configure a Kafka topic to stream data from (e.g., `wikimedia.streams`).
- After finishing the setup, Druid will begin ingesting data from your MSK cluster in real-time.

#### 5.7 Query Data in Druid

Once your streaming ingestion setup is complete and data is being ingested, you can start querying the data:

1. Go to the **SQL Query** tab in the Druid UI.
2. Run a query on the `wikimedia.streams` data source to view the ingested data:

```sql
SELECT * FROM wikimedia_streams LIMIT 10
```

This will return the first 10 rows of data from the `wikimedia.streams` topic that was ingested via MSK.

### Additional Resources

- [Druid Documentation: Streaming Ingestion](https://druid.apache.org/docs/latest/ingestion/streaming.html)
- [AWS MSK Documentation](https://docs.aws.amazon.com/msk/latest/developerguide/what-is-msk.html)


### Troubleshooting

- **Port Forwarding Issues**: If the `kubectl port-forward` command does not work, ensure that you have the correct permissions and the Kubernetes cluster is up and running. You can check your current port-forward status with:

  ```shell
  kubectl get svc -n druid
  ```

- **Authentication Issues**: If you encounter issues related to IAM authentication, verify that the AWS credentials being used have appropriate permissions to access the MSK cluster. The IAM role should have at least the following permissions:
    - `kafka:DescribeCluster`
    - `kafka:ListTopics`
    - `kafka:DescribeTopic`
    - `kafka:Consume`

- **Druid UI Unavailable**: If you cannot access the Druid UI at `https://localhost:9999`, verify that the port-forwarding command was executed correctly and that no firewall or network security settings are blocking access.
