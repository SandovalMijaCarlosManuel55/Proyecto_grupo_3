package pe.edu.usat.incidentapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import pe.edu.usat.incidentapp.model.Activo;
import pe.edu.usat.incidentapp.model.IncidenteRegistrado;
import pe.edu.usat.incidentapp.model.Sucursal;
import pe.edu.usat.incidentapp.repository.IncidenteRepository;
import pe.edu.usat.incidentapp.util.Resource;

public class NuevoIncidenteViewModel extends ViewModel {
    public static final int MAXIMO_FOTOS = 2;

    private final IncidenteRepository repository = IncidenteRepository.getInstance();

    private final MutableLiveData<Resource<List<Sucursal>>> sucursales = new MutableLiveData<>();
    private final MutableLiveData<Resource<Activo>> activo = new MutableLiveData<>();
    private final MutableLiveData<Resource<IncidenteRegistrado>> registro = new MutableLiveData<>();
    private final MutableLiveData<List<String>> fotos = new MutableLiveData<>(new ArrayList<>());

    //Código QR escaneado (se conserva aunque no se pueda consultar el activo sin conexión)
    private String codigoQr;
    private String nombreActivo;

    public LiveData<Resource<List<Sucursal>>> getSucursales() {
        return sucursales;
    }

    public LiveData<Resource<Activo>> getActivo() {
        return activo;
    }

    public LiveData<Resource<IncidenteRegistrado>> getRegistro() {
        return registro;
    }

    public LiveData<List<String>> getFotos() {
        return fotos;
    }

    public void cargarSucursales() {
        repository.listarSucursales(sucursales);
    }

    public void codigoEscaneado(String codigo) {
        codigoQr = codigo;
        nombreActivo = null;
        repository.buscarActivo(codigo, activo);
    }

    public void setNombreActivo(String nombre) {
        nombreActivo = nombre;
    }

    //El código escaneado no corresponde a ningún activo registrado
    public void limpiarActivo() {
        codigoQr = null;
        nombreActivo = null;
    }

    public String getCodigoQr() {
        return codigoQr;
    }

    public boolean puedeAgregarFoto() {
        return fotos.getValue().size() < MAXIMO_FOTOS;
    }

    public void agregarFoto(String ruta) {
        List<String> lista = new ArrayList<>(fotos.getValue());
        if (lista.size() < MAXIMO_FOTOS) {
            lista.add(ruta);
            fotos.setValue(lista);
        }
    }

    public void quitarFoto(int posicion) {
        List<String> lista = new ArrayList<>(fotos.getValue());
        if (posicion >= 0 && posicion < lista.size()) {
            lista.remove(posicion);
            fotos.setValue(lista);
        }
    }

    public void registrar(String titulo, String descripcion, String categoria, String prioridad, Sucursal sucursal) {
        repository.registrar(titulo, descripcion, categoria, prioridad, sucursal,
                codigoQr, nombreActivo, fotos.getValue(), registro);
    }
}
