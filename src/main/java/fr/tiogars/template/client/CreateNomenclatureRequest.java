package fr.tiogars.template.client;

public record CreateNomenclatureRequest(String code, String label, String description, Long parentId) {
}
