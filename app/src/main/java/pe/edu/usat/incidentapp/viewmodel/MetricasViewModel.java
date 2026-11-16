package pe.edu.usat.incidentapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.Collections;
import java.util.List;

import pe.edu.usat.incidentapp.model.Incidente;
import pe.edu.usat.incidentapp.model.Metricas;
import pe.edu.usat.incidentapp.repository.IncidenteRepository;
import pe.edu.usat.incidentapp.util.Resource;
import pe.edu.usat.incidentapp.util.SessionManager;

/**
 * Reportes y métricas del Administrador. Usa la misma fuente que la lista
 * (primero Room, luego el API) y transforma los incidentes en métricas.
 */
public class MetricasViewModel extends ViewModel {
    private final IncidenteRepository repository = IncidenteRepository.getInstance();
    private final MutableLiveData<Resource<List<Incidente>>> incidentes = new MutableLiveData<>();

    //Cada vez que llegan incidentes (caché o API) se recalculan las métricas
    private final LiveData<Resource<Metricas>> metricas = Transformations.map(incidentes, this::aMetricas);

    public LiveData<Resource<Metricas>> getMetricas() {
        return metricas;
    }

    //Sin filtros: el Administrador recibe todos los incidentes de su empresa
    public void cargar() {
        repository.listar(null, null, incidentes);
    }

    public boolean esAdministrador() {
        return SessionManager.ROL_ADMINISTRADOR.equals(SessionManager.getInstance().getRol());
    }

    public String getEmpresa() {
        return SessionManager.getInstance().getEmpresa();
    }

    private Resource<Metricas> aMetricas(Resource<List<Incidente>> resultado) {
        if (resultado == null) {
            return null;
        }
        List<Incidente> lista = resultado.getData();
        Metricas datos = lista != null && !lista.isEmpty() ? Metricas.calcular(lista) : null;
        switch (resultado.getEstado()) {
            case LOADING:
                return Resource.loading(datos);
            case SUCCESS:
                //Sin incidentes también es un resultado válido (métricas en cero)
                return Resource.success(datos != null ? datos : Metricas.calcular(Collections.emptyList()));
            default:
                return Resource.error(resultado.getMensaje(), datos, resultado.getCodigo());
        }
    }
}
