package pe.edu.usat.incidentapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import pe.edu.usat.incidentapp.repository.AuthRepository;
import pe.edu.usat.incidentapp.repository.IncidenteRepository;
import pe.edu.usat.incidentapp.util.SessionManager;

public class MainViewModel extends ViewModel {
    private final AuthRepository authRepository = AuthRepository.getInstance();
    private final IncidenteRepository incidenteRepository = IncidenteRepository.getInstance();

    public LiveData<String> getEventoSincronizacion() {
        return incidenteRepository.getEventoSincronizacion();
    }

    //Al recuperar la conexión se envían los incidentes pendientes
    public void conexionRecuperada() {
        incidenteRepository.sincronizarPendientes();
    }

    public boolean haySesionActiva() {
        return SessionManager.getInstance().haySesionActiva();
    }

    public void registrarTokenFcm() {
        if (haySesionActiva()) {
            authRepository.registrarTokenFcm();
        }
    }

    public void cerrarSesion() {
        authRepository.cerrarSesion();
    }

    public String getNombre() {
        return SessionManager.getInstance().getNombre();
    }

    public String getEmpresa() {
        return SessionManager.getInstance().getEmpresa();
    }

    public boolean esAdministrador() {
        return SessionManager.ROL_ADMINISTRADOR.equals(SessionManager.getInstance().getRol());
    }

    //Rol para mostrar en la barra (en la BD se guarda "Tecnico" sin tilde)
    public String getRol() {
        String rol = SessionManager.getInstance().getRol();
        return SessionManager.ROL_TECNICO.equals(rol) ? "Técnico" : rol;
    }
}
