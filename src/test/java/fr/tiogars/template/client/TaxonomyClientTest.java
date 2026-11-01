package fr.tiogars.template.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

class TaxonomyClientTest {
    @Test
    void tagCrudUsesReusableEndpoint() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            var client = new TagClient(server.url("/").uri());
            String tag = "{\"id\":1,\"code\":\"TAG\",\"label\":\"Tag\",\"description\":null}";
            server.enqueue(json(201, tag));
            assertThat(client.create(new CreateTagRequest("TAG", "Tag", null)).code()).isEqualTo("TAG");
            assertThat(server.takeRequest().getPath()).isEqualTo("/api/v1/tag");
            server.enqueue(json(200, "[" + tag + "]"));
            assertThat(client.findAll()).hasSize(1);
            assertThat(server.takeRequest().getMethod()).isEqualTo("GET");
            server.enqueue(json(200, tag));
            assertThat(client.findById(1).id()).isEqualTo(1L);
            assertThat(server.takeRequest().getPath()).isEqualTo("/api/v1/tag/1");
            server.enqueue(json(200, tag));
            client.update(1, new UpdateTagRequest("Tag", null));
            assertThat(server.takeRequest().getMethod()).isEqualTo("PUT");
            server.enqueue(new MockResponse().setResponseCode(204));
            client.delete(1);
            assertThat(server.takeRequest().getMethod()).isEqualTo("DELETE");
        }
    }

    @Test
    void nomenclatureCrudPreservesParentAndReportsConflict() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            var client = new NomenclatureClient(server.url("/").uri());
            String category = "{\"id\":2,\"code\":\"CHILD\",\"label\":\"Child\",\"parentId\":1}";
            server.enqueue(json(201, category));
            assertThat(client.create(new CreateNomenclatureRequest("CHILD", "Child", null, 1L)).parentId()).isEqualTo(1L);
            var request = server.takeRequest();
            assertThat(request.getPath()).isEqualTo("/api/v1/nomenclature");
            assertThat(request.getBody().readUtf8()).contains("\"parentId\":1");
            server.enqueue(json(200, category));
            client.update(2, new UpdateNomenclatureRequest("Child", null, 1L));
            assertThat(server.takeRequest().getPath()).isEqualTo("/api/v1/nomenclature/2");
            server.enqueue(new MockResponse().setResponseCode(409));
            assertThatThrownBy(() -> client.delete(2)).isInstanceOf(TaskException.class).hasMessageContaining("409");
        }
    }

    private static MockResponse json(int status, String body) {
        return new MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body);
    }
}
