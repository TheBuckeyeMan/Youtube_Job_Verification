package com.example.lambdatemplate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.RunTaskRequest;
import software.amazon.awssdk.services.ecs.model.RunTaskResponse;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

@Component
public class ServiceTrigger {
    private static final Logger log = LoggerFactory.getLogger(ServiceTrigger.class);
    private final LambdaClient lambdaClient;
    private final EcsClient ecsClient;

    @Value("${next.lambda.lambda1arn}")
    private String lambda1arn;

    @Value("${next.lambda.lambda2arn}")
    private String lambda2arn;

    @Value("${next.lambda.lambda3arn}")
    private String lambda3arn;

    @Value("${next.lambda.ecstaskarn}")
    private String ecstaskarn;

    @Value("${next.lambda.lambda5arn}")
    private String lambda5arn;

    @Value("${next.lambda.lambda6arn}")
    private String lambda6arn;

    @Value("${ecs.cluster.name}")
    private String ecsClusterName;

    @Value("${ecs.task.executionRoleArn}")
    private String ecsExecutionRoleArn;

    public ServiceTrigger(LambdaClient lambdaClient, EcsClient ecsClient) {
        this.lambdaClient = lambdaClient;
        this.ecsClient = ecsClient;
    }

    public void triggerNextService(String nextServiceName) {
        log.info("The value of nextServiceName is: " + nextServiceName);

        String functionArn = getFunctionArnForService(nextServiceName);
        log.info("The ARN found was: " + functionArn);

        if (functionArn != null) {
            if (functionArn.startsWith("arn:aws:ecs")) {
                log.info("Initiating ECS Task: " + functionArn);
                invokeEcsTask(functionArn);
            } else if (functionArn.equals(lambda2arn)){
                log.info("Kicking off Lambda: " + functionArn + " and " + lambda2arn);
                invokeLambda(functionArn);
                invokeLambda(lambda6arn);
            } else {
                log.info("Kicking off Lambda: " + functionArn);
                invokeLambda(functionArn);
            }
        } else {
            log.warn("No ARN found for service: " + nextServiceName);
        }
    }

    private void invokeLambda(String functionArn) {
        log.info("Attempting to invoke Lambda function: " + functionArn);
        try {
            InvokeRequest request = InvokeRequest.builder()
                .functionName(functionArn)
                .build();
            InvokeResponse response = lambdaClient.invoke(request);
            log.info("Lambda function triggered: " + functionArn + " with status code: " + response.statusCode());
        } catch (LambdaException e) {
            log.error("Failed to invoke Lambda function: " + functionArn, e);
        }
    }

    private void invokeEcsTask(String taskDefinitionArn) {
        log.info("Attempting to invoke ECS task: " + taskDefinitionArn);
        try {
            RunTaskRequest runTaskRequest = RunTaskRequest.builder()
            .cluster(ecsClusterName) // Cluster name: "youtube-cluster"
            .taskDefinition(taskDefinitionArn) // Task definition ARN
            .launchType("FARGATE")
            .networkConfiguration(builder -> builder
                .awsvpcConfiguration(vpc -> vpc
                    .subnets(
                        "subnet-0b9b1b37c75908735", 
                        "subnet-03eccd7b2757b12cd", 
                        "subnet-0f0de6e5e9ebf805d") // Subnet IDs
                    .securityGroups("sg-04d0b06614d60cdf6") // Security Group ID
                    .assignPublicIp("ENABLED"))) // Assign Public IP
            .overrides(override -> override.containerOverrides(builder -> builder
                .name("youtube-service-4") // Container Name
                .build())) // Omit `command` if not needed
            .build();

            RunTaskResponse runTaskResponse = ecsClient.runTask(runTaskRequest);
            log.info("ECS Task triggered successfully: " + runTaskResponse.tasks());
        } catch (Exception e) {
            log.error("Failed to invoke ECS task: " + taskDefinitionArn, e);
        }
    }

    private String getFunctionArnForService(String serviceName) {
        switch (serviceName) {
            case "youtube-service-1,":
                return lambda2arn;
            case "youtube-service-2,":
                return lambda3arn;
            case "youtube-service-3,":
                return ecstaskarn;
            case "youtube-service-4,":
                return lambda5arn;
            case "youtube-service-5,":
                return lambda6arn;
            case "youtube-service-6,":
                return "All Tasks Completed Successfully!";
            default:
                log.warn("Service not recognized: " + serviceName);
                return null;
        }
    }
}