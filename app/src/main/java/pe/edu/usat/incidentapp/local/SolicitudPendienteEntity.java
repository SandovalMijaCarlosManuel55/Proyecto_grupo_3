package pe.edu.usat.incidentapp.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Incidente registrado sin conexión que espera ser enviado al API.
 */
@Entity(tableName = "solicitudes_pendientes")
public class SolicitudPendienteEntity {
    public static final String ESTADO_PENDIENTE = "Pendiente";
    public static final String ESTADO_ERROR = "Error";

    @PrimaryKey(autoGenerate = true)
    public int id;
    public int idUsuario;
    public String titulo;
    public String descripcion;
    public String categoria;
    public String prioridad;
    public int idSucursal;
    public String sucursal;
    public String codigoQr;
    public String activo;
    //Rutas locales de las fotos separadas por "|"
    public String fotos;
    public String fechaRegistro;
    public String estado;
    public String mensajeError;
}
