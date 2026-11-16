package pe.edu.usat.incidentapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import pe.edu.usat.incidentapp.model.IncidenteDetalle;
import pe.edu.usat.incidentapp.model.Tecnico;
import pe.edu.usat.incidentapp.repository.IncidenteRepository;
import pe.edu.usat.incidentapp.util.Resource;
import pe.edu.usat.incidentapp.util.SessionManager;

public class DetalleIncidenteViewModel extends ViewModel {
    private final IncidenteRepository repository = IncidenteRepository.getInstance();

    private final MutableLiveData<Resource<IncidenteDetalle>> detalle = new MutableLiveData<>();
    private final MutableLiveData<Resource<String>> accion = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<Tecnico>>> tecnicos = new MutableLiveData<>();

    private int idIncidente;

    public LiveData<Resource<IncidenteDetalle>> getDetalle() {
        return detalle;
    }

    //Resultado de cambiar estado, asignar o comentar (mensaje del API)
    public LiveData<Resource<String>> getAccion() {
        return accion;
    }

    public LiveData<Resource<List<Tecnico>>> getTecnicos() {
        return tecnicos;
    }

    public void iniciar(int idIncidente) {
        if (this.idIncidente != idIncidente) {
            this.idIncidente = idIncidente;
            cargar();
        }
    }

    public void cargar() {
        repository.obtenerDetalle(idIncidente, detalle);
    }

    public void cambiarEstado(String estado, String comentario) {
        repository.cambiarEstado(idIncidente, estado, comentario, accion);
    }

    public void asignarTecnico(Tecnico tecnico) {
        repository.asignarTecnico(idIncidente, tecnico.getId(), accion);
    }

    public void cambiarPrioridad(String prioridad, String comentario) {
        repository.cambiarPrioridad(idIncidente, prioridad, comentario, accion);
    }

    public void comentar(String comentario) {
        repository.comentar(idIncidente, comentario, accion);
    }

    public void cargarTecnicos() {
        repository.listarTecnicos(tecnicos);
    }

    //Evita repetir el mensaje de la acción al girar la pantalla
    public void accionAtendida() {
        accion.setValue(null);
    }

    public void tecnicosAtendidos() {
        tecnicos.setValue(null);
    }

    // ---- Permisos según el rol del JWT ----

    public boolean esAdministrador() {
        return SessionManager.ROL_ADMINISTRADOR.equals(SessionManager.getInstance().getRol());
    }

    public boolean esTecnico() {
        return SessionManager.ROL_TECNICO.equals(SessionManager.getInstance().getRol());
    }

    //Estados a los que puede cambiar:
    //Técnico solo avanza un paso (Abierto -> En proceso -> Resuelto). Administrador todos (cierra y reabre)
    public String[] estadosPermitidos(String estadoActual) {
        if (!esAdministrador()) {
            String siguiente = siguienteEstadoTecnico(estadoActual);
            return siguiente != null ? new String[]{siguiente} : new String[0];
        }
        List<String> permitidos = new ArrayList<>();
        for (String estado : new String[]{"Abierto", "En proceso", "Resuelto", "Cerrado"}) {
            if (!estado.equals(estadoActual)) {
                permitidos.add(estado);
            }
        }
        return permitidos.toArray(new String[0]);
    }

    //null cuando el técnico ya no puede avanzar (Resuelto o Cerrado)
    public String siguienteEstadoTecnico(String estadoActual) {
        if ("Abierto".equals(estadoActual)) return "En proceso";
        if ("En proceso".equals(estadoActual)) return "Resuelto";
        return null;
    }

    //Prioridades distintas a la actual (cambio de prioridad del Administrador)
    public String[] prioridadesPermitidas(String prioridadActual) {
        List<String> permitidas = new ArrayList<>();
        for (String prioridad : new String[]{"Alta", "Media", "Baja"}) {
            if (!prioridad.equals(prioridadActual)) {
                permitidas.add(prioridad);
            }
        }
        return permitidas.toArray(new String[0]);
    }

    // ---- Verificación del equipo en campo (QR) ----

    //Compara el QR escaneado con el activo registrado en el incidente
    public boolean equipoCoincide(String codigoEscaneado) {
        IncidenteDetalle actual = getDetalleActual();
        return actual != null && actual.getCodigoQr() != null
                && actual.getCodigoQr().equalsIgnoreCase(codigoEscaneado.trim());
    }

    public IncidenteDetalle getDetalleActual() {
        Resource<IncidenteDetalle> resultado = detalle.getValue();
        return resultado != null ? resultado.getData() : null;
    }
}
