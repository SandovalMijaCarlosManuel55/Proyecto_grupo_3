package pe.edu.usat.incidentapp.ui.fragments;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import pe.edu.usat.incidentapp.R;
import pe.edu.usat.incidentapp.databinding.FragmentLoginBinding;
import pe.edu.usat.incidentapp.util.Resource;
import pe.edu.usat.incidentapp.viewmodel.LoginViewModel;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        //Si el JWT almacenado sigue vigente, ingresar directamente
        if (viewModel.haySesionActiva()) {
            irAlInicio();
            return;
        }

        binding.txtCorreo.setText(viewModel.getUltimoCorreo());

        binding.btnIngresar.setOnClickListener(v -> iniciarSesion());

        viewModel.getResultadoLogin().observe(getViewLifecycleOwner(), resultado -> {
            if (resultado == null) {
                return;
            }
            switch (resultado.getEstado()) {
                case LOADING:
                    mostrarCargando(true);
                    break;
                case SUCCESS:
                    mostrarCargando(false);
                    irAlInicio();
                    break;
                case ERROR:
                    mostrarCargando(false);
                    mostrarError(resultado);
                    break;
            }
        });
    }

    private void iniciarSesion() {
        String correo = binding.txtCorreo.getText().toString().trim();
        String password = binding.txtPassword.getText().toString();

        binding.tilCorreo.setError(null);
        binding.tilPassword.setError(null);

        //Validar campos vacíos antes de llamar a la API
        boolean valido = true;
        if (correo.isEmpty()) {
            binding.tilCorreo.setError("Ingrese su correo electrónico");
            valido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.tilCorreo.setError("Correo electrónico no válido");
            valido = false;
        }
        if (password.isEmpty()) {
            binding.tilPassword.setError("Ingrese su contraseña");
            valido = false;
        }
        if (!valido) {
            return;
        }

        viewModel.iniciarSesion(correo, password);
    }

    private void mostrarCargando(boolean cargando) {
        binding.progressLogin.setVisibility(cargando ? View.VISIBLE : View.GONE);
        binding.btnIngresar.setEnabled(!cargando);
        binding.txtCorreo.setEnabled(!cargando);
        binding.txtPassword.setEnabled(!cargando);
    }

    private void mostrarError(Resource<?> resultado) {
        Snackbar snackbar = Snackbar.make(binding.getRoot(), resultado.getMensaje(), Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(requireContext().getColor(R.color.estado_error));
        snackbar.show();
        if (resultado.getCodigo() == Resource.CODIGO_NO_AUTORIZADO) {
            binding.tilPassword.setError("Correo o contraseña incorrectos");
        }
    }

    //Navegar al inicio. La lista se adapta según el rol del JWT.
    //Solo si el Login sigue siendo la pantalla actual: al abrir la app desde una notificación push,
    //MainActivity ya navegó al detalle del incidente y no se debe volver a navegar
    private void irAlInicio() {
        NavController navController = NavHostFragment.findNavController(this);
        NavDestination actual = navController.getCurrentDestination();
        if (actual != null && actual.getId() == R.id.loginFragment) {
            navController.navigate(R.id.action_login_to_lista);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
