package fr.tiogars.template.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Provides HTTP access to the Task API.
 *
 * @since 0.1.0
 */
public class TaskClient {

    private static final String API_PATH = "/api/v1/task";
    private static final String APPLICATION_JSON = "application/json";
    private static final String TEXT_CSV = "text/csv";
    private static final TypeReference<List<Task>> TEMPLATE_LIST = new TypeReference<>() {
    };

    private final URI baseUri;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a client backed by the default Java HTTP client.
     *
     * @param baseUri base URI of the server
     */
    public TaskClient(URI baseUri) {
        this(baseUri, HttpClient.newHttpClient(), new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

    TaskClient(URI baseUri, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUri = baseUri;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a Task.
     *
     * @param request template data to create
     * @return template created by the server
     * @throws IOException if request serialization or response deserialization fails
     * @throws InterruptedException if the thread is interrupted while awaiting the response
     * @throws TaskException if the server returns an unexpected HTTP status
     */
    public Task create(CreateTaskRequest request) throws IOException, InterruptedException {
        return sendJson(jsonRequest(uri(""), "POST", request), Task.class, 201);
    }

    /**
     * Lists all Tasks.
     *
     * @return templates returned by the server
     * @throws IOException if response deserialization fails
     * @throws InterruptedException if the thread is interrupted while awaiting the response
     * @throws TaskException if the server returns an unexpected HTTP status
     */
    public List<Task> findAll() throws IOException, InterruptedException {
        return sendJson(getRequest(uri(""), APPLICATION_JSON), TEMPLATE_LIST, 200);
    }

    /**
     * Finds a Task by identifier.
     *
     * @param id persistent template identifier
     * @return matching template
     * @throws IOException if response deserialization fails
     * @throws InterruptedException if the thread is interrupted while awaiting the response
     * @throws TaskException if the server returns an unexpected HTTP status
     */
    public Task findById(long id) throws IOException, InterruptedException {
        return sendJson(getRequest(uri("/" + id), APPLICATION_JSON), Task.class, 200);
    }

    /**
     * Updates a Task.
     *
     * @param id persistent template identifier
     * @param request replacement label and description
     * @return template updated by the server
     * @throws IOException if request serialization or response deserialization fails
     * @throws InterruptedException if the thread is interrupted while awaiting the response
     * @throws TaskException if the server returns an unexpected HTTP status
     */
    public Task update(long id, UpdateTaskRequest request) throws IOException, InterruptedException {
        return sendJson(jsonRequest(uri("/" + id), "PUT", request), Task.class, 200);
    }

    /**
     * Deletes a Task.
     *
     * @param id persistent template identifier
     * @throws IOException if the request fails
     * @throws InterruptedException if the thread is interrupted while awaiting the response
     * @throws TaskException if the server returns an unexpected HTTP status
     */
    public void delete(long id) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri("/" + id)).DELETE().build();
        send(request, 204);
    }

    /**
     * Seeds Tasks from structured requests.
     *
     * @param requests templates to seed
     * @return templates stored by the server
     * @throws IOException if request serialization or response deserialization fails
     * @throws InterruptedException if the thread is interrupted while awaiting the response
     * @throws TaskException if the server returns an unexpected HTTP status
     */
    public List<Task> seed(List<CreateTaskRequest> requests) throws IOException, InterruptedException {
        return sendJson(jsonRequest(uri("/seed"), "POST", requests), TEMPLATE_LIST, 201);
    }

    /**
     * Imports Tasks from JSON-compatible requests.
     *
     * @param requests templates to import
     * @return templates imported by the server
     * @throws IOException if request serialization or response deserialization fails
     * @throws InterruptedException if the thread is interrupted while awaiting the response
     * @throws TaskException if the server returns an unexpected HTTP status
     */
    public List<Task> importJson(List<CreateTaskRequest> requests)
            throws IOException, InterruptedException {
        return sendJson(jsonRequest(uri("/import.json"), "POST", requests), TEMPLATE_LIST, 201);
    }

    /**
     * Imports Tasks from a CSV document.
     *
     * @param csv CSV document to import
     * @return templates imported by the server
     * @throws IOException if the request or response deserialization fails
     * @throws InterruptedException if the thread is interrupted while awaiting the response
     * @throws TaskException if the server returns an unexpected HTTP status
     */
    public List<Task> importCsv(String csv) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri("/import.csv"))
                .header("Content-Type", TEXT_CSV)
                .header("Accept", APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(csv))
                .build();
        return sendJson(request, TEMPLATE_LIST, 201);
    }

    /**
     * Exports all Tasks as JSON-compatible objects.
     *
     * @return templates exported by the server
     * @throws IOException if response deserialization fails
     * @throws InterruptedException if the thread is interrupted while awaiting the response
     * @throws TaskException if the server returns an unexpected HTTP status
     */
    public List<Task> exportJson() throws IOException, InterruptedException {
        return sendJson(getRequest(uri("/export.json"), APPLICATION_JSON), TEMPLATE_LIST, 200);
    }

    /**
     * Exports all Tasks as CSV.
     *
     * @return CSV document returned by the server
     * @throws IOException if the request fails
     * @throws InterruptedException if the thread is interrupted while awaiting the response
     * @throws TaskException if the server returns an unexpected HTTP status
     */
    public String exportCsv() throws IOException, InterruptedException {
        return send(getRequest(uri("/export.csv"), TEXT_CSV), 200).body();
    }

    private HttpRequest jsonRequest(URI uri, String method, Object body) throws IOException {
        return HttpRequest.newBuilder(uri)
                .header("Content-Type", APPLICATION_JSON)
                .header("Accept", APPLICATION_JSON)
                .method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
    }

    private HttpRequest getRequest(URI uri, String accept) {
        return HttpRequest.newBuilder(uri).header("Accept", accept).GET().build();
    }

    private <T> T sendJson(HttpRequest request, Class<T> responseType, int expectedStatus)
            throws IOException, InterruptedException {
        return objectMapper.readValue(send(request, expectedStatus).body(), responseType);
    }

    private <T> T sendJson(HttpRequest request, TypeReference<T> responseType, int expectedStatus)
            throws IOException, InterruptedException {
        return objectMapper.readValue(send(request, expectedStatus).body(), responseType);
    }

    private HttpResponse<String> send(HttpRequest request, int expectedStatus) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != expectedStatus) {
            throw new TaskException(
                    "Expected HTTP " + expectedStatus + " but received " + response.statusCode());
        }
        return response;
    }

    private URI uri(String suffix) {
        return baseUri.resolve(API_PATH + suffix);
    }
}