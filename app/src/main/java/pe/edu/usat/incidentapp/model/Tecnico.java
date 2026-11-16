package pe.edu.usat.incidentapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Técnico disponible para asignar (GET /usuarios/tecnicos).
 */
public class Tecnico {
    private int id;
    private String nombre;
    private String correo;

    @SerializedName("incidentes_activos")
    private int incidentesActivos;

    //Última ubicación compartida (null si nunca la compartió)
    private Double latitud;
    private Double longitud;

    @SerializedName("fecha_ubicacion")
    private String fechaUbicacion;

    //Incidente que está atendiendo, por ejemplo "#3 Sin internet en Terminal Trujillo"
    @SerializedName("incidente_activo")
    private String incidenteActivo;

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public int getIncidentesActivos() {
        return incidentesActivos;
    }

    public Double getLatitud() {
        return latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public String getFechaUbicacion() {
        return fechaUbicacion;
    }

    public String getIncidenteActivo() {
        return incidenteActivo;
    }

    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
    }

    //Texto que se muestra en la lista para elegir técnico
    @Override
    public String toString() {
        return nombre + " (" + incidentesActivos + " activos)";
    }
}
