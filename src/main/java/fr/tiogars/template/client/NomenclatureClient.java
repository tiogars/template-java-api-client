package fr.tiogars.template.client;

import java.net.URI;

public class NomenclatureClient extends TaxonomyClient<Nomenclature, CreateNomenclatureRequest, UpdateNomenclatureRequest> {
    public NomenclatureClient(URI baseUri) {
        super(baseUri, "/api/v1/nomenclature", Nomenclature.class);
    }
}
