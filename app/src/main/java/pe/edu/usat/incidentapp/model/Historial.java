package pe.edu.usat.incidentapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Un cambio en el historial del incidente (cambio de estado, asignación o comentario).
 */
public class Historial {
    private int id;

    @SerializedName("estado_anterior")
    private String estadoAnterior;

    @SerializedName("estado_nuevo")
    private String estadoNuevo;

    private String comentario;
    private String usuario;
    private String rol;

    //Fecha en UTC con formato yyyy-MM-dd HH:mm:ss
    @SerializedName("fecha_cambio")
    private String fechaCambio;

    public Historial(int id, String estadoAnterior, String estadoNuevo, String comentario,
                     String usuario, String rol, String fechaCambio) {
        this.id = id;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.comentario = comentario;
        this.usuario = usuario;
        this.rol = rol;
        this.fechaCambio = fechaCambio;
    }

    public int getId() {
        return id;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public String getComentario() {
        return comentario;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getRol() {
        return rol;
    }

    public String getFechaCambio() {
        return fechaCambio;
    }

    //true cuando el estado cambió (false = asignación o comentario)
    public boolean esCambioDeEstado() {
        return estadoAnterior == null || !estadoAnterior.equals(estadoNuevo);
    }
}
