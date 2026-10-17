package com.example.proyecto_grupo_3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.proyecto_grupo_3.adapter.EmpresaAdapter;
import com.example.proyecto_grupo_3.api.ApiResponse;
import com.example.proyecto_grupo_3.api.RetrofitClient;
import com.example.proyecto_grupo_3.databinding.FragmentSeleccionEmpresaBinding;
import com.example.proyecto_grupo_3.model.Empresa;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SeleccionEmpresaFragment extends Fragment {

    private FragmentSeleccionEmpresaBinding binding;
    private EmpresaAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentSeleccionEmpresaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvEmpresas.setLayoutManager(new LinearLayoutManager(requireContext()));

        binding.btnContinuar.setOnClickListener(v -> onContinuarClicked());

        fetchEmpresasFromApi();
    }

    private void fetchEmpresasFromApi() {
        showLoading(true);

        RetrofitClient.getInstance().getApiService().getEmpresas().enqueue(new Callback<ApiResponse<List<Empresa>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Empresa>>> call, @NonNull Response<ApiResponse<List<Empresa>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Empresa>> apiResponse = response.body();
                    if (apiResponse.getCode() == 1 && apiResponse.getData() != null && !apiResponse.getData().isEmpty()) {
                        displayEmpresas(apiResponse.getData());
                        return;
                    }
                }
                fallbackToMockData();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Empresa>>> call, @NonNull Throwable t) {
                fallbackToMockData();
            }
        });
    }

    private void displayEmpresas(List<Empresa> empresas) {
        showLoading(false);

        // Ensure EPSEL or the first option is selected by default
        boolean hasSelected = false;
        for (Empresa e : empresas) {
            if ("EPSEL".equalsIgnoreCase(e.getNombre())) {
                e.setSelected(true);
                hasSelected = true;
            } else {
                e.setSelected(false);
            }
        }

        if (!hasSelected && !empresas.isEmpty()) {
            empresas.get(0).setSelected(true);
        }

        adapter = new EmpresaAdapter(empresas, empresa -> {
            // Company selected
        });
        binding.rvEmpresas.setAdapter(adapter);
    }

    private void fallbackToMockData() {
        showLoading(false);
        List<Empresa> mockList = loadMockEmpresas();
        displayEmpresas(mockList);
    }

    private void showLoading(boolean isLoading) {
        if (binding == null) return;
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.rvEmpresas.setVisibility(View.GONE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.rvEmpresas.setVisibility(View.VISIBLE);
        }
    }

    private List<Empresa> loadMockEmpresas() {
        List<Empresa> lista = new ArrayList<>();

        lista.add(new Empresa(
                1,
                "EPSEL",
                "Empresa Prestadora de Servicios de Saneamiento.",
                "20123456789",
                250,
                true,
                R.drawable.ic_drop_water,
                true // EPSEL selected initially
        ));

        lista.add(new Empresa(
                2,
                "Empresa 2",
                "Servicios Generales.",
                "20123456780",
                80,
                true,
                R.drawable.ic_wrench_services,
                false
        ));

        lista.add(new Empresa(
                3,
                "Empresa 3",
                "Soluciones Tecnológicas.",
                "20123456781",
                120,
                true,
                R.drawable.ic_tech_chip,
                false
        ));

        lista.add(new Empresa(
                4,
                "Empresa 4",
                "Servicios Ambientales.",
                "20123456782",
                45,
                true,
                R.drawable.ic_leaf_eco,
                false
        ));

        lista.add(new Empresa(
                5,
                "Empresa 5",
                "Industria y Comercio.",
                "20123456783",
                300,
                true,
                R.drawable.ic_factory_store,
                false
        ));

        return lista;
    }

    private void onContinuarClicked() {
        if (adapter == null) return;
        Empresa selectedEmpresa = adapter.getSelectedEmpresa();

        if (selectedEmpresa == null) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.error_select_company),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putInt("empresa_id", selectedEmpresa.getId());
        bundle.putString("empresa_nombre", selectedEmpresa.getNombre());
        bundle.putString("empresa_desc", selectedEmpresa.getDescripcion());
        bundle.putInt("empresa_icon", selectedEmpresa.getIconResId());

        NavHostFragment.findNavController(this)
                .navigate(R.id.action_seleccionEmpresaFragment_to_loginFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
