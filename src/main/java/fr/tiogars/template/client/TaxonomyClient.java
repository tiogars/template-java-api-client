package fr.tiogars.template.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Shared HTTP transport for reusable taxonomy CRUD clients. */
public abstract class TaxonomyClient<T, C, U> {
    private final URI baseUri;
    private final String path;
    private final Class<T> type;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    protected TaxonomyClient(URI baseUri, String path, Class<T> type) {
        this(baseUri, path, type, HttpClient.newHttpClient(), new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

    protected TaxonomyClient(URI baseUri, String path, Class<T> type, HttpClient httpClient, ObjectMapper mapper) {
        this.baseUri = baseUri;
        this.path = path;
        this.type = type;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    public T create(C request) throws IOException, InterruptedException {
        return mapper.readValue(send("", "POST", request, 201), type);
    }

    public T update(long id, U request) throws IOException, InterruptedException {
        return mapper.readValue(send("/" + id, "PUT", request, 200), type);
    }

    public T findById(long id) throws IOException, InterruptedException {
        return mapper.readValue(send("/" + id, "GET", null, 200), type);
    }

    public List<T> findAll() throws IOException, InterruptedException {
        return mapper.readValue(send("", "GET", null, 200), mapper.getTypeFactory().constructCollectionType(List.class, type));
    }

    public void delete(long id) throws IOException, InterruptedException {
        send("/" + id, "DELETE", null, 204);
    }

    private String send(String suffix, String method, Object body, int expectedStatus) throws IOException, InterruptedException {
        var publisher = body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body));
        var request = HttpRequest.newBuilder(baseUri.resolve(path + suffix)).header("Accept", "application/json")
                .header("Content-Type", "application/json").method(method, publisher).build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != expectedStatus) {
            throw new TaskException("Expected HTTP " + expectedStatus + " but received " + response.statusCode());
        }
        return response.body();
    }
}
