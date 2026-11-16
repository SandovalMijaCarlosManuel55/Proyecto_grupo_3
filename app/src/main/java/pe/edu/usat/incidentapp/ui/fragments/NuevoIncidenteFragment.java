package pe.edu.usat.incidentapp.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pe.edu.usat.incidentapp.R;
import pe.edu.usat.incidentapp.databinding.FragmentNuevoIncidenteBinding;
import pe.edu.usat.incidentapp.databinding.ItemFotoBinding;
import pe.edu.usat.incidentapp.model.Activo;
import pe.edu.usat.incidentapp.model.IncidenteRegistrado;
import pe.edu.usat.incidentapp.model.Sucursal;
import pe.edu.usat.incidentapp.ui.MainActivity;
import pe.edu.usat.incidentapp.util.ImagenHelper;
import pe.edu.usat.incidentapp.util.Resource;
import pe.edu.usat.incidentapp.viewmodel.NuevoIncidenteViewModel;

public class NuevoIncidenteFragment extends Fragment {
    private static final String[] CATEGORIAS = {"Red", "Hardware", "Software", "Acceso", "Impresora", "Otro"};
    private static final String KEY_URI_CAMARA = "uri_camara";

    private FragmentNuevoIncidenteBinding binding;
    private NuevoIncidenteViewModel viewModel;

    private final List<Sucursal> sucursales = new ArrayList<>();
    private Sucursal sucursalSeleccionada;
    private Uri uriCamara;

    //Lector de QR (ZXing): la cámara lee el código del activo de TI
    private final ActivityResultLauncher<ScanOptions> lectorQr =
            registerForActivityResult(new ScanContract(), resultado -> {
                if (resultado.getContents() != null) {
                    viewModel.codigoEscaneado(resultado.getContents().trim());
                }
            });

    private final ActivityResultLauncher<Uri> tomarFoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), exito -> {
                if (exito && uriCamara != null) {
                    procesarFoto(uriCamara);
                }
            });

    private final ActivityResultLauncher<String> elegirFoto =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    procesarFoto(uri);
                }
            });

    private final ActivityResultLauncher<String> permisoCamara =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    abrirCamara();
                } else {
                    mostrarMensaje("Se necesita permiso de cámara para tomar la foto", true);
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            uriCamara = savedInstanceState.getParcelable(KEY_URI_CAMARA);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNuevoIncidenteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(NuevoIncidenteViewModel.class);

        binding.cboCategoria.setSimpleItems(CATEGORIAS);
        binding.cboSucursal.setOnItemClickListener((parent, v, posicion, id) ->
                sucursalSeleccionada = (Sucursal) parent.getItemAtPosition(posicion));

        binding.btnEscanear.setOnClickListener(v -> escanearEquipo());
        binding.btnAgregarFoto.setOnClickListener(v -> elegirOrigenFoto());
        binding.btnEnviar.setOnClickListener(v -> enviar());

        observarSucursales();
        observarActivo();
        observarFotos();
        observarRegistro();

        if (savedInstanceState == null) {
            viewModel.cargarSucursales();
        }
    }

    private void observarSucursales() {
        viewModel.getSucursales().observe(getViewLifecycleOwner(), resultado -> {
            if (resultado == null || resultado.getData() == null) {
                return;
            }
            sucursales.clear();
            sucursales.addAll(resultado.getData());
            binding.cboSucursal.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1, sucursales));

            if (resultado.getEstado() == Resource.Estado.ERROR && sucursales.isEmpty()) {
                mostrarMensaje("No se pudieron cargar las sucursales: " + resultado.getMensaje(), true);
            }
        });
    }

    private void observarActivo() {
        viewModel.getActivo().observe(getViewLifecycleOwner(), resultado -> {
            if (resultado == null) {
                return;
            }
            switch (resultado.getEstado()) {
                case LOADING:
                    binding.txtActivo.setText("Buscando el activo " + viewModel.getCodigoQr() + "...");
                    break;
                case SUCCESS:
                    Activo activo = resultado.getData();
                    viewModel.setNombreActivo(activo.getNombre());
                    binding.txtActivo.setText(activo.getCodigoQr() + " · " + activo.getNombre());
                    seleccionarSucursal(activo.getIdSucursal());
                    break;
                case ERROR:
                    if (resultado.getCodigo() == 0) {
                        //Sin conexión: se conserva el código y el API lo valida al sincronizar
                        binding.txtActivo.setText(viewModel.getCodigoQr() + " (se validará al sincronizar)");
                    } else {
                        viewModel.limpiarActivo();
                        binding.txtActivo.setText("Ningún equipo escaneado");
                        mostrarMensaje(resultado.getMensaje(), true);
                    }
                    break;
            }
        });
    }

    //El sistema autocompleta la sucursal donde está el activo escaneado
    private void seleccionarSucursal(int idSucursal) {
        for (Sucursal sucursal : sucursales) {
            if (sucursal.getId() == idSucursal) {
                sucursalSeleccionada = sucursal;
                binding.cboSucursal.setText(sucursal.getNombre(), false);
                binding.tilSucursal.setError(null);
                break;
            }
        }
    }

    //Miniaturas de las fotos antes de enviar (máximo 2)
    private void observarFotos() {
        viewModel.getFotos().observe(getViewLifecycleOwner(), fotos -> {
            binding.layoutFotos.removeAllViews();
            for (int i = 0; i < fotos.size(); i++) {
                final int posicion = i;
                ItemFotoBinding item = ItemFotoBinding.inflate(getLayoutInflater(), binding.layoutFotos, false);
                item.imgFoto.setImageBitmap(BitmapFactory.decodeFile(fotos.get(i)));
                item.btnQuitar.setOnClickListener(v -> viewModel.quitarFoto(posicion));
                binding.layoutFotos.addView(item.getRoot());
            }
            binding.layoutFotos.setVisibility(fotos.isEmpty() ? View.GONE : View.VISIBLE);
            binding.btnAgregarFoto.setEnabled(viewModel.puedeAgregarFoto());
            binding.btnAgregarFoto.setText(String.format(Locale.getDefault(),
                    "Agregar foto (%d/%d)", fotos.size(), NuevoIncidenteViewModel.MAXIMO_FOTOS));
        });
    }

    private void observarRegistro() {
        viewModel.getRegistro().observe(getViewLifecycleOwner(), resultado -> {
            if (resultado == null) {
                return;
            }
            switch (resultado.getEstado()) {
                case LOADING:
                    mostrarEnviando(true);
                    break;
                case SUCCESS:
                    mostrarEnviando(false);
                    IncidenteRegistrado registrado = resultado.getData();
                    if (registrado.isPendiente()) {
                        mostrarMensajeGlobal("Sin conexión: el incidente se guardó como Pendiente de sincronización");
                    } else {
                        mostrarMensajeGlobal(String.format(Locale.US,
                                "Incidente #INC-%04d registrado correctamente", registrado.getId()));
                    }
                    NavHostFragment.findNavController(this).popBackStack();
                    break;
                case ERROR:
                    mostrarEnviando(false);
                    if (resultado.getCodigo() == Resource.CODIGO_NO_AUTORIZADO) {
                        mostrarMensajeGlobal("Su sesión expiró, ingrese nuevamente");
                        ((MainActivity) requireActivity()).irAlLogin();
                        return;
                    }
                    mostrarMensaje("No se pudo registrar: " + resultado.getMensaje(), true);
                    break;
            }
        });
    }

    private void escanearEquipo() {
        ScanOptions opciones = new ScanOptions();
        opciones.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        opciones.setPrompt("Enfoque el código QR del equipo");
        opciones.setBeepEnabled(true);
        opciones.setOrientationLocked(false);
        lectorQr.launch(opciones);
    }

    private void elegirOrigenFoto() {
        if (!viewModel.puedeAgregarFoto()) {
            mostrarMensaje("Solo se permiten " + NuevoIncidenteViewModel.MAXIMO_FOTOS + " fotos", true);
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Agregar foto")
                .setItems(new String[]{"Cámara", "Galería"}, (dialog, opcion) -> {
                    if (opcion == 0) {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            abrirCamara();
                        } else {
                            permisoCamara.launch(Manifest.permission.CAMERA);
                        }
                    } else {
                        elegirFoto.launch("image/*");
                    }
                })
                .show();
    }

    private void abrirCamara() {
        uriCamara = ImagenHelper.crearUriCamara(requireContext());
        tomarFoto.launch(uriCamara);
    }

    //Reduce la imagen en segundo plano y la agrega al formulario
    private void procesarFoto(Uri uri) {
        android.content.Context context = requireContext().getApplicationContext();
        Handler principal = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                String ruta = ImagenHelper.comprimir(context, uri);
                principal.post(() -> viewModel.agregarFoto(ruta));
            } catch (Exception e) {
                principal.post(() -> mostrarMensaje("No se pudo cargar la foto", true));
            }
        }).start();
    }

    private void enviar() {
        String titulo = binding.txtTitulo.getText().toString().trim();
        String descripcion = binding.txtDescripcion.getText().toString().trim();
        String categoria = binding.cboCategoria.getText().toString();
        String prioridad = null;
        int idChip = binding.chipGroupPrioridad.getCheckedChipId();
        if (idChip != View.NO_ID) {
            prioridad = ((Chip) binding.chipGroupPrioridad.findViewById(idChip)).getText().toString();
        }

        //Validar que todos los campos obligatorios estén completos
        boolean valido = true;
        binding.tilTitulo.setError(titulo.isEmpty() ? "Ingrese el título del incidente" : null);
        binding.tilDescripcion.setError(descripcion.isEmpty() ? "Ingrese la descripción detallada" : null);
        binding.tilCategoria.setError(categoria.isEmpty() ? "Seleccione la categoría" : null);
        binding.tilSucursal.setError(sucursalSeleccionada == null ? "Seleccione la sucursal" : null);
        binding.txtErrorPrioridad.setVisibility(prioridad == null ? View.VISIBLE : View.GONE);

        if (titulo.isEmpty() || descripcion.isEmpty() || categoria.isEmpty()
                || sucursalSeleccionada == null || prioridad == null) {
            valido = false;
        }
        if (!valido) {
            mostrarMensaje("Complete todos los campos obligatorios", true);
            return;
        }

        viewModel.registrar(titulo, descripcion, categoria, prioridad, sucursalSeleccionada);
    }

    private void mostrarEnviando(boolean enviando) {
        binding.progressEnviar.setVisibility(enviando ? View.VISIBLE : View.GONE);
        binding.btnEnviar.setEnabled(!enviando);
        binding.btnEnviar.setText(enviando ? "Enviando..." : "Enviar");
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        if (binding == null) {
            return;
        }
        Snackbar snackbar = Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG);
        if (error) {
            snackbar.setBackgroundTint(requireContext().getColor(R.color.estado_error));
        }
        snackbar.show();
    }

    //Snackbar sobre la actividad para que se vea al volver a la lista
    private void mostrarMensajeGlobal(String mensaje) {
        Snackbar.make(requireActivity().findViewById(android.R.id.content), mensaje, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (uriCamara != null) {
            outState.putParcelable(KEY_URI_CAMARA, uriCamara);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
