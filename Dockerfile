FROM public.ecr.aws/amazoncorretto/amazoncorretto:21

COPY /build/libs/trmnl-lambda.jar ./
ENTRYPOINT [ "/usr/bin/java", "-jar", "trmnl-lambda.jar" ]
