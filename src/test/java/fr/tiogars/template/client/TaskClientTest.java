package fr.tiogars.template.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class TaskClientTest {

    private static final String TEMPLATE_JSON = """
            {
              "id": 42,
              "code": "customers",
              "label": "Customers",
              "description": null,
              "createdDate": "2026-08-29T12:00:00Z",
              "lastModifiedDate": "2026-08-30T12:00:00Z",
              "createdBy": "creator",
              "lastModifiedBy": "editor"
            }
            """;
    private static final String TEMPLATE_LIST_JSON = "[" + TEMPLATE_JSON + "]";

    private MockWebServer server;
    private TaskClient client;

    @BeforeEach
    public void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new TaskClient(server.url("/").uri());
    }

    @AfterEach
    public void stopServer() throws IOException {
        server.shutdown();
    }

    @Test
    public void create_shouldPostRequestAndReturnTemplate() throws Exception {
        enqueueJson(201, TEMPLATE_JSON);

        Task template = client.create(new CreateTaskRequest("customers", "Customers", null));

        assertThat(template).isEqualTo(expectedTemplate());
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/v1/task");
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
        assertThat(request.getBody().readUtf8())
                .isEqualTo("{\"code\":\"customers\",\"label\":\"Customers\",\"description\":null,"
                        + "\"status\":\"TODO\",\"priority\":\"NORMAL\",\"dueDate\":null,\"assignedTo\":null,"
                        + "\"tagIds\":[],\"nomenclatureId\":null}");
    }

    @Test
    public void findAll_shouldGetCollection() throws Exception {
        enqueueJson(200, TEMPLATE_LIST_JSON);

        assertThat(client.findAll()).containsExactly(expectedTemplate());
        assertRequest("GET", "/api/v1/task", "application/json");
    }

    @Test
    public void findById_shouldGetTemplate() throws Exception {
        enqueueJson(200, TEMPLATE_JSON);

        assertThat(client.findById(42)).isEqualTo(expectedTemplate());
        assertRequest("GET", "/api/v1/task/42", "application/json");
    }

    @Test
    public void findById_acceptsOlderResponsesWithoutAuditActors() throws Exception {
        enqueueJson(200, """
                {"id":42,"code":"customers","label":"Customers","description":null,
                 "createdDate":"2026-08-29T12:00:00Z","lastModifiedDate":"2026-08-30T12:00:00Z"}
                """);

        assertThat(client.findById(42)).isEqualTo(new Task(
                42L, "customers", "Customers", null,
                Instant.parse("2026-08-29T12:00:00Z"), Instant.parse("2026-08-30T12:00:00Z")));
    }

    @Test
    void taskAttributesUseIsoDatesAndRoundtrip() throws Exception {
        enqueueJson(201, """
                {"id":42,"code":"TASK","label":"Task","description":null,
                 "status":"IN_PROGRESS","priority":"URGENT","dueDate":"2026-12-31",
                 "assignedTo":"alice","tagIds":[2,7],"nomenclatureId":5,
                 "createdDate":"2026-08-29T12:00:00Z","lastModifiedDate":"2026-08-30T12:00:00Z",
                 "createdBy":"creator","lastModifiedBy":"editor"}
                """);
        var request = new CreateTaskRequest("TASK", "Task", null, TaskStatus.IN_PROGRESS, TaskPriority.URGENT,
                java.time.LocalDate.parse("2026-12-31"), "alice", List.of(2L, 7L), 5L);
        Task result = client.create(request);
        assertThat(result.status()).isEqualTo(request.status());
        assertThat(result.priority()).isEqualTo(request.priority());
        assertThat(result.dueDate()).isEqualTo(request.dueDate());
        assertThat(result.assignedTo()).isEqualTo(request.assignedTo());
        assertThat(result.tagIds()).isEqualTo(request.tagIds());
        assertThat(result.nomenclatureId()).isEqualTo(request.nomenclatureId());
        assertThat(server.takeRequest().getBody().readUtf8()).contains("\"dueDate\":\"2026-12-31\"");
    }

    @Test
    public void update_shouldPutRequestAndReturnTemplate() throws Exception {
        enqueueJson(200, TEMPLATE_JSON);

        assertThat(client.update(42, new UpdateTaskRequest("Customers", null)))
                .isEqualTo(expectedTemplate());
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("PUT");
        assertThat(request.getPath()).isEqualTo("/api/v1/task/42");
        assertThat(request.getBody().readUtf8())
                .isEqualTo("{\"label\":\"Customers\",\"description\":null,"
                        + "\"status\":\"TODO\",\"priority\":\"NORMAL\",\"dueDate\":null,\"assignedTo\":null,"
                        + "\"tagIds\":[],\"nomenclatureId\":null}");
    }

    @Test
    public void delete_shouldSendDeleteRequest() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));

        client.delete(42);

        assertRequest("DELETE", "/api/v1/task/42", null);
    }

    @Test
    public void seed_shouldPostRequestsAndReturnTemplates() throws Exception {
        enqueueJson(201, TEMPLATE_LIST_JSON);

        List<Task> templates = client.seed(
                List.of(new CreateTaskRequest("customers", "Customers", null)));

        assertThat(templates).containsExactly(expectedTemplate());
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/v1/task/seed");
        assertThat(request.getBody().readUtf8()).startsWith("[{\"code\":\"customers\"");
    }

    @Test
    public void importJson_shouldPostRequestsAndReturnTemplates() throws Exception {
        enqueueJson(201, TEMPLATE_LIST_JSON);

        assertThat(client.importJson(List.of(new CreateTaskRequest("customers", "Customers", null))))
                .containsExactly(expectedTemplate());
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/v1/task/import.json");
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
    }

    @Test
    public void importCsv_shouldPostCsvAndReturnTemplates() throws Exception {
        enqueueJson(201, TEMPLATE_LIST_JSON);
        String csv = "code,label,description\ncustomers,Customers,";

        assertThat(client.importCsv(csv)).containsExactly(expectedTemplate());
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/v1/task/import.csv");
        assertThat(request.getHeader("Content-Type")).isEqualTo("text/csv");
        assertThat(request.getBody().readUtf8()).isEqualTo(csv);
    }

    @Test
    public void exportJson_shouldGetTemplates() throws Exception {
        enqueueJson(200, TEMPLATE_LIST_JSON);

        assertThat(client.exportJson()).containsExactly(expectedTemplate());
        assertRequest("GET", "/api/v1/task/export.json", "application/json");
    }

    @Test
    public void exportCsv_shouldGetCsvDocument() throws Exception {
        String csv = "id,code,label,description,createdDate,lastModifiedDate\n";
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "text/csv").setBody(csv));

        assertThat(client.exportCsv()).isEqualTo(csv);
        assertRequest("GET", "/api/v1/task/export.csv", "text/csv");
    }

    @Test
    public void request_shouldThrowTaskExceptionOnUnexpectedStatus() {
        server.enqueue(new MockResponse().setResponseCode(404));

        assertThatThrownBy(() -> client.findById(99))
                .isInstanceOf(TaskException.class)
                .hasMessage("Expected HTTP 200 but received 404");
    }

    private void enqueueJson(int status, String body) {
        server.enqueue(new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body));
    }

    private void assertRequest(String method, String path, String accept) throws InterruptedException {
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo(method);
        assertThat(request.getPath()).isEqualTo(path);
        if (accept != null) {
            assertThat(request.getHeader("Accept")).isEqualTo(accept);
        }
    }

    private Task expectedTemplate() {
        return new Task(
                42L,
                "customers",
                "Customers",
                null,
                Instant.parse("2026-08-29T12:00:00Z"),
                Instant.parse("2026-08-30T12:00:00Z"),
                "creator",
                "editor");
    }
}