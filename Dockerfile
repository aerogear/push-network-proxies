# Use latest jboss/base-jdk:8 image as the base
FROM jboss/base-jdk:8

COPY impl/target/proxy-1.0.0-alpha.1-SNAPSHOT.jar /proxy.jar

EXPOSE 16002 16003

ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /proxy.jar apnsProxy" ]
