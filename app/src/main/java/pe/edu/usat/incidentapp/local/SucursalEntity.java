package pe.edu.usat.incidentapp.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import pe.edu.usat.incidentapp.model.Sucursal;

/**
 * Sucursal en caché para poder registrar incidentes y ver el mapa sin conexión.
 */
@Entity(tableName = "sucursales")
public class SucursalEntity {
    @PrimaryKey
    public int id;
    public String nombre;
    public String direccion;
    public double latitud;
    public double longitud;
    public int incidentesAbiertos;

    public static SucursalEntity desdeModelo(Sucursal sucursal) {
        SucursalEntity entity = new SucursalEntity();
        entity.id = sucursal.getId();
        entity.nombre = sucursal.getNombre();
        entity.direccion = sucursal.getDireccion();
        entity.latitud = sucursal.getLatitud();
        entity.longitud = sucursal.getLongitud();
        entity.incidentesAbiertos = sucursal.getIncidentesAbiertos();
        return entity;
    }

    public Sucursal aModelo() {
        return new Sucursal(id, nombre, direccion, latitud, longitud, incidentesAbiertos);
    }
}
