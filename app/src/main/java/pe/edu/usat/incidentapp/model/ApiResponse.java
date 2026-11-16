package pe.edu.usat.incidentapp.model;

/**
 * Respuesta estándar del API REST: {"status": true, "data": ..., "message": "..."}
 */
public class ApiResponse<T> {
    private boolean status;
    private T data;
    private String message;

    public boolean isStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
