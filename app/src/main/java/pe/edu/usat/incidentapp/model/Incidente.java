package pe.edu.usat.incidentapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Incidente tal como lo devuelve GET /incidentes
 */
public class Incidente {
    private int id;
    private String titulo;
    private String descripcion;
    private String categoria;
    private String prioridad;
    private String estado;

    //Fecha de registro en UTC con formato yyyy-MM-dd HH:mm:ss
    @SerializedName("fecha_registro")
    private String fechaRegistro;

    //Fecha en que se resolvió (null si aún no se resuelve). Se usa en las métricas
    @SerializedName("fecha_resolucion")
    private String fechaResolucion;

    private String solicitante;
    private String tecnico;
    private String sucursal;
    private String activo;

    //Primera foto: link de internet (http...) o nombre del archivo subido al API
    private String foto;

    //true cuando el incidente se registró sin conexión y aún no se envía al API
    private transient boolean pendiente;

    public Incidente() {
    }

    public Incidente(int id, String titulo, String descripcion, String categoria, String prioridad,
                     String estado, String fechaRegistro, String solicitante, String tecnico,
                     String sucursal, String activo) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.prioridad = prioridad;
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
        this.solicitante = solicitante;
        this.tecnico = tecnico;
        this.sucursal = sucursal;
        this.activo = activo;
    }

    public int getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public String getEstado() {
        return estado;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public String getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(String fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public String getTecnico() {
        return tecnico;
    }

    public String getSucursal() {
        return sucursal;
    }

    public String getActivo() {
        return activo;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public boolean isPendiente() {
        return pendiente;
    }

    public void setPendiente(boolean pendiente) {
        this.pendiente = pendiente;
    }
}
