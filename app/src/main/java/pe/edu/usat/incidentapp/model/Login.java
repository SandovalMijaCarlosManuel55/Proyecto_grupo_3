package pe.edu.usat.incidentapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Datos que devuelve POST /auth/login
 */
public class Login {
    @SerializedName("usuario_id")
    private int usuarioId;
    private String nombre;
    private String correo;
    private String rol;
    private String token;
    @SerializedName("id_empresa")
    private int idEmpresa;
    private String empresa;

    public int getUsuarioId() {
        return usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getRol() {
        return rol;
    }

    public String getToken() {
        return token;
    }

    public int getIdEmpresa() {
        return idEmpresa;
    }

    public String getEmpresa() {
        return empresa;
    }
}
