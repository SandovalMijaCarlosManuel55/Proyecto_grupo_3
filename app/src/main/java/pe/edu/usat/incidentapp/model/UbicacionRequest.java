package pe.edu.usat.incidentapp.model;

/**
 * Cuerpo de PUT /usuarios/ubicacion (el técnico comparte su ubicación actual).
 */
public class UbicacionRequest {
    private final double latitud;
    private final double longitud;

    public UbicacionRequest(double latitud, double longitud) {
        this.latitud = latitud;
        this.longitud = longitud;
    }
}
