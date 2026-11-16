package pe.edu.usat.incidentapp.service;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import pe.edu.usat.incidentapp.R;
import pe.edu.usat.incidentapp.repository.AuthRepository;
import pe.edu.usat.incidentapp.ui.MainActivity;

/**
 * Recibe las notificaciones push de Firebase Cloud Messaging.
 * - App en primer plano: llega a onMessageReceived y aquí se muestra la notificación.
 * - App en segundo plano: Android muestra la notificación solo; al tocarla abre MainActivity
 *   con los datos ("id_incidente") como extras del Intent.
 */
public class IncidentMessagingService extends FirebaseMessagingService {
    public static final String CANAL_INCIDENTES = "incidentes";
    public static final String EXTRA_ID_INCIDENTE = "id_incidente";

    //Firebase generó un token nuevo para este celular: registrarlo en el API
    @Override
    public void onNewToken(@NonNull String token) {
        AuthRepository.getInstance().enviarTokenFcm(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage mensaje) {
        String titulo = "IncidentApp";
        String cuerpo = "";
        if (mensaje.getNotification() != null) {
            titulo = mensaje.getNotification().getTitle();
            cuerpo = mensaje.getNotification().getBody();
        }
        String idIncidente = mensaje.getData().get(EXTRA_ID_INCIDENTE);
        mostrarNotificacion(titulo, cuerpo, idIncidente);
    }

    private void mostrarNotificacion(String titulo, String cuerpo, String idIncidente) {
        //Android 13+: sin permiso de notificaciones no se puede mostrar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //Al tocar la notificación se abre la app directamente en el detalle del incidente
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_ID_INCIDENTE, idIncidente);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int codigo = idIncidente != null ? Integer.parseInt(idIncidente) : 0;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, codigo, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificacion = new NotificationCompat.Builder(this, CANAL_INCIDENTES)
                .setSmallIcon(R.drawable.ic_incidente)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(cuerpo))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(this).notify(codigo, notificacion.build());
    }

    //Canal de notificaciones (obligatorio desde Android 8). Se crea al iniciar la app
    public static void crearCanal(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(CANAL_INCIDENTES,
                    "Incidentes", NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription("Asignaciones, cambios de estado y comentarios de incidentes");
            context.getSystemService(NotificationManager.class).createNotificationChannel(canal);
        }
    }
}
