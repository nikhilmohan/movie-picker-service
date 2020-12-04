FROM openjdk:12.0.2

EXPOSE 9020

ADD ./target/*.jar app.jar

ENTRYPOINT ["java","-jar","/app.jar"]
