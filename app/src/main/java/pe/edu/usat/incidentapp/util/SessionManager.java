package pe.edu.usat.incidentapp.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Almacena la sesión del usuario (JWT) en SharedPreferences.
 * El rol, nombre, id y empresa se leen del propio JWT.
 */
public class SessionManager {
    public static final String ROL_ADMINISTRADOR = "Administrador";
    public static final String ROL_TECNICO = "Tecnico";
    public static final String ROL_SOLICITANTE = "Solicitante";

    private static final String PREFERENCIAS = "INCIDENTAPP_SP";
    private static final String KEY_TOKEN = "TOKEN_JWT";
    private static final String KEY_CORREO = "CORREO";

    private static SessionManager instance;
    private final SharedPreferences sharedPref;

    private SessionManager(Context context) {
        sharedPref = context.getApplicationContext().getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE);
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
    }

    public static SessionManager getInstance() {
        return instance;
    }

    public void guardarSesion(String token, String correo) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_CORREO, correo);
        editor.apply();
    }

    //Elimina el token almacenado (cierre de sesión). Se conserva el correo para el próximo ingreso
    public void cerrarSesion() {
        sharedPref.edit().remove(KEY_TOKEN).apply();
    }

    public String getToken() {
        return sharedPref.getString(KEY_TOKEN, null);
    }

    public String getCorreo() {
        return sharedPref.getString(KEY_CORREO, "");
    }

    //La sesión es válida si el token no expiró y pertenece a una empresa
    public boolean haySesionActiva() {
        String token = getToken();
        return JwtUtils.estaVigente(token) && JwtUtils.obtenerIdEmpresa(token) > 0;
    }

    public String getEmpresa() {
        String token = getToken();
        return token != null ? JwtUtils.obtenerEmpresa(token) : null;
    }

    public String getRol() {
        String token = getToken();
        return token != null ? JwtUtils.obtenerRol(token) : null;
    }

    public String getNombre() {
        String token = getToken();
        return token != null ? JwtUtils.obtenerNombre(token) : null;
    }

    public int getUsuarioId() {
        String token = getToken();
        return token != null ? JwtUtils.obtenerUsuarioId(token) : 0;
    }
}
