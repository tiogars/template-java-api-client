package fr.tiogars.template.client;

/**
 * Defines the mutable data of a Task.
 *
 * @param label replacement display label
 * @param description optional replacement description
 * @since 0.1.0
 */
public record UpdateTaskRequest(String label, String description, TaskStatus status,
        TaskPriority priority, java.time.LocalDate dueDate, String assignedTo,
        java.util.List<Long> tagIds, Long nomenclatureId) {
    public UpdateTaskRequest {
        status = status == null ? TaskStatus.TODO : status;
        priority = priority == null ? TaskPriority.NORMAL : priority;
        tagIds = tagIds == null ? java.util.List.of() : java.util.List.copyOf(tagIds);
    }

    public UpdateTaskRequest(String label, String description) {
        this(label, description, TaskStatus.TODO, TaskPriority.NORMAL, null, null, java.util.List.of(), null);
    }
}