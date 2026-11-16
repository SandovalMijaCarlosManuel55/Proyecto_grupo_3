package pe.edu.usat.incidentapp.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import pe.edu.usat.incidentapp.model.Historial;

/**
 * Cambio de estado guardado en caché local (Room) para ver el historial sin conexión.
 */
@Entity(tableName = "historial")
public class HistorialEntity {
    @PrimaryKey
    public int id;
    public int idIncidente;
    public String estadoAnterior;
    public String estadoNuevo;
    public String comentario;
    public String usuario;
    public String rol;
    public String fechaCambio;

    public static HistorialEntity desdeModelo(int idIncidente, @NonNull Historial historial) {
        HistorialEntity entity = new HistorialEntity();
        entity.id = historial.getId();
        entity.idIncidente = idIncidente;
        entity.estadoAnterior = historial.getEstadoAnterior();
        entity.estadoNuevo = historial.getEstadoNuevo();
        entity.comentario = historial.getComentario();
        entity.usuario = historial.getUsuario();
        entity.rol = historial.getRol();
        entity.fechaCambio = historial.getFechaCambio();
        return entity;
    }

    public Historial aModelo() {
        return new Historial(id, estadoAnterior, estadoNuevo, comentario, usuario, rol, fechaCambio);
    }
}
