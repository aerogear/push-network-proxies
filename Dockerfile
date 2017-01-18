# Use latest jboss/base-jdk:8 image as the base
FROM jboss/base-jdk:8

USER root
# Clean the metadata
RUN yum install -y unzip wget && yum -q clean all

USER jboss

RUN wget -O proxy.jar https://repository.jboss.org/nexus/content/repositories/snapshots/org/jboss/aerogear/proxy/1.0.0.Alpha1-SNAPSHOT/proxy-1.0.0.Alpha1-20170117.164727-1.jar
RUN pwd 
#COPY ./proxy.jar /proxy.jar

EXPOSE 16002 16003

ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /opt/jboss/proxy.jar apnsProxy --apnsMockFeedbackHost 0.0.0.0 --apnsMockGatewayHost 0.0.0.0" ]
