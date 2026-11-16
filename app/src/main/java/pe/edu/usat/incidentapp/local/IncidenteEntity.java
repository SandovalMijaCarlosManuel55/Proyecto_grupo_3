package pe.edu.usat.incidentapp.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import pe.edu.usat.incidentapp.model.Incidente;

/**
 * Incidente guardado en caché local (Room) para mostrarlo sin conexión.
 */
@Entity(tableName = "incidentes")
public class IncidenteEntity {
    @PrimaryKey
    public int id;
    public String titulo;
    public String descripcion;
    public String categoria;
    public String prioridad;
    public String estado;
    public String fechaRegistro;
    public String fechaResolucion;
    public String solicitante;
    public String tecnico;
    public String sucursal;
    public String activo;
    public String foto;

    public static IncidenteEntity desdeModelo(@NonNull Incidente incidente) {
        IncidenteEntity entity = new IncidenteEntity();
        entity.id = incidente.getId();
        entity.titulo = incidente.getTitulo();
        entity.descripcion = incidente.getDescripcion();
        entity.categoria = incidente.getCategoria();
        entity.prioridad = incidente.getPrioridad();
        entity.estado = incidente.getEstado();
        entity.fechaRegistro = incidente.getFechaRegistro();
        entity.fechaResolucion = incidente.getFechaResolucion();
        entity.solicitante = incidente.getSolicitante();
        entity.tecnico = incidente.getTecnico();
        entity.sucursal = incidente.getSucursal();
        entity.activo = incidente.getActivo();
        entity.foto = incidente.getFoto();
        return entity;
    }

    public Incidente aModelo() {
        Incidente incidente = new Incidente(id, titulo, descripcion, categoria, prioridad, estado,
                fechaRegistro, solicitante, tecnico, sucursal, activo);
        incidente.setFoto(foto);
        incidente.setFechaResolucion(fechaResolucion);
        return incidente;
    }
}
