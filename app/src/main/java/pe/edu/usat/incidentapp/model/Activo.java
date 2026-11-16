package pe.edu.usat.incidentapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Activo de TI identificado por su código QR (GET /activos/{codigo_qr})
 */
public class Activo {
    private int id;
    @SerializedName("codigo_qr")
    private String codigoQr;
    private String nombre;
    private String tipo;
    private String estado;
    @SerializedName("id_sucursal")
    private int idSucursal;
    private String sucursal;

    public int getId() {
        return id;
    }

    public String getCodigoQr() {
        return codigoQr;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public String getEstado() {
        return estado;
    }

    public int getIdSucursal() {
        return idSucursal;
    }

    public String getSucursal() {
        return sucursal;
    }
}
