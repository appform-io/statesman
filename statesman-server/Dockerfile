FROM ubuntu:18.04

RUN apt-get clean && apt-get update && apt-get install -y --no-install-recommends software-properties-common openjdk-8-jdk ca-certificates && apt-get install -y --no-install-recommends ca-certificates-java bash curl tzdata iproute2 zip unzip wget

EXPOSE 8080
EXPOSE 8081
EXPOSE 8082
EXPOSE 8083

VOLUME /var/log/statesman-server

ADD config/docker.yml config/docker.yml
ADD target/statesman*.jar server.jar
ADD startup.sh startup.sh

CMD ./startup.sh
