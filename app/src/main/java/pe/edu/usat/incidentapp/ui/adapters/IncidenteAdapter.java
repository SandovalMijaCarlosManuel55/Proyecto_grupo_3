package pe.edu.usat.incidentapp.ui.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bumptech.glide.Glide;

import pe.edu.usat.incidentapp.R;
import pe.edu.usat.incidentapp.databinding.ItemIncidenteBinding;
import pe.edu.usat.incidentapp.model.Incidente;
import pe.edu.usat.incidentapp.util.ColoresHelper;
import pe.edu.usat.incidentapp.util.ImagenHelper;
import pe.edu.usat.incidentapp.util.SlaHelper;

public class IncidenteAdapter extends RecyclerView.Adapter<IncidenteAdapter.ViewHolder> {
    //Acción al tocar una card (abrir el detalle)
    public interface OnIncidenteClickListener {
        void onIncidenteClick(Incidente incidente);
    }

    private List<Incidente> listaIncidentes = new ArrayList<>();
    private final OnIncidenteClickListener listener;

    public IncidenteAdapter(OnIncidenteClickListener listener) {
        this.listener = listener;
    }

    public void actualizarLista(List<Incidente> lista) {
        this.listaIncidentes = lista != null ? lista : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemIncidenteBinding binding = ItemIncidenteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Incidente incidente = listaIncidentes.get(position);
        holder.mostrarDatos(incidente);
        holder.itemView.setOnClickListener(v -> listener.onIncidenteClick(incidente));
    }

    @Override
    public int getItemCount() {
        return listaIncidentes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemIncidenteBinding binding;

        public ViewHolder(ItemIncidenteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void mostrarDatos(Incidente incidente) {
            Context context = binding.getRoot().getContext();

            //Número de ticket
            if (incidente.isPendiente()) {
                binding.txtTicket.setText("Sin ticket");
            } else {
                binding.txtTicket.setText(String.format(Locale.US, "#INC-%04d", incidente.getId()));
            }

            mostrarFoto(incidente.getFoto());

            binding.txtTitulo.setText(incidente.getTitulo());
            binding.txtCategoria.setText(incidente.getCategoria() + " · " + incidente.getSucursal());
            binding.txtFecha.setText("Registrado: " + SlaHelper.fechaLegible(incidente.getFechaRegistro()));

            String tecnico = incidente.getTecnico();
            binding.txtTecnico.setText("Técnico: " + (tecnico != null ? tecnico : "Sin asignar"));

            //Prioridad con color: rojo=Alta, naranja=Media, verde=Baja
            binding.chipPrioridad.setText(incidente.getPrioridad());
            binding.chipPrioridad.setChipBackgroundColor(ColorStateList.valueOf(
                    ContextCompat.getColor(context, ColoresHelper.prioridad(incidente.getPrioridad()))));

            //Estado actual
            binding.txtEstado.setText(incidente.getEstado());
            binding.txtEstado.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, ColoresHelper.estado(incidente.getEstado(), incidente.isPendiente()))));

            //Semáforo SLA (no aplica para incidentes aún no sincronizados)
            if (incidente.isPendiente()) {
                binding.layoutSla.setVisibility(View.GONE);
            } else {
                binding.layoutSla.setVisibility(View.VISIBLE);
                SlaHelper.Semaforo semaforo = SlaHelper.calcular(
                        incidente.getPrioridad(), incidente.getEstado(), incidente.getFechaRegistro());
                binding.indicadorSla.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, ColoresHelper.semaforo(semaforo))));
                binding.txtSla.setText(SlaHelper.descripcion(
                        incidente.getPrioridad(), incidente.getEstado(), incidente.getFechaRegistro()));
            }
        }

        private void mostrarFoto(String foto) {
            if (foto == null || foto.isEmpty()) {
                Glide.with(binding.imgFoto).clear(binding.imgFoto);
                binding.imgFoto.setVisibility(View.GONE);
                return;
            }
            binding.imgFoto.setVisibility(View.VISIBLE);

            Glide.with(binding.imgFoto)
                    .load(ImagenHelper.origenFoto(foto))
                    .centerCrop()
                    .error(R.drawable.ic_vacio)
                    .into(binding.imgFoto);
        }
    }
}
