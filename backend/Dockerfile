# Builds and runs the Momentum backend on a hosting service, such as Render.
#
# Step 1 builds it with the full Java 25 kit. Step 2 copies only the finished
# program into a smaller Java 25 runtime, so the running image has no build
# tools or source code in it.
#
# No settings or passwords are copied in. The hosting service gives them as
# environment settings (DB_URL, DB_PASSWORD, JWT_SECRET and so on).

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
