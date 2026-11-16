package pe.edu.usat.incidentapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Detalle completo que devuelve GET /incidentes/{id}: incidente + historial + fotos.
 */
public class IncidenteDetalle extends Incidente {
    private String direccion;
    private Double latitud;
    private Double longitud;

    @SerializedName("codigo_qr")
    private String codigoQr;

    private List<Historial> historial = new ArrayList<>();
    private List<String> fotos = new ArrayList<>();

    public IncidenteDetalle() {
    }

    //Detalle armado con los datos guardados en Room (modo offline)
    public IncidenteDetalle(Incidente incidente, List<Historial> historial) {
        super(incidente.getId(), incidente.getTitulo(), incidente.getDescripcion(), incidente.getCategoria(),
                incidente.getPrioridad(), incidente.getEstado(), incidente.getFechaRegistro(),
                incidente.getSolicitante(), incidente.getTecnico(), incidente.getSucursal(), incidente.getActivo());
        setFoto(incidente.getFoto());
        setFechaResolucion(incidente.getFechaResolucion());
        this.historial = historial;
        if (incidente.getFoto() != null) {
            fotos.add(incidente.getFoto());
        }
    }

    public String getDireccion() {
        return direccion;
    }

    public Double getLatitud() {
        return latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public String getCodigoQr() {
        return codigoQr;
    }

    public List<Historial> getHistorial() {
        return historial != null ? historial : new ArrayList<>();
    }

    public List<String> getFotos() {
        return fotos != null ? fotos : new ArrayList<>();
    }

    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
    }
}
