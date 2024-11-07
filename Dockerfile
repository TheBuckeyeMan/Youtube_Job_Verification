FROM public.ecr.aws/lambda/java:21
COPY target/lambdatemplate.jar ${LAMBDA_TASK_ROOT}/lib/
COPY src/main/resources ${LAMBDA_TASK_ROOT}/
CMD [ "com.example.lambdatemplate.api.Handler.LambdaHandler::handleRequest"]