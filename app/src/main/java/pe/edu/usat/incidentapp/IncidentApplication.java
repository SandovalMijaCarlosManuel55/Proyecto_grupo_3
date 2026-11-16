package pe.edu.usat.incidentapp;

import android.app.Application;

import pe.edu.usat.incidentapp.repository.AuthRepository;
import pe.edu.usat.incidentapp.repository.IncidenteRepository;
import pe.edu.usat.incidentapp.service.IncidentMessagingService;
import pe.edu.usat.incidentapp.util.SessionManager;

public class IncidentApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //Inicializar la sesión y los repositorios con el contexto de la aplicación
        SessionManager.init(this);
        AuthRepository.init(this);
        IncidenteRepository.init(this);

        //Canal para las notificaciones push (FCM)
        IncidentMessagingService.crearCanal(this);
    }
}
