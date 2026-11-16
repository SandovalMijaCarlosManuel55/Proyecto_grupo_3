package pe.edu.usat.incidentapp.util;

import org.json.JSONObject;

import retrofit2.Response;

/**
 * Obtiene el mensaje de error ("message") que devuelve el API en respuestas 400, 401, 500, etc.
 */
public class ApiError {

    public static String mensaje(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                JSONObject jsonObject = new JSONObject(response.errorBody().string());
                return jsonObject.getString("message");
            }
        } catch (Exception ignored) {
        }
        return "Error del servidor (código " + response.code() + ")";
    }
}
