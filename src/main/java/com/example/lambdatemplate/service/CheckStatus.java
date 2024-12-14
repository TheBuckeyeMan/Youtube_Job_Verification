package com.example.lambdatemplate.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.S3Client;
import com.example.lambdatemplate.service.EmailOnError;
import org.springframework.beans.factory.annotation.Value;

@Component
public class CheckStatus {
    private static final Logger log = LoggerFactory.getLogger(CheckStatus.class);
    private final S3Client s3Client;
    private final EmailOnError emailOnError;
    private final ServiceTrigger serviceTrigger;
    
    @Value("${variables.emailto}")
    String emailRecipiant;

    public CheckStatus(S3Client s3Client, EmailOnError emailOnError, ServiceTrigger serviceTrigger){
        this.s3Client = s3Client;
        this.emailOnError = emailOnError;
        this.serviceTrigger = serviceTrigger;
    }
    //Download Existing S3 Logging File
    public String downloadLogFilesFromS3(String bucketName, String logFileKey) throws IOException{
        try{
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(logFileKey)
                .build();
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);

            //Convert response object to string for viewing -> may not be required
            List<String> fileContent = new BufferedReader(new InputStreamReader(s3Object))
                .lines()
                .collect(Collectors.toList());

            if (!fileContent.isEmpty()){
                log.info("This is the content of the donwloaded S3 Bucket" + fileContent); //Print the file context
                String status = fileContent.get(fileContent.size() - 1);
                String serviceName = getServiceName(status);
                log.info("The Status is: " + status);
                if (status.contains("Success")){
                    log.info("Prior Application: " + serviceName + " Succeeded");
                    //Add in code here to trigger the next lambda function
                    serviceTrigger.triggerNextLambda(serviceName);
                } else {
                    log.info("Prior Applicaiton: " + serviceName + " Failed");
                    File LogFile = createTempFile(fileContent);
                    emailOnError.sendErrorEmail(
                        emailRecipiant,//here we are trying to figure out whats ging on
                        "Appllication Error At: " + serviceName,
                        "The application: " + serviceName +" has failed and required investigation. All downstreem services have been temporatily deactivated untill the issue has been fixed",
                        LogFile);
                }
                return status;
            } else {
                log.warn("No Records were found in the log file");
                File LogFile = createTempFile(fileContent);
                System.out.println("RECIPIANT CHECK: " + System.getenv("EMAIL_TO")); //check for the value
                    emailOnError.sendErrorEmail(
                        emailRecipiant, //here we are trying to figure out whats ging on
                        "Logging Error",
                        "The logging error can be caused by a few reason. 1. The logging bucket does not exist as specified in the Email service. 2. The logging buckets contents are empty and do not include any records.",
                        LogFile);
                return "No Records Found";
            }
            
        } catch (S3Exception e) {
            log.error("Log file not found, Defaulting to error out. Services not kicked off, emails not sent", e);
            return ""; // Return empty if log file not found
        }
    }
    private File createTempFile(List<String> content) throws IOException{
        File tempFile = File.createTempFile("Logs", ".csv");
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))){
            for (String line : content){
                writer.write(line);
                writer.newLine();
            }
        }
        return tempFile;
    }
    private String getServiceName(String status){
        if (status.contains("On: ")){
            return status.substring(status.indexOf("On: ") + 4).trim();
        }
        return "Service Unidentified";
    }
}