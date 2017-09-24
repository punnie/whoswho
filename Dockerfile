FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/whoswho.jar /whoswho/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/whoswho/app.jar"]
