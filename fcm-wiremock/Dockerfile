FROM openjdk:8-jre

ENV WIREMOCK_VERSION 2.6.0
ENV PORT 3000

# Prepare environment
RUN mkdir wiremock
WORKDIR wiremock
ADD mappings mappings

# Get sources
RUN wget https://repo1.maven.org/maven2/com/github/tomakehurst/wiremock-standalone/${WIREMOCK_VERSION}/wiremock-standalone-${WIREMOCK_VERSION}.jar \
    -O wiremock-standalone.jar;

# FCM Proxy port
EXPOSE ${PORT}

CMD java -jar wiremock-standalone.jar --port ${PORT}