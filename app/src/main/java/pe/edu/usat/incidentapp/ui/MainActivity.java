package pe.edu.usat.incidentapp.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import pe.edu.usat.incidentapp.R;
import pe.edu.usat.incidentapp.databinding.ActivityMainBinding;
import pe.edu.usat.incidentapp.service.IncidentMessagingService;
import pe.edu.usat.incidentapp.ui.fragments.DetalleIncidenteFragmentArgs;
import pe.edu.usat.incidentapp.viewmodel.MainViewModel;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private MainViewModel viewModel;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    //Android 13+: pedir permiso para mostrar las notificaciones push
    private final ActivityResultLauncher<String> permisoNotificaciones =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
            return insets;
        });

        setSupportActionBar(binding.toolbar);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        //Login y Lista son pantallas de primer nivel (sin flecha de regreso)
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.loginFragment, R.id.listaIncidentesFragment).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean enLogin = destination.getId() == R.id.loginFragment;
            binding.appBar.setVisibility(enLogin ? View.GONE : View.VISIBLE);
            if (!enLogin) {
                //Mostrar la empresa, el usuario y su rol (leídos del JWT)
                if (destination.getId() == R.id.listaIncidentesFragment) {
                    binding.toolbar.setTitle(viewModel.getEmpresa());
                }
                binding.toolbar.setSubtitle(viewModel.getNombre() + " · " + viewModel.getRol());
            }
            invalidateOptionsMenu();
        });

        //Mensaje cuando se sincronizan incidentes registrados sin conexión
        viewModel.getEventoSincronizacion().observe(this, mensaje -> {
            if (mensaje != null) {
                Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
            }
        });

        monitorearConexion();
        pedirPermisoNotificaciones();

        if (savedInstanceState == null) {
            //Sesión guardada: asegurar que el API tenga el token FCM de este celular
            viewModel.registrarTokenFcm();
            abrirDesdeNotificacion(getIntent());
        }
    }

    //La app ya estaba abierta y se tocó una notificación
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        abrirDesdeNotificacion(intent);
    }

    //Al tocar la notificación push se abre directamente el detalle del incidente
    private void abrirDesdeNotificacion(Intent intent) {
        String idIncidente = intent.getStringExtra(IncidentMessagingService.EXTRA_ID_INCIDENTE);
        if (idIncidente == null || !viewModel.haySesionActiva()) {
            return;
        }
        intent.removeExtra(IncidentMessagingService.EXTRA_ID_INCIDENTE);

        //Dejar la lista debajo del detalle para que "atrás" regrese a ella
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.loginFragment) {
            navController.navigate(R.id.action_login_to_lista);
        }
        Bundle argumentos = new DetalleIncidenteFragmentArgs.Builder(Integer.parseInt(idIncidente))
                .build().toBundle();
        navController.navigate(R.id.detalleIncidenteFragment, argumentos);
    }

    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    //Banner de modo offline y sincronización automática al recuperar la conexión
    private void monitorearConexion() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        actualizarBanner(hayConexion());

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> actualizarBanner(true));
                viewModel.conexionRecuperada();
            }

            @Override
            public void onLost(@NonNull Network network) {
                //Se perdió la red por defecto. Si hay otra (ej. WiFi -> datos), onAvailable vuelve a ocultar el banner.
                //No se consulta hayConexion() aquí porque en este instante la red perdida aún puede figurar como activa
                runOnUiThread(() -> actualizarBanner(false));
            }
        };
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    private boolean hayConexion() {
        Network red = connectivityManager.getActiveNetwork();
        if (red == null) {
            return false;
        }
        NetworkCapabilities capacidades = connectivityManager.getNetworkCapabilities(red);
        return capacidades != null && capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void actualizarBanner(boolean conectado) {
        binding.bannerOffline.setVisibility(conectado ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //Menú según el rol del JWT: el Administrador ve el menú completo (incluye Reportes y métricas)
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int destino = navController.getCurrentDestination() != null
                ? navController.getCurrentDestination().getId() : 0;
        boolean enLogin = destino == R.id.loginFragment;
        boolean admin = viewModel.esAdministrador();

        menu.findItem(R.id.action_cerrar_sesion).setVisible(!enLogin);
        menu.findItem(R.id.mapaFragment).setVisible(!enLogin && destino != R.id.mapaFragment);
        menu.findItem(R.id.perfilFragment).setVisible(!enLogin && destino != R.id.perfilFragment);
        menu.findItem(R.id.metricasFragment).setVisible(!enLogin && admin && destino != R.id.metricasFragment);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_cerrar_sesion) {
            confirmarCierreSesion();
            return true;
        }
        //Opciones "Mapa", "Métricas" y "Perfil": navegan al destino con el mismo id en el nav_graph
        return NavigationUI.onNavDestinationSelected(item, navController) || super.onOptionsItemSelected(item);
    }

    //También lo usa el botón "Cerrar sesión" del Perfil
    public void confirmarCierreSesion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Desea cerrar la sesión del usuario?")
                .setNegativeButton("No", null)
                .setPositiveButton("Sí", (dialog, which) -> irAlLogin())
                .show();
    }

    //Elimina el token almacenado y redirige al Login (también se usa cuando el token expira)
    public void irAlLogin() {
        viewModel.cerrarSesion();
        NavOptions opciones = new NavOptions.Builder()
                .setPopUpTo(navController.getGraph().getId(), true)
                .build();
        navController.navigate(R.id.loginFragment, null, opciones);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
}
