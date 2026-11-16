package pe.edu.usat.incidentapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Cuerpo de PUT /incidentes/{id}/asignar
 */
public class AsignarRequest {
    @SerializedName("id_tecnico")
    private final int idTecnico;

    public AsignarRequest(int idTecnico) {
        this.idTecnico = idTecnico;
    }
}
