# Use latest jboss/base-jdk:8 image as the base
FROM jboss/base-jdk:8

USER root
# Clean the metadata
RUN yum install -y unzip wget && yum -q clean all

USER jboss

RUN wget -O proxy.jar https://s3.eu-central-1.amazonaws.com/ups-logging/apns-mock.jar
RUN pwd 
#COPY ./proxy.jar /proxy.jar

EXPOSE 16002 16003

ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /opt/jboss/proxy.jar apnsProxy --apnsMockGatewayHost 0.0.0.0" ]
