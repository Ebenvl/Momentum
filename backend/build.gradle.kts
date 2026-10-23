// Build file for the LearnTech Momentum backend.
// Lists the libraries the backend uses and how to run it.

plugins {
    // Lets Gradle build and run a normal Java program with a main method.
    application
}

group = "com.momentum"
version = "0.1.0"

java {
    // Build with Java 25, the current long term support version,
    // regardless of which other Java versions are installed.
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    // Where Gradle downloads the libraries below from.
    mavenCentral()
}

dependencies {
    // Javalin: the small web framework that receives requests from the app.
    // The bundle also includes Jackson, which converts Java objects to JSON
    // and back, and Logback, which prints messages to the console.
    implementation("io.javalin:javalin-bundle:7.2.3")

    // The driver that lets Java talk to MySQL.
    implementation("com.mysql:mysql-connector-j:8.4.0")

    // Keeps a small pool of open database connections, so each request
    // does not have to open a brand new connection to MySQL.
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Hashes passwords so they are never stored as plain text.
    implementation("org.mindrot:jbcrypt:0.4")

    // Creates and checks JWT login tokens, so the app can prove who is
    // logged in without sending the password with every request.
    implementation("com.auth0:java-jwt:4.5.2")

    // Sends emails: two-step login codes and password reset codes.
    implementation("org.eclipse.angus:angus-mail:2.0.3")
}

application {
    // The class with the main method that starts the server.
    mainClass.set("com.momentum.backend.App")
}
