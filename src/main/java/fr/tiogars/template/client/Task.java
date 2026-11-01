package fr.tiogars.template.client;

import java.time.Instant;

/**
 * Represents a Task returned by the Task API.
 *
 * @param id persistent identifier
 * @param code unique template code
 * @param label display label
 * @param description optional description
 * @param createdDate creation timestamp
 * @param lastModifiedDate last modification timestamp
 * @param createdBy username at creation
 * @param lastModifiedBy username at the last modification
 * @since 0.1.0
 */
public record Task(
        Long id,
        String code,
        String label,
        String description,
        Instant createdDate,
        Instant lastModifiedDate,
        String createdBy,
        String lastModifiedBy,
        TaskStatus status,
        TaskPriority priority,
        java.time.LocalDate dueDate,
        String assignedTo,
        java.util.List<Long> tagIds,
        Long nomenclatureId
) {
    public Task {
        status = status == null ? TaskStatus.TODO : status;
        priority = priority == null ? TaskPriority.NORMAL : priority;
        tagIds = tagIds == null ? java.util.List.of() : java.util.List.copyOf(tagIds);
    }

    public Task(Long id, String code, String label, String description,
            Instant createdDate, Instant lastModifiedDate, String createdBy, String lastModifiedBy) {
        this(id, code, label, description, createdDate, lastModifiedDate, createdBy, lastModifiedBy,
                TaskStatus.TODO, TaskPriority.NORMAL, null, null, java.util.List.of(), null);
    }
    /**
     * Creates a template without audit actors for compatibility with older API responses.
     *
     * @param id persistent identifier
     * @param code unique template code
     * @param label display label
     * @param description optional description
     * @param createdDate creation timestamp
     * @param lastModifiedDate last modification timestamp
     */
    public Task(Long id, String code, String label, String description,
            Instant createdDate, Instant lastModifiedDate) {
        this(id, code, label, description, createdDate, lastModifiedDate, null, null);
    }
}