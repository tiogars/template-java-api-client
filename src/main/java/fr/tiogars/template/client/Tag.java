package fr.tiogars.template.client;

import java.time.Instant;

public record Tag(Long id, String code, String label, String description, Instant createdDate,
        Instant lastModifiedDate, String createdBy, String lastModifiedBy) {
}
