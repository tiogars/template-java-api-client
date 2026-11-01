package fr.tiogars.template.client;

import java.net.URI;

public class TagClient extends TaxonomyClient<Tag, CreateTagRequest, UpdateTagRequest> {
    public TagClient(URI baseUri) {
        super(baseUri, "/api/v1/tag", Tag.class);
    }
}
