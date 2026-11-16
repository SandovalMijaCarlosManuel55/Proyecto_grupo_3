package pe.edu.usat.incidentapp.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SolicitudPendienteDao {

    @Insert
    long insertar(SolicitudPendienteEntity solicitud);

    @Update
    void actualizar(SolicitudPendienteEntity solicitud);

    @Delete
    void eliminar(SolicitudPendienteEntity solicitud);

    @Query("SELECT * FROM solicitudes_pendientes WHERE idUsuario = :idUsuario ORDER BY id DESC")
    List<SolicitudPendienteEntity> listarPorUsuario(int idUsuario);

    @Query("SELECT * FROM solicitudes_pendientes WHERE estado = 'Pendiente' AND idUsuario = :idUsuario ORDER BY id")
    List<SolicitudPendienteEntity> listarPorSincronizar(int idUsuario);
}
