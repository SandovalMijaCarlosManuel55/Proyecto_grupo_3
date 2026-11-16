package pe.edu.usat.incidentapp.viewmodel;

import androidx.lifecycle.ViewModel;

import pe.edu.usat.incidentapp.util.SessionManager;

/**
 * Datos del usuario autenticado (leídos del JWT guardado en la sesión).
 */
public class PerfilViewModel extends ViewModel {
    private final SessionManager sesion = SessionManager.getInstance();

    public String getNombre() {
        return sesion.getNombre();
    }

    public String getCorreo() {
        return sesion.getCorreo();
    }

    public String getEmpresa() {
        return sesion.getEmpresa();
    }

    //En la BD el rol se guarda como "Tecnico" (sin tilde)
    public String getRol() {
        String rol = sesion.getRol();
        return SessionManager.ROL_TECNICO.equals(rol) ? "Técnico" : rol;
    }

    //Qué puede hacer cada rol en la app
    public String getPermisos() {
        String rol = sesion.getRol();
        if (SessionManager.ROL_ADMINISTRADOR.equals(rol)) {
            return "Ve todos los incidentes, asigna técnicos, cambia la prioridad, cierra o reabre incidentes "
                    + "y revisa los reportes y métricas del área de TI.";
        }
        if (SessionManager.ROL_TECNICO.equals(rol)) {
            return "Atiende los incidentes asignados (En proceso → Resuelto), escanea el QR del equipo "
                    + "y comparte su ubicación en el mapa.";
        }
        return "Registra incidentes con foto y QR del equipo y recibe notificaciones cuando cambia su estado.";
    }

    public boolean esAdministrador() {
        return SessionManager.ROL_ADMINISTRADOR.equals(sesion.getRol());
    }

    //Iniciales para el avatar (ej. "Carlos Mendoza" -> "CM")
    public String getIniciales() {
        String nombre = getNombre();
        if (nombre == null || nombre.trim().isEmpty()) {
            return "?";
        }
        StringBuilder iniciales = new StringBuilder();
        for (String parte : nombre.trim().split("\\s+")) {
            if (iniciales.length() < 2 && !parte.isEmpty()) {
                iniciales.append(Character.toUpperCase(parte.charAt(0)));
            }
        }
        return iniciales.toString();
    }
}
