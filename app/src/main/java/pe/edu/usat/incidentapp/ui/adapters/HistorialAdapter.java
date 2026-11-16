package pe.edu.usat.incidentapp.ui.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import pe.edu.usat.incidentapp.databinding.ItemHistorialBinding;
import pe.edu.usat.incidentapp.model.Historial;
import pe.edu.usat.incidentapp.util.ColoresHelper;
import pe.edu.usat.incidentapp.util.SlaHelper;

/**
 * Línea de tiempo del incidente: estado anterior → estado nuevo, usuario, fecha y comentario.
 */
public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.ViewHolder> {
    private List<Historial> historial = new ArrayList<>();

    public void actualizarLista(List<Historial> lista) {
        this.historial = lista != null ? lista : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistorialBinding binding = ItemHistorialBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //La línea vertical no se dibuja debajo del último elemento
        holder.mostrarDatos(historial.get(position), position == historial.size() - 1);
    }

    @Override
    public int getItemCount() {
        return historial.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistorialBinding binding;

        public ViewHolder(ItemHistorialBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void mostrarDatos(Historial h, boolean ultimo) {
            if (!h.esCambioDeEstado()) {
                binding.txtCambio.setText("Comentario · " + h.getEstadoNuevo());
            } else if (h.getEstadoAnterior() == null) {
                binding.txtCambio.setText(h.getEstadoNuevo());
            } else {
                binding.txtCambio.setText(h.getEstadoAnterior() + " → " + h.getEstadoNuevo());
            }
            binding.txtComentario.setText(h.getComentario());
            binding.txtUsuarioFecha.setText(h.getUsuario() + " · " + SlaHelper.fechaLegible(h.getFechaCambio()));
            binding.lineaTiempo.setVisibility(ultimo ? View.INVISIBLE : View.VISIBLE);

            //Punto de la línea de tiempo con el color del estado
            binding.puntoTiempo.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(
                    binding.getRoot().getContext(), ColoresHelper.estado(h.getEstadoNuevo(), false))));
        }
    }
}
