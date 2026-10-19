package com.momentum.backend.learntech;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.momentum.backend.config.Config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

// Talks to the LearnTech API. Read only, it never changes anything
// on the platform. Only the backend uses this, the app never sees the key.
//
// If learntech.base_url in config.properties is "sample", it reads the sample
// replies from the API document in backend/sample-data instead, so the sync can
// be built and tested before the real API address is confirmed.
public class LearnTechClient {

    // Not final, because the admin can change them from the settings screen.
    // volatile: a change made by one request is seen straight away by the sync.
    private volatile String baseUrl;
    private volatile String apiKey;
    private final HttpClient http;
    private final ObjectMapper json;

    public LearnTechClient(Config config) {
        useSettings(config.get("learntech.base_url"), config.get("learntech.api_key"));

        // Gives up after 15 seconds instead of hanging if LearnTech is slow or down.
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        // Turns the API's JSON into our Api... classes.
        // SNAKE_CASE: "course_id" in the JSON fills courseId in Java.
        // FAIL_ON_UNKNOWN_PROPERTIES off: the doc says fields may be added later,
        // so new fields are ignored instead of breaking the sync.
        this.json = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // Switches to a different address and key. Used at startup (config.properties,
    // or the admin's saved settings) and when the admin saves new settings.
    public void useSettings(String url, String key) {
        // Drops a "/" at the end, so ".../v1/" + "/courses" doesn't become "v1//courses".
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.apiKey = key;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    // GET /learners: every learner with their courses (for the scheduled sync).
    public LearnersResponse getAllLearners() throws LearnTechException {
        String body = fetch("/learners", "learners.json");
        return read(body, LearnersResponse.class);
    }

    // GET /learners?idst=...: one learner (for the sync when they log in).
    // Still an array, with just one learner in it.
    public LearnersResponse getLearner(int idst) throws LearnTechException {
        String body = fetch("/learners?idst=" + idst, "learners.json");
        return read(body, LearnersResponse.class);
    }

    // GET /courses: the full course catalogue.
    public CoursesResponse getCourses() throws LearnTechException {
        String body = fetch("/courses", "courses.json");
        return read(body, CoursesResponse.class);
    }

    // Gets the raw JSON text, either from the API or from a sample file.
    private String fetch(String path, String sampleFile) throws LearnTechException {

        if (baseUrl.equals("sample")) {
            try {
                return Files.readString(Path.of("sample-data", sampleFile));
            } catch (IOException e) {
                throw new LearnTechException("could not read sample-data/" + sampleFile);
            }
        }

        // The request, with the two headers the API doc says every call needs.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("X-Api-Key", apiKey)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            // Some network errors have no message, so the error's type is used instead.
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new LearnTechException("could not reach LearnTech: " + reason);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LearnTechException("request to LearnTech was interrupted");
        }

        // 200 means OK. Anything else is an error, explained in the reply's
        // "error" part: 401 wrong key, 404 not found, and so on.
        if (response.statusCode() != 200) {
            throw new LearnTechException("LearnTech replied " + response.statusCode()
                    + ": " + errorMessage(response.body()));
        }
        return response.body();
    }

    // Turns JSON text into one of our classes, and checks "success" first,
    // like the API doc says to.
    private <T> T read(String body, Class<T> type) throws LearnTechException {
        try {
            JsonNode tree = json.readTree(body);
            if (!tree.path("success").asBoolean(false)) {
                throw new LearnTechException("LearnTech said it failed: " + errorMessage(body));
            }
            return json.treeToValue(tree, type);
        } catch (IOException e) {
            throw new LearnTechException("LearnTech sent something that isn't the expected JSON");
        }
    }

    // Pulls error.message out of an error reply, if there is one.
    private String errorMessage(String body) {
        try {
            return json.readTree(body).path("error").path("message").asText("no message");
        } catch (IOException e) {
            return "no message";
        }
    }
}
