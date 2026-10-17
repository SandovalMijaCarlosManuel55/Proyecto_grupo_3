package com.example.proyecto_grupo_3.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_grupo_3.R;
import com.example.proyecto_grupo_3.databinding.ItemEmpresaBinding;
import com.example.proyecto_grupo_3.model.Empresa;

import java.util.List;

public class EmpresaAdapter extends RecyclerView.Adapter<EmpresaAdapter.EmpresaViewHolder> {

    public interface OnEmpresaSelectedListener {
        void onEmpresaSelected(Empresa empresa);
    }

    private final List<Empresa> empresaList;
    private final OnEmpresaSelectedListener listener;
    private int selectedPosition = -1;

    public EmpresaAdapter(List<Empresa> empresaList, OnEmpresaSelectedListener listener) {
        this.empresaList = empresaList;
        this.listener = listener;

        // Initialize selected position if any item is marked as selected
        for (int i = 0; i < empresaList.size(); i++) {
            if (empresaList.get(i).isSelected()) {
                selectedPosition = i;
                break;
            }
        }
    }

    @NonNull
    @Override
    public EmpresaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEmpresaBinding binding = ItemEmpresaBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new EmpresaViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EmpresaViewHolder holder, int position) {
        Empresa empresa = empresaList.get(position);
        boolean isSelected = position == selectedPosition;

        holder.binding.tvCompanyName.setText(empresa.getNombre());
        holder.binding.tvCompanyDescription.setText(empresa.getDescripcion());
        holder.binding.imgCompanyIcon.setImageResource(empresa.getIconResId());
        holder.binding.radioButtonCompany.setChecked(isSelected);

        if (isSelected) {
            holder.binding.containerCompanyItem.setBackgroundResource(R.drawable.bg_company_item_selected);
        } else {
            holder.binding.containerCompanyItem.setBackgroundResource(R.drawable.bg_company_item_normal);
        }

        holder.binding.containerCompanyItem.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();

            if (selectedPosition != RecyclerView.NO_POSITION) {
                // Update selection states in model
                if (previousPosition >= 0 && previousPosition < empresaList.size()) {
                    empresaList.get(previousPosition).setSelected(false);
                }
                empresaList.get(selectedPosition).setSelected(true);

                notifyItemChanged(previousPosition);
                notifyItemChanged(selectedPosition);

                if (listener != null) {
                    listener.onEmpresaSelected(empresaList.get(selectedPosition));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return empresaList != null ? empresaList.size() : 0;
    }

    public Empresa getSelectedEmpresa() {
        if (selectedPosition >= 0 && selectedPosition < empresaList.size()) {
            return empresaList.get(selectedPosition);
        }
        return null;
    }

    public static class EmpresaViewHolder extends RecyclerView.ViewHolder {
        final ItemEmpresaBinding binding;

        public EmpresaViewHolder(@NonNull ItemEmpresaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
