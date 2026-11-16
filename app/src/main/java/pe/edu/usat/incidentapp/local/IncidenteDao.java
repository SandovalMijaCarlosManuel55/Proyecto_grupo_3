package pe.edu.usat.incidentapp.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface IncidenteDao {

    //Filtros opcionales: si estado o prioridad es null no se filtra por ese campo
    @Query("SELECT * FROM incidentes " +
            "WHERE (:estado IS NULL OR estado = :estado) " +
            "AND (:prioridad IS NULL OR prioridad = :prioridad) " +
            "ORDER BY fechaRegistro DESC, id DESC")
    List<IncidenteEntity> listar(String estado, String prioridad);

    @Query("SELECT * FROM incidentes WHERE id = :id")
    IncidenteEntity obtener(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(List<IncidenteEntity> incidentes);

    @Query("DELETE FROM incidentes")
    void eliminarTodos();

    //Reemplaza toda la caché con la respuesta más reciente del API
    @Transaction
    default void reemplazar(List<IncidenteEntity> incidentes) {
        eliminarTodos();
        insertar(incidentes);
    }
}
