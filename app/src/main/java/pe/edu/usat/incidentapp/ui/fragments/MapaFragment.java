package pe.edu.usat.incidentapp.ui.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import pe.edu.usat.incidentapp.R;
import pe.edu.usat.incidentapp.databinding.FragmentMapaBinding;
import pe.edu.usat.incidentapp.model.Sucursal;
import pe.edu.usat.incidentapp.model.Tecnico;
import pe.edu.usat.incidentapp.util.Resource;
import pe.edu.usat.incidentapp.util.SlaHelper;
import pe.edu.usat.incidentapp.viewmodel.MapaViewModel;

/**
 * Mapa de sucursales (marcadores) y, para el Administrador, la posición de los técnicos.
 * El Técnico puede compartir su ubicación actual.
 */
public class MapaFragment extends Fragment implements OnMapReadyCallback {

    private FragmentMapaBinding binding;
    private MapaViewModel viewModel;
    private GoogleMap mapa;

    //El encuadre de las sucursales se aplica cuando el mapa terminó de dibujarse
    private boolean mapaCargado;
    private CameraUpdate encuadrePendiente;

    //Marcadores actuales (se reemplazan al recargar)
    private final List<Marker> marcadoresSucursales = new ArrayList<>();
    private final List<Marker> marcadoresTecnicos = new ArrayList<>();

    private final ActivityResultLauncher<String[]> permisoUbicacion =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permisos -> {
                if (tienePermisoUbicacion()) {
                    obtenerYCompartirUbicacion();
                } else {
                    mostrarMensaje("Se necesita permiso de ubicación para compartirla", true);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMapaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MapaViewModel.class);

        binding.fabCompartirUbicacion.setVisibility(viewModel.esTecnico() ? View.VISIBLE : View.GONE);
        binding.leyendaTecnico.setVisibility(viewModel.esAdministrador() ? View.VISIBLE : View.GONE);
        binding.fabCompartirUbicacion.setOnClickListener(v -> compartirUbicacion());

        //El mapa se carga de forma asíncrona y avisa en onMapReady
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapa);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapa = googleMap;
        mapa.getUiSettings().setZoomControlsEnabled(true);
        mapa.setOnMapLoadedCallback(() -> {
            mapaCargado = true;
            if (mapa != null && encuadrePendiente != null) {
                mapa.moveCamera(encuadrePendiente);
                encuadrePendiente = null;
            }
        });
        if (tienePermisoUbicacion()) {
            activarMiUbicacion();
        }

        viewModel.getSucursales().observe(getViewLifecycleOwner(), this::mostrarSucursales);
        viewModel.getTecnicos().observe(getViewLifecycleOwner(), this::mostrarTecnicos);
        viewModel.getUbicacionCompartida().observe(getViewLifecycleOwner(), this::mostrarResultadoUbicacion);
        viewModel.cargar();
    }

    // ===================== MARCADORES =====================

    private void mostrarSucursales(Resource<List<Sucursal>> resultado) {
        if (resultado == null || binding == null) {
            return;
        }
        binding.progressMapa.setVisibility(
                resultado.getEstado() == Resource.Estado.LOADING ? View.VISIBLE : View.GONE);
        if (resultado.getEstado() == Resource.Estado.ERROR) {
            mostrarMensaje(resultado.getMensaje(), true);
        }
        List<Sucursal> sucursales = resultado.getData();
        if (sucursales == null || sucursales.isEmpty()) {
            return;
        }

        for (Marker marcador : marcadoresSucursales) {
            marcador.remove();
        }
        marcadoresSucursales.clear();

        //Al tocar el marcador se ve: nombre, dirección e incidentes abiertos
        LatLngBounds.Builder limites = new LatLngBounds.Builder();
        for (Sucursal s : sucursales) {
            LatLng posicion = new LatLng(s.getLatitud(), s.getLongitud());
            Marker marcador = mapa.addMarker(new MarkerOptions()
                    .position(posicion)
                    .title(s.getNombre())
                    .snippet(s.getDireccion() + " · " + s.getIncidentesAbiertos() + " incidentes abiertos")
                    .icon(BitmapDescriptorFactory.defaultMarker(s.getIncidentesAbiertos() > 0
                            ? BitmapDescriptorFactory.HUE_RED : BitmapDescriptorFactory.HUE_GREEN)));
            marcadoresSucursales.add(marcador);
            limites.include(posicion);
        }

        //Encuadrar todas las sucursales
        if (sucursales.size() == 1) {
            encuadrar(CameraUpdateFactory.newLatLngZoom(marcadoresSucursales.get(0).getPosition(), 15));
        } else {
            encuadrar(CameraUpdateFactory.newLatLngBounds(limites.build(), 120));
        }
    }

    //newLatLngBounds necesita el tamaño final del mapa: si aún no termina de cargar, se aplica en onMapLoaded
    private void encuadrar(CameraUpdate encuadre) {
        if (mapaCargado) {
            mapa.moveCamera(encuadre);
        } else {
            encuadrePendiente = encuadre;
        }
    }

    //Solo Administrador: posición actual de cada técnico con su incidente activo
    private void mostrarTecnicos(Resource<List<Tecnico>> resultado) {
        if (resultado == null || resultado.getData() == null) {
            return;
        }
        for (Marker marcador : marcadoresTecnicos) {
            marcador.remove();
        }
        marcadoresTecnicos.clear();

        for (Tecnico t : resultado.getData()) {
            if (!t.tieneUbicacion()) {
                continue;
            }
            String incidente = t.getIncidenteActivo() != null ? t.getIncidenteActivo() : "Sin incidente activo";
            Marker marcador = mapa.addMarker(new MarkerOptions()
                    .position(new LatLng(t.getLatitud(), t.getLongitud()))
                    .title("Técnico: " + t.getNombre())
                    .snippet(incidente + " · " + SlaHelper.fechaLegible(t.getFechaUbicacion()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            marcadoresTecnicos.add(marcador);
        }
    }

    // ===================== UBICACIÓN DEL TÉCNICO =====================

    private void compartirUbicacion() {
        if (tienePermisoUbicacion()) {
            obtenerYCompartirUbicacion();
        } else {
            permisoUbicacion.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    @SuppressLint("MissingPermission") //El permiso se valida antes de llamar a este método
    private void obtenerYCompartirUbicacion() {
        activarMiUbicacion();
        binding.fabCompartirUbicacion.setEnabled(false);

        FusedLocationProviderClient ubicacion = LocationServices.getFusedLocationProviderClient(requireActivity());
        ubicacion.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        binding.fabCompartirUbicacion.setEnabled(true);
                        mostrarMensaje("No se pudo obtener la ubicación. Active el GPS e intente nuevamente", true);
                        return;
                    }
                    LatLng posicion = new LatLng(location.getLatitude(), location.getLongitude());
                    mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(posicion, 15));
                    viewModel.compartirUbicacion(location.getLatitude(), location.getLongitude());
                })
                .addOnFailureListener(e -> {
                    binding.fabCompartirUbicacion.setEnabled(true);
                    mostrarMensaje("No se pudo obtener la ubicación", true);
                });
    }

    private void mostrarResultadoUbicacion(Resource<String> resultado) {
        if (resultado == null || binding == null || resultado.getEstado() == Resource.Estado.LOADING) {
            return;
        }
        binding.fabCompartirUbicacion.setEnabled(true);
        mostrarMensaje(resultado.getEstado() == Resource.Estado.SUCCESS
                ? resultado.getData() : resultado.getMensaje(), resultado.getEstado() == Resource.Estado.ERROR);
        viewModel.ubicacionAtendida();
    }

    @SuppressLint("MissingPermission")
    private void activarMiUbicacion() {
        if (mapa != null && tienePermisoUbicacion()) {
            mapa.setMyLocationEnabled(true); //Punto azul con la posición actual
        }
    }

    private boolean tienePermisoUbicacion() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        Snackbar snackbar = Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(),
                error ? R.color.estado_error : R.color.estado_resuelto));
        snackbar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        marcadoresSucursales.clear();
        marcadoresTecnicos.clear();
        mapa = null;
        mapaCargado = false;
        encuadrePendiente = null;
        binding = null;
    }
}
