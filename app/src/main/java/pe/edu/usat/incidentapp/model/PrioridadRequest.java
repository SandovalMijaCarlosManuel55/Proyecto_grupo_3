package pe.edu.usat.incidentapp.model;

/**
 * Cuerpo de PUT /incidentes/{id}/prioridad (solo Administrador)
 */
public class PrioridadRequest {
    private final String prioridad;
    private final String comentario;

    public PrioridadRequest(String prioridad, String comentario) {
        this.prioridad = prioridad;
        this.comentario = comentario;
    }
}
