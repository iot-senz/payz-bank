FROM ubuntu:latest

MAINTAINER Eranga Bandara (erangaeb@gmail.com)

# install java and other required packages
RUN apt-get update -y
RUN apt-get install -y python-software-properties
RUN apt-get install -y software-properties-common
RUN add-apt-repository -y ppa:webupd8team/java
RUN apt-get update -y

# install java
RUN echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections
RUN apt-get install -y oracle-java7-installer
RUN rm -rf /var/lib/apt/lists/*
RUN rm -rf /var/cache/oracle-jdk7-installer

# set JAVA_HOME
ENV JAVA_HOME /usr/lib/jvm/java-7-oracle

# set service variables
ENV SWITCH_HOST dev.localhost
ENV SWITCH_PORT 7070
ENV CASSANDRA_HOST dev.localhost
ENV CASSANDRA_PORT 9090

# working directory
WORKDIR /app

# copy file
ADD target/scala-2.11/payz-bank-assembly-1.0.jar trans.jar

# logs volume
RUN mkdir logs
VOLUME ["/app/logs"]

# .keys volume
VOLUME ["/app/.keys"]

# command
ENTRYPOINT [ "java", "-jar", "/app/trans.jar" ]
