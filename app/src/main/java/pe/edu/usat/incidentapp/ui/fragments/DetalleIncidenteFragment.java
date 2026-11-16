package pe.edu.usat.incidentapp.ui.fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.List;
import java.util.Locale;

import pe.edu.usat.incidentapp.R;
import pe.edu.usat.incidentapp.databinding.DialogCambiarEstadoBinding;
import pe.edu.usat.incidentapp.databinding.DialogCambiarPrioridadBinding;
import pe.edu.usat.incidentapp.databinding.FragmentDetalleIncidenteBinding;
import pe.edu.usat.incidentapp.databinding.ItemDatoBinding;
import pe.edu.usat.incidentapp.model.IncidenteDetalle;
import pe.edu.usat.incidentapp.model.Tecnico;
import pe.edu.usat.incidentapp.ui.MainActivity;
import pe.edu.usat.incidentapp.ui.adapters.HistorialAdapter;
import pe.edu.usat.incidentapp.util.ColoresHelper;
import pe.edu.usat.incidentapp.util.ImagenHelper;
import pe.edu.usat.incidentapp.util.Resource;
import pe.edu.usat.incidentapp.util.SlaHelper;
import pe.edu.usat.incidentapp.viewmodel.DetalleIncidenteViewModel;

/**
 * Detalle del incidente: datos, fotos, historial (línea de tiempo) y acciones según el rol.
 */
public class DetalleIncidenteFragment extends Fragment {

    private FragmentDetalleIncidenteBinding binding;
    private DetalleIncidenteViewModel viewModel;
    private final HistorialAdapter historialAdapter = new HistorialAdapter();

    //Último detalle mostrado (se usa en los botones de acción)
    private IncidenteDetalle detalle;

    //Lector de QR (ZXing): el técnico verifica que atiende el equipo correcto
    private final ActivityResultLauncher<ScanOptions> lectorQr =
            registerForActivityResult(new ScanContract(), resultado -> {
                if (resultado.getContents() != null) {
                    verificarEquipo(resultado.getContents().trim());
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDetalleIncidenteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DetalleIncidenteViewModel.class);

        //SafeArgs: id del incidente enviado desde la lista (o desde la notificación push)
        int idIncidente = DetalleIncidenteFragmentArgs.fromBundle(getArguments()).getIdIncidente();

        binding.rvHistorial.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistorial.setAdapter(historialAdapter);

        binding.btnReintentar.setOnClickListener(v -> viewModel.cargar());
        binding.btnCambiarEstado.setOnClickListener(v -> mostrarDialogoEstado());
        binding.btnCambiarPrioridad.setOnClickListener(v -> mostrarDialogoPrioridad());
        binding.btnAsignar.setOnClickListener(v -> viewModel.cargarTecnicos());
        binding.btnVerificarQr.setOnClickListener(v -> escanearEquipo());
        binding.btnComoLlegar.setOnClickListener(v -> abrirComoLlegar());
        binding.tilComentario.setEndIconOnClickListener(v -> enviarComentario());

        viewModel.getDetalle().observe(getViewLifecycleOwner(), this::mostrarResultado);
        viewModel.getAccion().observe(getViewLifecycleOwner(), this::mostrarAccion);
        viewModel.getTecnicos().observe(getViewLifecycleOwner(), this::mostrarTecnicos);

        viewModel.iniciar(idIncidente);
    }

    // ===================== MOSTRAR DATOS =====================

    private void mostrarResultado(Resource<IncidenteDetalle> resultado) {
        if (resultado == null || binding == null) {
            return;
        }
        boolean hayDatos = resultado.getData() != null;
        if (hayDatos) {
            mostrarDetalle(resultado.getData());
        }

        switch (resultado.getEstado()) {
            case LOADING:
                binding.progressDetalle.setVisibility(View.VISIBLE);
                binding.layoutError.setVisibility(View.GONE);
                break;
            case SUCCESS:
                binding.progressDetalle.setVisibility(View.GONE);
                binding.layoutError.setVisibility(View.GONE);
                break;
            case ERROR:
                binding.progressDetalle.setVisibility(View.GONE);
                if (resultado.getCodigo() == Resource.CODIGO_NO_AUTORIZADO) {
                    mostrarMensaje("Su sesión expiró, ingrese nuevamente", true);
                    ((MainActivity) requireActivity()).irAlLogin();
                    return;
                }
                if (hayDatos) {
                    //Sin conexión: Room sigue mostrando los últimos datos guardados
                    mostrarMensaje(resultado.getMensaje() + ". Mostrando datos guardados.", true);
                } else {
                    binding.txtError.setText(resultado.getMensaje());
                    binding.layoutError.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    private void mostrarDetalle(IncidenteDetalle d) {
        detalle = d;
        Context context = requireContext();

        binding.txtTicket.setText(String.format(Locale.US, "#INC-%04d", d.getId()));
        binding.txtTitulo.setText(d.getTitulo());
        binding.txtDescripcion.setText(d.getDescripcion());

        binding.chipPrioridad.setText(d.getPrioridad());
        binding.chipPrioridad.setChipBackgroundColor(ColorStateList.valueOf(
                ContextCompat.getColor(context, ColoresHelper.prioridad(d.getPrioridad()))));

        binding.txtEstado.setText(d.getEstado());
        binding.txtEstado.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, ColoresHelper.estado(d.getEstado(), false))));

        //Semáforo SLA calculado en la app (Alta 4h, Media 24h, Baja 72h)
        SlaHelper.Semaforo semaforo = SlaHelper.calcular(d.getPrioridad(), d.getEstado(), d.getFechaRegistro());
        binding.indicadorSla.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, ColoresHelper.semaforo(semaforo))));
        binding.txtSla.setText(SlaHelper.descripcion(d.getPrioridad(), d.getEstado(), d.getFechaRegistro()));

        mostrarDatos(d);
        mostrarFotos(d.getFotos());
        historialAdapter.actualizarLista(d.getHistorial());
        configurarAcciones(d);
    }

    //Filas de datos: ícono + etiqueta + valor (layout item_dato reutilizado con include)
    private void mostrarDatos(IncidenteDetalle d) {
        String sucursal = d.getSucursal() + (d.getDireccion() != null ? "\n" + d.getDireccion() : "");
        String equipo = d.getActivo() != null ? d.getActivo() + " · " + d.getCodigoQr() : "No registrado";

        mostrarDato(binding.datoCategoria, R.drawable.ic_categoria, "Categoría", d.getCategoria());
        mostrarDato(binding.datoSucursal, R.drawable.ic_ubicacion, "Sucursal", sucursal);
        mostrarDato(binding.datoEquipo, R.drawable.ic_equipo, "Equipo afectado", equipo);
        mostrarDato(binding.datoSolicitante, R.drawable.ic_persona, "Solicitante", d.getSolicitante());
        mostrarDato(binding.datoTecnico, R.drawable.ic_tecnico, "Técnico asignado",
                d.getTecnico() != null ? d.getTecnico() : "Sin asignar");
        mostrarDato(binding.datoRegistro, R.drawable.ic_reloj, "Registrado",
                SlaHelper.fechaLegible(d.getFechaRegistro()));

        boolean resuelto = d.getFechaResolucion() != null;
        binding.datoResolucion.getRoot().setVisibility(resuelto ? View.VISIBLE : View.GONE);
        if (resuelto) {
            mostrarDato(binding.datoResolucion, R.drawable.ic_resuelto, "Resuelto",
                    SlaHelper.fechaLegible(d.getFechaResolucion()));
        }
    }

    private void mostrarDato(ItemDatoBinding dato, int icono, String etiqueta, String valor) {
        dato.imgIcono.setImageResource(icono);
        dato.txtEtiqueta.setText(etiqueta);
        dato.txtValor.setText(valor);
    }

    private void mostrarFotos(List<String> fotos) {
        binding.cardFotos.setVisibility(fotos.isEmpty() ? View.GONE : View.VISIBLE);

        ImageView[] imagenes = {binding.imgFoto1, binding.imgFoto2};
        for (int i = 0; i < imagenes.length; i++) {
            if (i < fotos.size()) {
                String foto = fotos.get(i);
                imagenes[i].setVisibility(View.VISIBLE);
                Glide.with(this).load(ImagenHelper.origenFoto(foto)).centerCrop()
                        .error(R.drawable.ic_vacio).into(imagenes[i]);
                imagenes[i].setOnClickListener(v -> verFotoCompleta(foto));
            } else {
                imagenes[i].setVisibility(View.INVISIBLE);
            }
        }
    }

    //Foto en tamaño completo al tocarla
    private void verFotoCompleta(String foto) {
        ImageView imagen = new ImageView(requireContext());
        imagen.setAdjustViewBounds(true);
        Glide.with(this).load(ImagenHelper.origenFoto(foto)).fitCenter().into(imagen);
        new MaterialAlertDialogBuilder(requireContext())
                .setView(imagen)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    //Botones visibles según el rol del JWT
    private void configurarAcciones(IncidenteDetalle d) {
        boolean admin = viewModel.esAdministrador();
        boolean tecnico = viewModel.esTecnico();
        boolean cerrado = "Cerrado".equals(d.getEstado());

        //El técnico solo ve el botón mientras pueda avanzar (Abierto -> En proceso -> Resuelto)
        boolean tecnicoPuedeAvanzar = tecnico && viewModel.siguienteEstadoTecnico(d.getEstado()) != null;
        binding.btnCambiarEstado.setVisibility(admin || tecnicoPuedeAvanzar ? View.VISIBLE : View.GONE);
        binding.btnCambiarPrioridad.setVisibility(admin && !cerrado ? View.VISIBLE : View.GONE);
        binding.btnAsignar.setVisibility(admin && !cerrado ? View.VISIBLE : View.GONE);
        binding.btnAsignar.setText(d.getTecnico() != null ? "Reasignar técnico" : "Asignar técnico");
        binding.btnVerificarQr.setVisibility(tecnico && d.getCodigoQr() != null ? View.VISIBLE : View.GONE);
        binding.btnComoLlegar.setVisibility((admin || tecnico) && d.tieneUbicacion() ? View.VISIBLE : View.GONE);

        //El Solicitante no gestiona: se oculta toda la tarjeta de acciones
        binding.cardAcciones.setVisibility(admin || tecnico ? View.VISIBLE : View.GONE);
    }

    // ===================== CAMBIO DE ESTADO =====================

    private void mostrarDialogoEstado() {
        if (detalle == null) {
            return;
        }
        DialogCambiarEstadoBinding dialogo = DialogCambiarEstadoBinding.inflate(getLayoutInflater());
        String[] estados = viewModel.estadosPermitidos(detalle.getEstado());
        dialogo.cboEstado.setSimpleItems(estados);
        if (estados.length == 1) {
            //Técnico: el único paso siguiente queda seleccionado
            dialogo.cboEstado.setText(estados[0], false);
        }

        AlertDialog alerta = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cambiar estado")
                .setView(dialogo.getRoot())
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Confirmar", null)
                .show();

        //Validación local antes de llamar al API: estado y comentario obligatorios
        alerta.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String estado = dialogo.cboEstado.getText().toString();
            String comentario = dialogo.txtComentarioEstado.getText().toString().trim();

            dialogo.tilEstado.setError(estado.isEmpty() ? "Seleccione el nuevo estado" : null);
            dialogo.tilComentarioEstado.setError(comentario.isEmpty() ? "El comentario es obligatorio" : null);
            if (estado.isEmpty() || comentario.isEmpty()) {
                return;
            }
            viewModel.cambiarEstado(estado, comentario);
            alerta.dismiss();
        });
    }

    // ===================== CAMBIO DE PRIORIDAD (ADMINISTRADOR) =====================

    private void mostrarDialogoPrioridad() {
        if (detalle == null) {
            return;
        }
        DialogCambiarPrioridadBinding dialogo = DialogCambiarPrioridadBinding.inflate(getLayoutInflater());
        dialogo.cboPrioridad.setSimpleItems(viewModel.prioridadesPermitidas(detalle.getPrioridad()));
        dialogo.txtPrioridadActual.setText("Prioridad actual: " + detalle.getPrioridad());

        AlertDialog alerta = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cambiar prioridad")
                .setView(dialogo.getRoot())
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Confirmar", null)
                .show();

        //Validación local: la nueva prioridad es obligatoria (el motivo es opcional)
        alerta.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String prioridad = dialogo.cboPrioridad.getText().toString();
            String motivo = dialogo.txtMotivo.getText().toString().trim();
            dialogo.tilPrioridad.setError(prioridad.isEmpty() ? "Seleccione la nueva prioridad" : null);
            if (prioridad.isEmpty()) {
                return;
            }
            viewModel.cambiarPrioridad(prioridad, motivo);
            alerta.dismiss();
        });
    }

    // ===================== ASIGNAR TÉCNICO (ADMINISTRADOR) =====================

    private void mostrarTecnicos(Resource<List<Tecnico>> resultado) {
        if (resultado == null) {
            return;
        }
        if (resultado.getEstado() == Resource.Estado.ERROR) {
            mostrarMensaje(resultado.getMensaje(), true);
            viewModel.tecnicosAtendidos();
            return;
        }
        if (resultado.getEstado() != Resource.Estado.SUCCESS) {
            return;
        }
        List<Tecnico> tecnicos = resultado.getData();
        viewModel.tecnicosAtendidos();
        if (tecnicos.isEmpty()) {
            mostrarMensaje("No hay técnicos registrados en su empresa", true);
            return;
        }

        String[] nombres = new String[tecnicos.size()];
        for (int i = 0; i < tecnicos.size(); i++) {
            nombres[i] = tecnicos.get(i).toString();
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Asignar técnico")
                .setItems(nombres, (dialog, posicion) -> viewModel.asignarTecnico(tecnicos.get(posicion)))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ===================== COMENTARIO =====================

    private void enviarComentario() {
        String comentario = binding.txtComentario.getText().toString().trim();
        if (comentario.isEmpty()) {
            binding.tilComentario.setError("Escriba un comentario");
            return;
        }
        binding.tilComentario.setError(null);
        viewModel.comentar(comentario);
    }

    //Resultado de cambiar estado, asignar o comentar
    private void mostrarAccion(Resource<String> resultado) {
        if (resultado == null || binding == null) {
            return;
        }
        boolean enviando = resultado.getEstado() == Resource.Estado.LOADING;
        binding.progressDetalle.setVisibility(enviando ? View.VISIBLE : View.GONE);
        binding.btnCambiarEstado.setEnabled(!enviando);
        binding.btnCambiarPrioridad.setEnabled(!enviando);
        binding.btnAsignar.setEnabled(!enviando);
        binding.tilComentario.setEnabled(!enviando);

        if (resultado.getEstado() == Resource.Estado.SUCCESS) {
            mostrarMensaje(resultado.getData(), false);
            binding.txtComentario.setText("");
            viewModel.accionAtendida();
            viewModel.cargar(); //Refrescar estado e historial
        } else if (resultado.getEstado() == Resource.Estado.ERROR) {
            if (resultado.getCodigo() == Resource.CODIGO_NO_AUTORIZADO) {
                ((MainActivity) requireActivity()).irAlLogin();
                return;
            }
            mostrarMensaje(resultado.getMensaje(), true);
            viewModel.accionAtendida();
        }
    }

    // ===================== QR Y CÓMO LLEGAR =====================

    private void escanearEquipo() {
        ScanOptions opciones = new ScanOptions();
        opciones.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        opciones.setPrompt("Escanee el QR del equipo que está atendiendo");
        opciones.setBeepEnabled(true);
        opciones.setOrientationLocked(false);
        lectorQr.launch(opciones);
    }

    //Verifica que el activo escaneado coincide con el registrado en el incidente
    private void verificarEquipo(String codigoEscaneado) {
        if (detalle == null) {
            return;
        }
        if (viewModel.equipoCoincide(codigoEscaneado)) {
            mostrarMensaje("Equipo verificado: " + detalle.getActivo() + " (" + codigoEscaneado + ")", false);
        } else {
            mostrarMensaje("El equipo escaneado (" + codigoEscaneado + ") no coincide con el registrado ("
                    + detalle.getCodigoQr() + ")", true);
        }
    }

    //Abre Google Maps con indicaciones desde la ubicación actual hasta la sucursal
    private void abrirComoLlegar() {
        if (detalle == null || !detalle.tieneUbicacion()) {
            return;
        }
        String destino = detalle.getLatitud() + "," + detalle.getLongitud();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + destino));
        intent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            //Sin la app de Google Maps: abrir las indicaciones en el navegador
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + destino)));
        }
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        Snackbar snackbar = Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(requireContext(),
                error ? R.color.estado_error : R.color.estado_resuelto));
        snackbar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
