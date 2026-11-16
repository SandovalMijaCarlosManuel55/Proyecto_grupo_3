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

import pe.edu.usat.incidentapp.R;
import pe.edu.usat.incidentapp.databinding.FragmentPerfilBinding;
import pe.edu.usat.incidentapp.databinding.ItemDatoBinding;
import pe.edu.usat.incidentapp.ui.MainActivity;
import pe.edu.usat.incidentapp.viewmodel.PerfilViewModel;

/**
 * Perfil del usuario autenticado y botón de cierre de sesión.
 */
public class PerfilFragment extends Fragment {

    private FragmentPerfilBinding binding;
    private PerfilViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPerfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PerfilViewModel.class);

        binding.txtIniciales.setText(viewModel.getIniciales());
        binding.txtNombre.setText(viewModel.getNombre());
        binding.chipRol.setText(viewModel.getRol());

        mostrarDato(binding.datoCorreo, R.drawable.ic_enviar, "Correo electrónico", viewModel.getCorreo());
        mostrarDato(binding.datoEmpresa, R.drawable.ic_ubicacion, "Empresa", viewModel.getEmpresa());
        mostrarDato(binding.datoPermisos, R.drawable.ic_tecnico, "Qué puede hacer", viewModel.getPermisos());

        binding.btnMetricas.setVisibility(viewModel.esAdministrador() ? View.VISIBLE : View.GONE);
        binding.btnMetricas.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_perfil_to_metricas));

        //Elimina el token almacenado y redirige al Login (con confirmación)
        binding.btnCerrarSesion.setOnClickListener(v ->
                ((MainActivity) requireActivity()).confirmarCierreSesion());
    }

    private void mostrarDato(ItemDatoBinding dato, int icono, String etiqueta, String valor) {
        dato.imgIcono.setImageResource(icono);
        dato.txtEtiqueta.setText(etiqueta);
        dato.txtValor.setText(valor != null ? valor : "-");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
