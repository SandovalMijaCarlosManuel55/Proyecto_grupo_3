package pe.edu.usat.incidentapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Datos que devuelve POST /incidentes
 */
public class IncidenteRegistrado {
    private int id;
    private String estado;
    @SerializedName("total_fotos")
    private int totalFotos;

    //true cuando no hubo conexión y se guardó en Room como pendiente
    private transient boolean pendiente;

    public static IncidenteRegistrado pendiente() {
        IncidenteRegistrado registrado = new IncidenteRegistrado();
        registrado.estado = "Pendiente de sincronización";
        registrado.pendiente = true;
        return registrado;
    }

    public int getId() {
        return id;
    }

    public String getEstado() {
        return estado;
    }

    public int getTotalFotos() {
        return totalFotos;
    }

    public boolean isPendiente() {
        return pendiente;
    }
}
