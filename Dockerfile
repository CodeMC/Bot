FROM eclipse-temurin:21

# Install Git
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

RUN mkdir /CodeMC-Bot
WORKDIR /CodeMC-Bot

ADD ./build/libs/CodeMC-Bot.jar .

ENTRYPOINT ["java", "-jar", "CodeMC-Bot.jar"]