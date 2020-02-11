FROM openjdk:8-alpine

COPY target/uberjar/golf-handicap.jar /golf-handicap/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/golf-handicap/app.jar"]
