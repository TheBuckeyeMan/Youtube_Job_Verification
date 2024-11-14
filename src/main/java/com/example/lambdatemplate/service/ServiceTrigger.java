package com.example.lambdatemplate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

@Component
public class ServiceTrigger {
    private static final Logger log = LoggerFactory.getLogger(ServiceTrigger.class);
    private final LambdaClient lambdaClient;

    @Value("${next.lambda.lambda1arn}")
    private String lambda1arn;

    @Value("${next.lambda.lambda2arn}")
    private String lambda2arn;



    public ServiceTrigger(LambdaClient lambdaClient) {
        this.lambdaClient = lambdaClient;
        //add constructor to send error email if lambda arn is unable to be found
    }

    public void triggerNextLambda(String nextServiceName){
        log.info("The value of nextServiceName is: " + nextServiceName);
        log.info("The value of lambda1arn is: " + lambda1arn);
        log.info("The value of lambda2arn is: " + lambda2arn);
        log.info("The value of nextServiceName before lambda invoke is: " + nextServiceName);
        String functionArn = getFunctionArnForService(nextServiceName);
        log.info("The Function ARN Found was:" + functionArn);
        if (functionArn != null) {
            invokeLambda(functionArn);
        } else {
            log.warn("No ARN Found for service: " + nextServiceName);
            //send email that says lambda arn is unable to be found and need supdated
        }
    }

    private String getFunctionArnForService(String serviceName) {
        log.info("The value of serviceName before checking each case to assign an arn is: " + serviceName);
        switch (serviceName){
            case "youtube-service-1,":
                return lambda1arn;
            case "youtube-service-2,":
                return lambda2arn;
            default:
                log.warn("Service not recognized:" + serviceName);
                return null;
        }
    }

    public void invokeLambda(String functionArn){
        log.info("Attempting to trigger lambda with the ARN of: " + functionArn);
        try{
            InvokeRequest request = InvokeRequest.builder()
                .functionName(functionArn)
                .build();
            InvokeResponse response = lambdaClient.invoke(request);
            log.info("Triggered lambda function: " + functionArn + " with status code: " + response.statusCode());

            if (response.statusCode() != 200){
                log.error("Failed to trigger lambda function: " + functionArn + " with status code: " + response.statusCode());
            }
        } catch (LambdaException e){
            log.error("Failed to invoke Lambda function: " + functionArn, e);
        }
    }
}
