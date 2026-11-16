package pe.edu.usat.incidentapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import pe.edu.usat.incidentapp.model.Sucursal;
import pe.edu.usat.incidentapp.model.Tecnico;
import pe.edu.usat.incidentapp.repository.IncidenteRepository;
import pe.edu.usat.incidentapp.util.Resource;
import pe.edu.usat.incidentapp.util.SessionManager;

public class MapaViewModel extends ViewModel {
    private final IncidenteRepository repository = IncidenteRepository.getInstance();

    private final MutableLiveData<Resource<List<Sucursal>>> sucursales = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<Tecnico>>> tecnicos = new MutableLiveData<>();
    private final MutableLiveData<Resource<String>> ubicacionCompartida = new MutableLiveData<>();

    public LiveData<Resource<List<Sucursal>>> getSucursales() {
        return sucursales;
    }

    public LiveData<Resource<List<Tecnico>>> getTecnicos() {
        return tecnicos;
    }

    public LiveData<Resource<String>> getUbicacionCompartida() {
        return ubicacionCompartida;
    }

    //Sucursales para todos; el Administrador además ve la posición de los técnicos
    public void cargar() {
        repository.listarSucursales(sucursales);
        if (esAdministrador()) {
            repository.listarTecnicos(tecnicos);
        }
    }

    public void compartirUbicacion(double latitud, double longitud) {
        repository.compartirUbicacion(latitud, longitud, ubicacionCompartida);
    }

    //Evita repetir el mensaje al girar la pantalla
    public void ubicacionAtendida() {
        ubicacionCompartida.setValue(null);
    }

    public boolean esAdministrador() {
        return SessionManager.ROL_ADMINISTRADOR.equals(SessionManager.getInstance().getRol());
    }

    public boolean esTecnico() {
        return SessionManager.ROL_TECNICO.equals(SessionManager.getInstance().getRol());
    }
}
