package fr.tiogars.template.client;

/**
 * Defines the data required to create a Task.
 *
 * @param code unique template code
 * @param label display label
 * @param description optional description
 * @since 0.1.0
 */
public record CreateTaskRequest(String code, String label, String description, TaskStatus status,
        TaskPriority priority, java.time.LocalDate dueDate, String assignedTo,
        java.util.List<Long> tagIds, Long nomenclatureId) {
    public CreateTaskRequest {
        status = status == null ? TaskStatus.TODO : status;
        priority = priority == null ? TaskPriority.NORMAL : priority;
        tagIds = tagIds == null ? java.util.List.of() : java.util.List.copyOf(tagIds);
    }

    public CreateTaskRequest(String code, String label, String description) {
        this(code, label, description, TaskStatus.TODO, TaskPriority.NORMAL, null, null, java.util.List.of(), null);
    }
}