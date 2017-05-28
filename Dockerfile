FROM openjdk:8-jdk

COPY build/libs/*.jar /mirrorgate/
WORKDIR /mirrorgate

ENV SPRING_PROFILES_ACTIVE=scheduled

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar $(ls *.jar)" ]
