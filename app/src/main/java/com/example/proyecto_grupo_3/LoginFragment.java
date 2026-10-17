package com.example.proyecto_grupo_3;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.proyecto_grupo_3.api.RetrofitClient;
import com.example.proyecto_grupo_3.databinding.FragmentLoginBinding;
import com.example.proyecto_grupo_3.model.AuthRequest;
import com.example.proyecto_grupo_3.model.AuthResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private int empresaId = -1;
    private String empresaNombre = "";
    private String empresaDesc = "";
    private int empresaIcon = R.drawable.ic_drop_water;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Extraer argumentos recibidos de la selección de empresa
        if (getArguments() != null) {
            empresaId = getArguments().getInt("empresa_id", -1);
            empresaNombre = getArguments().getString("empresa_nombre", "EPSEL");
            empresaDesc = getArguments().getString("empresa_desc", "Empresa Prestadora de Servicios de Saneamiento.");
            empresaIcon = getArguments().getInt("empresa_icon", R.drawable.ic_drop_water);

            binding.tvSelectedCompanyName.setText(empresaNombre);
            binding.tvSelectedCompanyDesc.setText(empresaDesc);
            binding.imgSelectedCompanyIcon.setImageResource(empresaIcon);
        }

        // Botón regresar
        binding.btnBackLogin.setOnClickListener(v ->
                NavHostFragment.findNavController(LoginFragment.this).navigateUp()
        );

        // Botón Iniciar Sesión
        binding.btnIniciarSesion.setOnClickListener(v -> intentarLogin());

        // Enlace ¿Olvidaste tu contraseña?
        binding.tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(requireContext(), getString(R.string.forgot_password_msg), Toast.LENGTH_SHORT).show()
        );
    }

    private void intentarLogin() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

        // Validaciones locales
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(requireContext(), getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), getString(R.string.error_invalid_email), Toast.LENGTH_SHORT).show();
            return;
        }

        realizarAutenticacion(email, password);
    }

    private void realizarAutenticacion(String email, String password) {
        setLoading(true);

        // Estructura esperada por Flask-JWT: {"email": "...", "password": "..."}
        AuthRequest authRequest = new AuthRequest(email, password);

        RetrofitClient.getInstance().getApiService().autenticar(authRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null && !TextUtils.isEmpty(response.body().getAccessToken())) {
                    String token = response.body().getAccessToken();

                    Toast.makeText(requireContext(), getString(R.string.login_success), Toast.LENGTH_SHORT).show();

                    // Navegar al menú principal pasando la información
                    Bundle bundle = new Bundle();
                    bundle.putString("usuario_nombre", email.split("@")[0]);
                    bundle.putString("usuario_rol", "Solicitante");
                    bundle.putString("empresa_nombre", empresaNombre);
                    bundle.putString("auth_token", token);

                    NavHostFragment.findNavController(LoginFragment.this)
                            .navigate(R.id.action_loginFragment_to_menuPrincipalFragment, bundle);
                } else {
                    Toast.makeText(requireContext(), getString(R.string.error_auth_failed), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(requireContext(), "Error de conexión con el servidor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (binding == null) return;

        if (isLoading) {
            binding.progressBarLogin.setVisibility(View.VISIBLE);
            binding.btnIniciarSesion.setEnabled(false);
            binding.btnIniciarSesion.setText("");
        } else {
            binding.progressBarLogin.setVisibility(View.GONE);
            binding.btnIniciarSesion.setEnabled(true);
            binding.btnIniciarSesion.setText(getString(R.string.btn_iniciar_sesion));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
