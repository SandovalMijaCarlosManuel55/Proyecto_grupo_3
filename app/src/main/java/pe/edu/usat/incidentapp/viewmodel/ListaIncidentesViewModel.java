package pe.edu.usat.incidentapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import pe.edu.usat.incidentapp.model.Incidente;
import pe.edu.usat.incidentapp.repository.IncidenteRepository;
import pe.edu.usat.incidentapp.util.Resource;

public class ListaIncidentesViewModel extends ViewModel {
    private final IncidenteRepository repository = IncidenteRepository.getInstance();
    private final MutableLiveData<Resource<List<Incidente>>> incidentes = new MutableLiveData<>();

    //Filtros seleccionados (null = todos)
    private String filtroEstado;
    private String filtroPrioridad;

    public LiveData<Resource<List<Incidente>>> getIncidentes() {
        return incidentes;
    }

    //Avisa cuando se enviaron al API los incidentes registrados sin conexión
    public LiveData<String> getEventoSincronizacion() {
        return repository.getEventoSincronizacion();
    }

    public void cargar() {
        repository.listar(filtroEstado, filtroPrioridad, incidentes);
    }

    public void setFiltroEstado(String estado) {
        filtroEstado = estado;
        cargar();
    }

    public void setFiltroPrioridad(String prioridad) {
        filtroPrioridad = prioridad;
        cargar();
    }

    public String getFiltroEstado() {
        return filtroEstado;
    }

    public String getFiltroPrioridad() {
        return filtroPrioridad;
    }
}
