FROM openjdk:8
COPY cloud.jar /tmp/main.jar
ENV JAVA_OPTION "-Xms512m -Xmx512m"
WORKDIR /tmp
CMD ["java", "-jar", "main.jar"]