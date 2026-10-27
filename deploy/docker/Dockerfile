# syntax=docker/dockerfile:1
# Builds one service image from the Maven reactor.
#   docker build -f deploy/docker/Dockerfile --build-arg SERVICE=shop-service -t partradar/shop-service .

FROM eclipse-temurin:25-jdk AS build
ARG SERVICE
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY libs libs
COPY services services
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q package -pl services/${SERVICE} -am -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true \
    && java -Djarmode=tools -jar services/${SERVICE}/target/${SERVICE}-*.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre AS runtime
RUN useradd --system --uid 1001 partradar
WORKDIR /app
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
USER partradar
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
