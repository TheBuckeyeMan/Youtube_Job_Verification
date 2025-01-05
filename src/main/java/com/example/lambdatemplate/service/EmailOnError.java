package com.example.lambdatemplate.service;

import java.io.File;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmailOnError {
    private static final Logger log = LoggerFactory.getLogger(EmailOnError.class);
    private final JavaMailSender mailSender;

    public EmailOnError(JavaMailSender mailSender){
        this.mailSender = mailSender;
    }

    public void sendErrorEmail(String recipiant, String subject, String body, File file){
        try{
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,true);

            helper.setTo(recipiant);
            helper.setSubject(subject);
            helper.setText(body);

            if (file != null){
                helper.addAttachment("Logs.csv", file);
            }
            mailSender.send(message);
            log.info("Error email sent succesfully to {}: ", recipiant);
         } catch (MessagingException e){
                log.error("Failed to send email: ", e);
        }
    }
}