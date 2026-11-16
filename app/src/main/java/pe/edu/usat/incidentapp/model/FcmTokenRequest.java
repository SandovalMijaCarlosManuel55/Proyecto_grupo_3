package pe.edu.usat.incidentapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Cuerpo de POST /auth/fcm-token. Con fcm_token null se desregistra el dispositivo (cierre de sesión).
 */
public class FcmTokenRequest {
    @SerializedName("fcm_token")
    private final String fcmToken;

    public FcmTokenRequest(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
