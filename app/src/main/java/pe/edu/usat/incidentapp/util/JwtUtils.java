package pe.edu.usat.incidentapp.util;

import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * Lee los datos (payload) del JWT sin validar la firma.
 * La firma la valida el API; la app solo necesita el rol y la expiración.
 */
public class JwtUtils {

    private static JSONObject obtenerPayload(String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length < 2) {
                return null;
            }
            byte[] bytes = Base64.decode(partes[1], Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    public static String obtenerRol(String token) {
        JSONObject payload = obtenerPayload(token);
        return payload != null ? payload.optString("rol", null) : null;
    }

    public static String obtenerNombre(String token) {
        JSONObject payload = obtenerPayload(token);
        return payload != null ? payload.optString("nombre", null) : null;
    }

    public static String obtenerEmpresa(String token) {
        JSONObject payload = obtenerPayload(token);
        return payload != null ? payload.optString("empresa", null) : null;
    }

    public static int obtenerIdEmpresa(String token) {
        JSONObject payload = obtenerPayload(token);
        return payload != null ? payload.optInt("id_empresa", 0) : 0;
    }

    public static int obtenerUsuarioId(String token) {
        JSONObject payload = obtenerPayload(token);
        return payload != null ? payload.optInt("usuario_id", 0) : 0;
    }

    //El token es válido mientras la fecha de expiración (exp, en segundos) no haya pasado
    public static boolean estaVigente(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        JSONObject payload = obtenerPayload(token);
        if (payload == null) {
            return false;
        }
        long exp = payload.optLong("exp", 0);
        return exp * 1000 > System.currentTimeMillis();
    }
}
