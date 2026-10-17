package com.example.proyecto_grupo_3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.proyecto_grupo_3.databinding.FragmentMenuPrincipalBinding;

public class MenuPrincipalFragment extends Fragment {

    private FragmentMenuPrincipalBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentMenuPrincipalBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String userName = getArguments().getString("usuario_nombre", "Usuario");
            String userRole = getArguments().getString("usuario_rol", "Solicitante");
            String companyName = getArguments().getString("empresa_nombre", "");

            binding.tvWelcomeUser.setText(getString(R.string.welcome_user, userName));
            binding.tvUserRole.setText(getString(R.string.role_label, userRole));
            binding.tvCompanyNameMenu.setText(getString(R.string.company_label, companyName));
        }

        binding.btnLogout.setOnClickListener(v ->
                NavHostFragment.findNavController(MenuPrincipalFragment.this)
                        .navigate(R.id.action_menuPrincipalFragment_to_seleccionEmpresaFragment)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
