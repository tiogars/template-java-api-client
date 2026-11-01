package fr.tiogars.template.client;

public record UpdateNomenclatureRequest(String label, String description, Long parentId) {
}
