package pe.edu.usat.incidentapp.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface HistorialDao {

    @Query("SELECT * FROM historial WHERE idIncidente = :idIncidente ORDER BY fechaCambio DESC, id DESC")
    List<HistorialEntity> listarPorIncidente(int idIncidente);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(List<HistorialEntity> historial);

    @Query("DELETE FROM historial WHERE idIncidente = :idIncidente")
    void eliminarPorIncidente(int idIncidente);

    @Query("DELETE FROM historial")
    void eliminarTodos();

    //Reemplaza el historial de un incidente con la respuesta más reciente del API
    @Transaction
    default void reemplazar(int idIncidente, List<HistorialEntity> historial) {
        eliminarPorIncidente(idIncidente);
        insertar(historial);
    }
}
