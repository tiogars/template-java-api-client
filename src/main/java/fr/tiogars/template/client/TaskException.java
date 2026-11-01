package fr.tiogars.template.client;

/**
 * Indicates that the Task API returned an unexpected response.
 *
 * @since 0.1.0
 */
public class TaskException extends RuntimeException {

    /**
     * Creates an exception with the response failure details.
     *
     * @param message response failure details
     */
    public TaskException(String message) {
        super(message);
    }
}