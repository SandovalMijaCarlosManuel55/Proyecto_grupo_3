package pe.edu.usat.incidentapp.model;

/**
 * Cuerpo de PUT /incidentes/{id}/estado y de POST /incidentes/{id}/comentarios
 */
public class CambioEstadoRequest {
    private final String estado;
    private final String comentario;

    public CambioEstadoRequest(String estado, String comentario) {
        this.estado = estado;
        this.comentario = comentario;
    }

    //Solo comentario (no cambia el estado)
    public static CambioEstadoRequest comentario(String comentario) {
        return new CambioEstadoRequest(null, comentario);
    }
}
