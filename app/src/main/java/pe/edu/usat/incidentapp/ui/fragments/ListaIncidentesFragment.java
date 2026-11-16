package pe.edu.usat.incidentapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import pe.edu.usat.incidentapp.R;
import pe.edu.usat.incidentapp.databinding.FragmentListaIncidentesBinding;
import pe.edu.usat.incidentapp.model.Incidente;
import pe.edu.usat.incidentapp.ui.MainActivity;
import pe.edu.usat.incidentapp.ui.adapters.IncidenteAdapter;
import pe.edu.usat.incidentapp.util.Resource;
import pe.edu.usat.incidentapp.util.SessionManager;
import pe.edu.usat.incidentapp.viewmodel.ListaIncidentesViewModel;

public class ListaIncidentesFragment extends Fragment {

    private FragmentListaIncidentesBinding binding;
    private ListaIncidentesViewModel viewModel;
    private IncidenteAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentListaIncidentesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ListaIncidentesViewModel.class);

        configurarSegunRol();

        //Una columna en celular, dos en tablet
        int columnas = getResources().getInteger(R.integer.columnas_lista);
        adapter = new IncidenteAdapter(this::abrirDetalle);
        binding.rvIncidentes.setLayoutManager(new GridLayoutManager(requireContext(), columnas));
        binding.rvIncidentes.setAdapter(adapter);

        binding.swipeIncidentes.setOnRefreshListener(() -> viewModel.cargar());
        binding.btnReintentar.setOnClickListener(v -> viewModel.cargar());
        binding.fabNuevo.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_lista_to_nuevo));

        //Filtros por estado y prioridad
        binding.chipGroupEstado.setOnCheckedStateChangeListener((group, ids) ->
                viewModel.setFiltroEstado(textoChip(group, ids)));
        binding.chipGroupPrioridad.setOnCheckedStateChangeListener((group, ids) ->
                viewModel.setFiltroPrioridad(textoChip(group, ids)));

        viewModel.getIncidentes().observe(getViewLifecycleOwner(), this::mostrarResultado);

        //Al sincronizar los pendientes, recargar para mostrarlos con su número de ticket
        viewModel.getEventoSincronizacion().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje != null) {
                viewModel.cargar();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        //Refrescar al volver (por ejemplo después de registrar un incidente)
        viewModel.cargar();
    }

    //Administrador ve todos, Técnico sus asignados y Solicitante sus reportes
    private void configurarSegunRol() {
        String rol = SessionManager.getInstance().getRol();
        if (SessionManager.ROL_ADMINISTRADOR.equals(rol)) {
            binding.txtTituloLista.setText("Todos los incidentes");
            binding.fabNuevo.setVisibility(View.VISIBLE);
        } else if (SessionManager.ROL_TECNICO.equals(rol)) {
            binding.txtTituloLista.setText("Mis incidentes asignados");
            binding.fabNuevo.setVisibility(View.GONE);
        } else {
            binding.txtTituloLista.setText("Mis reportes");
            binding.fabNuevo.setVisibility(View.VISIBLE);
        }
    }

    //SafeArgs: enviar el id del incidente al detalle
    private void abrirDetalle(Incidente incidente) {
        if (incidente.isPendiente()) {
            Snackbar.make(binding.getRoot(),
                    "Este incidente aún no se envía al servidor (se sincronizará al recuperar la conexión)",
                    Snackbar.LENGTH_LONG).show();
            return;
        }
        NavHostFragment.findNavController(this).navigate(
                ListaIncidentesFragmentDirections.actionListaToDetalle(incidente.getId()));
    }

    //El chip "Todos"/"Todas" no filtra (null)
    private String textoChip(ChipGroup group, List<Integer> ids) {
        if (ids.isEmpty()) {
            return null;
        }
        Chip chip = group.findViewById(ids.get(0));
        String texto = chip.getText().toString();
        return texto.startsWith("Tod") ? null : texto;
    }

    private void mostrarResultado(Resource<List<Incidente>> resultado) {
        if (resultado == null || binding == null) {
            return;
        }
        List<Incidente> lista = resultado.getData();
        boolean hayDatos = lista != null && !lista.isEmpty();

        switch (resultado.getEstado()) {
            case LOADING:
                //Mientras carga se muestran los datos de Room (si existen)
                binding.swipeIncidentes.setRefreshing(hayDatos);
                binding.progressLista.setVisibility(hayDatos ? View.GONE : View.VISIBLE);
                binding.layoutError.setVisibility(View.GONE);
                binding.layoutVacio.setVisibility(View.GONE);
                adapter.actualizarLista(lista);
                break;

            case SUCCESS:
                binding.swipeIncidentes.setRefreshing(false);
                binding.progressLista.setVisibility(View.GONE);
                binding.layoutError.setVisibility(View.GONE);
                binding.layoutVacio.setVisibility(hayDatos ? View.GONE : View.VISIBLE);
                adapter.actualizarLista(lista);
                break;

            case ERROR:
                binding.swipeIncidentes.setRefreshing(false);
                binding.progressLista.setVisibility(View.GONE);
                binding.layoutVacio.setVisibility(View.GONE);

                if (resultado.getCodigo() == Resource.CODIGO_NO_AUTORIZADO) {
                    //Token vencido o inválido: volver al login
                    Snackbar.make(binding.getRoot(), "Su sesión expiró, ingrese nuevamente", Snackbar.LENGTH_LONG).show();
                    ((MainActivity) requireActivity()).irAlLogin();
                    return;
                }

                adapter.actualizarLista(lista);
                if (hayDatos) {
                    //Room sigue mostrando los últimos datos disponibles
                    binding.layoutError.setVisibility(View.GONE);
                    Snackbar.make(binding.getRoot(),
                                    resultado.getMensaje() + ". Mostrando datos guardados.", Snackbar.LENGTH_LONG)
                            .setAction("Reintentar", v -> viewModel.cargar())
                            .show();
                } else {
                    binding.txtError.setText(resultado.getMensaje());
                    binding.layoutError.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
