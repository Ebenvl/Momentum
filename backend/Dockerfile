# Builds the backend with the full Java 25 kit, then runs only the finished
# program on a smaller runtime. Passwords come from the hosting service's
# environment settings, never from a file.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY . .
RUN chmod +x gradlew && ./gradlew installDist --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /src/build/install/momentum-backend /app

# Runs as a normal user instead of root, so a break-in can't take over the container.
RUN useradd --system --no-create-home momentum
USER momentum

# Keeps Java inside the free plan's 512 MB of memory.
ENV JAVA_OPTS="-Xmx300m"

EXPOSE 7070
CMD ["bin/momentum-backend"]
