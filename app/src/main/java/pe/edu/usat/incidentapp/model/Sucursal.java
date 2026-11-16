package pe.edu.usat.incidentapp.model;

import com.google.gson.annotations.SerializedName;

public class Sucursal {
    private int id;
    private String nombre;
    private String direccion;
    private double latitud;
    private double longitud;

    //Incidentes Abiertos o En proceso (se muestran al tocar el marcador del mapa)
    @SerializedName("incidentes_abiertos")
    private int incidentesAbiertos;

    public Sucursal(int id, String nombre, String direccion, double latitud, double longitud,
                    int incidentesAbiertos) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.incidentesAbiertos = incidentesAbiertos;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public int getIncidentesAbiertos() {
        return incidentesAbiertos;
    }

    //Texto que se muestra en el desplegable de sucursales
    @Override
    public String toString() {
        return nombre;
    }
}
