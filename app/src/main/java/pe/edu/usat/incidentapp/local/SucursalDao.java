package pe.edu.usat.incidentapp.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SucursalDao {

    @Query("SELECT * FROM sucursales ORDER BY nombre")
    List<SucursalEntity> listar();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(List<SucursalEntity> sucursales);

    @Query("DELETE FROM sucursales")
    void eliminarTodos();
}
