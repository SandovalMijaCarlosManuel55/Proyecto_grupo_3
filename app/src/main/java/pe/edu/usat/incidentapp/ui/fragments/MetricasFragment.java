package pe.edu.usat.incidentapp.ui.fragments;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;
import java.util.Map;

import pe.edu.usat.incidentapp.R;
import pe.edu.usat.incidentapp.databinding.FragmentMetricasBinding;
import pe.edu.usat.incidentapp.databinding.ItemMetricaBinding;
import pe.edu.usat.incidentapp.model.Metricas;
import pe.edu.usat.incidentapp.ui.MainActivity;
import pe.edu.usat.incidentapp.util.ColoresHelper;
import pe.edu.usat.incidentapp.util.Resource;
import pe.edu.usat.incidentapp.util.SlaHelper;
import pe.edu.usat.incidentapp.viewmodel.MetricasViewModel;

/**
 * Reportes y métricas (solo Administrador): incidentes por estado y prioridad,
 * semáforo SLA, tiempo de resolución y desempeño de cada técnico.
 */
public class MetricasFragment extends Fragment {

    private FragmentMetricasBinding binding;
    private MetricasViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMetricasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MetricasViewModel.class);

        //Pantalla exclusiva del Administrador
        if (!viewModel.esAdministrador()) {
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        binding.swipeMetricas.setOnRefreshListener(() -> viewModel.cargar());
        binding.btnReintentar.setOnClickListener(v -> viewModel.cargar());
        viewModel.getMetricas().observe(getViewLifecycleOwner(), this::mostrarResultado);
        viewModel.cargar();
    }

    private void mostrarResultado(Resource<Metricas> resultado) {
        if (resultado == null || binding == null) {
            return;
        }
        Metricas metricas = resultado.getData();
        boolean hayDatos = metricas != null;
        if (hayDatos) {
            mostrarMetricas(metricas);
        }
        binding.layoutContenido.setVisibility(hayDatos ? View.VISIBLE : View.GONE);

        switch (resultado.getEstado()) {
            case LOADING:
                binding.swipeMetricas.setRefreshing(hayDatos);
                binding.progressMetricas.setVisibility(hayDatos ? View.GONE : View.VISIBLE);
                binding.layoutError.setVisibility(View.GONE);
                break;
            case SUCCESS:
                binding.swipeMetricas.setRefreshing(false);
                binding.progressMetricas.setVisibility(View.GONE);
                binding.layoutError.setVisibility(View.GONE);
                break;
            case ERROR:
                binding.swipeMetricas.setRefreshing(false);
                binding.progressMetricas.setVisibility(View.GONE);
                if (resultado.getCodigo() == Resource.CODIGO_NO_AUTORIZADO) {
                    ((MainActivity) requireActivity()).irAlLogin();
                    return;
                }
                if (hayDatos) {
                    Snackbar.make(binding.getRoot(), resultado.getMensaje() + ". Mostrando datos guardados.",
                            Snackbar.LENGTH_LONG).setAction("Reintentar", v -> viewModel.cargar()).show();
                } else {
                    binding.txtError.setText(resultado.getMensaje());
                    binding.layoutError.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    private void mostrarMetricas(Metricas m) {
        binding.txtResumen.setText(viewModel.getEmpresa() + " · " + m.getTotal() + " incidentes registrados");
        binding.txtTotal.setText(String.valueOf(m.getTotal()));
        binding.txtActivos.setText(String.valueOf(m.getActivos()));
        binding.txtSlaVencidos.setText(String.valueOf(m.getSlaRojo()));
        binding.txtSinAsignar.setText(String.valueOf(m.getSinAsignar()));

        //Semáforo SLA (Alta 4h, Media 24h, Baja 72h)
        binding.contenedorSla.removeAllViews();
        int activos = m.getActivos();
        agregarFila(binding.contenedorSla, "En tiempo", m.getSlaVerde(), activos,
                ColoresHelper.semaforo(SlaHelper.Semaforo.VERDE), null);
        agregarFila(binding.contenedorSla, "Por vencer (menos del 25%)", m.getSlaAmarillo(), activos,
                ColoresHelper.semaforo(SlaHelper.Semaforo.AMARILLO), null);
        agregarFila(binding.contenedorSla, "Vencidos (escalar)", m.getSlaRojo(), activos,
                ColoresHelper.semaforo(SlaHelper.Semaforo.ROJO), null);

        //Tiempo de resolución
        binding.contenedorResolucion.removeAllViews();
        agregarFila(binding.contenedorResolucion, "Resueltos dentro del SLA",
                m.getPorcentajeDentroSla(), 100, R.color.estado_resuelto,
                m.getResueltos() + " incidentes resueltos · tiempo promedio "
                        + Metricas.duracion(m.getPromedioResolucionMs()), "%");

        //Por estado
        binding.contenedorEstado.removeAllViews();
        for (Map.Entry<String, Integer> e : m.getPorEstado().entrySet()) {
            agregarFila(binding.contenedorEstado, e.getKey(), e.getValue(), m.getTotal(),
                    ColoresHelper.estado(e.getKey(), false), null);
        }

        //Por prioridad
        binding.contenedorPrioridad.removeAllViews();
        for (Map.Entry<String, Integer> e : m.getPorPrioridad().entrySet()) {
            agregarFila(binding.contenedorPrioridad, e.getKey(), e.getValue(), m.getTotal(),
                    ColoresHelper.prioridad(e.getKey()), null);
        }

        //Desempeño por técnico: resueltos sobre asignados
        binding.contenedorTecnicos.removeAllViews();
        if (m.getTecnicos().isEmpty()) {
            agregarFila(binding.contenedorTecnicos, "Aún no hay incidentes asignados", 0, 1,
                    R.color.estado_cerrado, null);
        }
        for (Metricas.DesempenoTecnico t : m.getTecnicos()) {
            String detalle = String.format(Locale.getDefault(),
                    "%d asignados · %d activos · resolución promedio %s",
                    t.getAsignados(), t.getActivos(), Metricas.duracion(t.getPromedioResolucionMs()));
            agregarFila(binding.contenedorTecnicos, t.getNombre(), t.getResueltos(), t.getAsignados(),
                    R.color.estado_resuelto, detalle, " resueltos");
        }
    }

    private void agregarFila(LinearLayout contenedor, String etiqueta, int valor, int total, int color, String detalle) {
        agregarFila(contenedor, etiqueta, valor, total, color, detalle, "");
    }

    //Fila: etiqueta, valor y barra con la proporción valor / total
    private void agregarFila(LinearLayout contenedor, String etiqueta, int valor, int total,
                             int color, String detalle, String sufijo) {
        ItemMetricaBinding fila = ItemMetricaBinding.inflate(getLayoutInflater(), contenedor, false);
        fila.txtEtiqueta.setText(etiqueta);
        fila.txtValor.setText(valor + sufijo);
        fila.barra.setProgress(total > 0 ? Math.round(valor * 100f / total) : 0);
        int colorReal = ContextCompat.getColor(requireContext(), color);
        fila.barra.setIndicatorColor(colorReal);
        fila.barra.setTrackColor(ColorStateList.valueOf(colorReal).withAlpha(40).getDefaultColor());
        if (detalle != null) {
            fila.txtDetalle.setText(detalle);
            fila.txtDetalle.setVisibility(View.VISIBLE);
        }
        contenedor.addView(fila.getRoot());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
