# OpenWhisk Java Runtime with QSE Dependencies
FROM openwhisk/java8action:latest

# Copy QSE dependencies
COPY target/qse-openwhisk-1.0-SNAPSHOT.jar /javaAction/qse.jar

# Copy any additional resources
COPY src/main/resources/ /javaAction/resources/

# Set environment variables
ENV MINIO_ENDPOINT=http://minio:9000
ENV MINIO_ACCESS_KEY=minioadmin
ENV MINIO_SECRET_KEY=minioadmin
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379

# Add the JAR to the classpath
ENV CLASSPATH="/javaAction/qse.jar:$CLASSPATH"

# Update the runner to include our dependencies
COPY proxy /javaAction/proxy
RUN chmod +x /javaAction/proxy

CMD ["/bin/bash", "-c", "cd /javaAction && exec /javaAction/proxy"]