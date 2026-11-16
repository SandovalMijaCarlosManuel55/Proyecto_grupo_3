package pe.edu.usat.incidentapp.repository;

import android.content.Context;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import pe.edu.usat.incidentapp.local.AppDatabase;
import pe.edu.usat.incidentapp.local.HistorialEntity;
import pe.edu.usat.incidentapp.local.IncidenteEntity;
import pe.edu.usat.incidentapp.local.SolicitudPendienteEntity;
import pe.edu.usat.incidentapp.local.SucursalEntity;
import pe.edu.usat.incidentapp.model.Activo;
import pe.edu.usat.incidentapp.model.ApiResponse;
import pe.edu.usat.incidentapp.model.AsignarRequest;
import pe.edu.usat.incidentapp.model.CambioEstadoRequest;
import pe.edu.usat.incidentapp.model.Historial;
import pe.edu.usat.incidentapp.model.Incidente;
import pe.edu.usat.incidentapp.model.IncidenteDetalle;
import pe.edu.usat.incidentapp.model.IncidenteRegistrado;
import pe.edu.usat.incidentapp.model.PrioridadRequest;
import pe.edu.usat.incidentapp.model.Sucursal;
import pe.edu.usat.incidentapp.model.Tecnico;
import pe.edu.usat.incidentapp.model.UbicacionRequest;
import pe.edu.usat.incidentapp.network.ApiService;
import pe.edu.usat.incidentapp.network.RetrofitClient;
import pe.edu.usat.incidentapp.util.ApiError;
import pe.edu.usat.incidentapp.util.Resource;
import pe.edu.usat.incidentapp.util.SessionManager;
import pe.edu.usat.incidentapp.util.SlaHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Coordina las dos fuentes de datos de incidentes:
 * primero Room (rápido, offline) y luego Retrofit (actualizado).
 * Contiene la lógica de sincronización de solicitudes pendientes.
 */
public class IncidenteRepository {
    private static final String SIN_CONEXION = "No se pudo conectar con el servidor";
    private static final MediaType TEXTO = MediaType.parse("text/plain");
    private static final MediaType IMAGEN = MediaType.parse("image/jpeg");

    private static IncidenteRepository instance;

    private final ApiService apiService;
    private final AppDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean sincronizando = new AtomicBoolean(false);

    //Mensaje que se muestra cuando se sincronizan incidentes registrados sin conexión
    private final MutableLiveData<String> eventoSincronizacion = new MutableLiveData<>();

    private IncidenteRepository(Context context) {
        apiService = RetrofitClient.createService();
        database = AppDatabase.getInstance(context);
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new IncidenteRepository(context.getApplicationContext());
        }
    }

    public static IncidenteRepository getInstance() {
        return instance;
    }

    public LiveData<String> getEventoSincronizacion() {
        return eventoSincronizacion;
    }

    // ===================== LISTADO =====================

    public void listar(String estado, String prioridad, MutableLiveData<Resource<List<Incidente>>> resultado) {
        executor.execute(() -> {
            //1. Mostrar primero los datos de Room mientras carga la respuesta del API
            List<Incidente> cache = leerCache(estado, prioridad);
            resultado.postValue(Resource.loading(cache));

            //2. Consultar el API
            apiService.getIncidentes(estado, prioridad).enqueue(new Callback<ApiResponse<List<Incidente>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Incidente>>> call,
                                       Response<ApiResponse<List<Incidente>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        List<Incidente> remotos = response.body().getData();
                        executor.execute(() -> {
                            //3. Actualizar Room con la respuesta y refrescar la lista
                            List<IncidenteEntity> entidades = new ArrayList<>();
                            for (Incidente incidente : remotos) {
                                entidades.add(IncidenteEntity.desdeModelo(incidente));
                            }
                            if (estado == null && prioridad == null) {
                                database.incidenteDao().reemplazar(entidades);
                            } else {
                                database.incidenteDao().insertar(entidades);
                            }

                            List<Incidente> lista = new ArrayList<>(leerPendientes(estado, prioridad));
                            lista.addAll(remotos);
                            resultado.postValue(Resource.success(lista));
                        });
                    } else {
                        //Si el API falla, Room sigue mostrando los últimos datos disponibles
                        resultado.postValue(Resource.error(ApiError.mensaje(response), cache, response.code()));
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Incidente>>> call, Throwable t) {
                    resultado.postValue(Resource.error(SIN_CONEXION, cache, 0));
                }
            });
        });
    }

    private List<Incidente> leerCache(String estado, String prioridad) {
        List<Incidente> lista = new ArrayList<>(leerPendientes(estado, prioridad));
        for (IncidenteEntity entity : database.incidenteDao().listar(estado, prioridad)) {
            lista.add(entity.aModelo());
        }
        return lista;
    }

    //Incidentes registrados sin conexión (se muestran al inicio de la lista del solicitante)
    private List<Incidente> leerPendientes(String estado, String prioridad) {
        List<Incidente> lista = new ArrayList<>();
        if (estado != null) {
            return lista;
        }
        int idUsuario = SessionManager.getInstance().getUsuarioId();
        for (SolicitudPendienteEntity s : database.solicitudPendienteDao().listarPorUsuario(idUsuario)) {
            if (prioridad != null && !prioridad.equals(s.prioridad)) {
                continue;
            }
            String estadoPendiente = SolicitudPendienteEntity.ESTADO_ERROR.equals(s.estado)
                    ? "Error al sincronizar" : "Pendiente de sincronización";
            Incidente incidente = new Incidente(-s.id, s.titulo, s.descripcion, s.categoria, s.prioridad,
                    estadoPendiente, s.fechaRegistro, SessionManager.getInstance().getNombre(),
                    null, s.sucursal, s.activo);
            incidente.setPendiente(true);
            lista.add(incidente);
        }
        return lista;
    }

    // ===================== REGISTRO =====================

    public void registrar(String titulo, String descripcion, String categoria, String prioridad,
                          Sucursal sucursal, String codigoQr, String nombreActivo, List<String> fotos,
                          MutableLiveData<Resource<IncidenteRegistrado>> resultado) {
        resultado.setValue(Resource.loading(null));

        Call<ApiResponse<IncidenteRegistrado>> call = crearLlamadaRegistro(
                titulo, descripcion, categoria, prioridad, sucursal.getId(), codigoQr, fotos);

        call.enqueue(new Callback<ApiResponse<IncidenteRegistrado>>() {
            @Override
            public void onResponse(Call<ApiResponse<IncidenteRegistrado>> call,
                                   Response<ApiResponse<IncidenteRegistrado>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    eliminarArchivos(fotos);
                    resultado.postValue(Resource.success(response.body().getData()));
                } else {
                    resultado.postValue(Resource.error(ApiError.mensaje(response), null, response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<IncidenteRegistrado>> call, Throwable t) {
                //Sin conexión: guardar en Room como pendiente para sincronizar luego
                executor.execute(() -> {
                    SolicitudPendienteEntity solicitud = new SolicitudPendienteEntity();
                    solicitud.idUsuario = SessionManager.getInstance().getUsuarioId();
                    solicitud.titulo = titulo;
                    solicitud.descripcion = descripcion;
                    solicitud.categoria = categoria;
                    solicitud.prioridad = prioridad;
                    solicitud.idSucursal = sucursal.getId();
                    solicitud.sucursal = sucursal.getNombre();
                    solicitud.codigoQr = codigoQr;
                    solicitud.activo = nombreActivo;
                    solicitud.fotos = TextUtils.join("|", fotos);
                    solicitud.fechaRegistro = SlaHelper.formatearFechaUtc(new Date());
                    solicitud.estado = SolicitudPendienteEntity.ESTADO_PENDIENTE;
                    database.solicitudPendienteDao().insertar(solicitud);

                    resultado.postValue(Resource.success(IncidenteRegistrado.pendiente()));
                });
            }
        });
    }

    private Call<ApiResponse<IncidenteRegistrado>> crearLlamadaRegistro(
            String titulo, String descripcion, String categoria, String prioridad,
            int idSucursal, String codigoQr, List<String> fotos) {

        List<MultipartBody.Part> partesFotos = new ArrayList<>();
        for (String ruta : fotos) {
            File archivo = new File(ruta);
            if (archivo.exists()) {
                partesFotos.add(MultipartBody.Part.createFormData(
                        "fotos", archivo.getName(), RequestBody.create(IMAGEN, archivo)));
            }
        }

        return apiService.registrarIncidente(
                texto(titulo),
                texto(descripcion),
                texto(categoria),
                texto(prioridad),
                texto(String.valueOf(idSucursal)),
                texto(codigoQr != null ? codigoQr : ""),
                partesFotos
        );
    }

    private RequestBody texto(String valor) {
        return RequestBody.create(TEXTO, valor);
    }

    private void eliminarArchivos(List<String> rutas) {
        for (String ruta : rutas) {
            File archivo = new File(ruta);
            if (archivo.exists()) {
                archivo.delete();
            }
        }
    }

    // ===================== SINCRONIZACIÓN =====================

    //Envía al API las solicitudes registradas sin conexión. Se llama al recuperar la red
    public void sincronizarPendientes() {
        if (!SessionManager.getInstance().haySesionActiva() || !sincronizando.compareAndSet(false, true)) {
            return;
        }
        executor.execute(() -> {
            int enviados = 0;
            try {
                int idUsuario = SessionManager.getInstance().getUsuarioId();
                for (SolicitudPendienteEntity s : database.solicitudPendienteDao().listarPorSincronizar(idUsuario)) {
                    List<String> fotos = new ArrayList<>();
                    if (s.fotos != null && !s.fotos.isEmpty()) {
                        for (String ruta : s.fotos.split("\\|")) {
                            fotos.add(ruta);
                        }
                    }
                    try {
                        Response<ApiResponse<IncidenteRegistrado>> response = crearLlamadaRegistro(
                                s.titulo, s.descripcion, s.categoria, s.prioridad,
                                s.idSucursal, s.codigoQr, fotos).execute();

                        if (response.isSuccessful()) {
                            database.solicitudPendienteDao().eliminar(s);
                            eliminarArchivos(fotos);
                            enviados++;
                        } else if (response.code() == Resource.CODIGO_NO_AUTORIZADO) {
                            break; //Token vencido: se reintenta cuando inicie sesión nuevamente
                        } else {
                            //El API rechazó la solicitud (datos inválidos): no se reintenta
                            s.estado = SolicitudPendienteEntity.ESTADO_ERROR;
                            s.mensajeError = ApiError.mensaje(response);
                            database.solicitudPendienteDao().actualizar(s);
                        }
                    } catch (IOException e) {
                        break; //Sigue sin conexión, se reintenta luego
                    }
                }
            } finally {
                sincronizando.set(false);
            }
            if (enviados > 0) {
                eventoSincronizacion.postValue(enviados == 1
                        ? "Se sincronizó 1 incidente registrado sin conexión"
                        : "Se sincronizaron " + enviados + " incidentes registrados sin conexión");
            }
        });
    }

    // ===================== DETALLE E HISTORIAL =====================

    public void obtenerDetalle(int id, MutableLiveData<Resource<IncidenteDetalle>> resultado) {
        executor.execute(() -> {
            //1. Mostrar primero lo guardado en Room (lista + historial)
            IncidenteDetalle cache = leerDetalleCache(id);
            resultado.postValue(Resource.loading(cache));

            //2. Consultar el API
            apiService.getIncidente(id).enqueue(new Callback<ApiResponse<IncidenteDetalle>>() {
                @Override
                public void onResponse(Call<ApiResponse<IncidenteDetalle>> call,
                                       Response<ApiResponse<IncidenteDetalle>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        IncidenteDetalle detalle = response.body().getData();
                        //La primera foto es la miniatura que muestra la lista
                        if (!detalle.getFotos().isEmpty()) {
                            detalle.setFoto(detalle.getFotos().get(0));
                        }
                        executor.execute(() -> guardarDetalleCache(detalle));
                        resultado.postValue(Resource.success(detalle));
                    } else {
                        resultado.postValue(Resource.error(ApiError.mensaje(response), cache, response.code()));
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<IncidenteDetalle>> call, Throwable t) {
                    resultado.postValue(Resource.error(SIN_CONEXION, cache, 0));
                }
            });
        });
    }

    private IncidenteDetalle leerDetalleCache(int id) {
        IncidenteEntity entity = database.incidenteDao().obtener(id);
        if (entity == null) {
            return null;
        }
        List<Historial> historial = new ArrayList<>();
        for (HistorialEntity h : database.historialDao().listarPorIncidente(id)) {
            historial.add(h.aModelo());
        }
        return new IncidenteDetalle(entity.aModelo(), historial);
    }

    private void guardarDetalleCache(IncidenteDetalle detalle) {
        List<IncidenteEntity> incidente = new ArrayList<>();
        incidente.add(IncidenteEntity.desdeModelo(detalle));
        database.incidenteDao().insertar(incidente);

        List<HistorialEntity> historial = new ArrayList<>();
        for (Historial h : detalle.getHistorial()) {
            historial.add(HistorialEntity.desdeModelo(detalle.getId(), h));
        }
        database.historialDao().reemplazar(detalle.getId(), historial);
    }

    // ===================== GESTIÓN (TÉCNICO Y ADMINISTRADOR) =====================

    public void cambiarEstado(int id, String estado, String comentario, MutableLiveData<Resource<String>> resultado) {
        enviarAccion(apiService.cambiarEstado(id, new CambioEstadoRequest(estado, comentario)), resultado);
    }

    public void asignarTecnico(int id, int idTecnico, MutableLiveData<Resource<String>> resultado) {
        enviarAccion(apiService.asignarTecnico(id, new AsignarRequest(idTecnico)), resultado);
    }

    public void cambiarPrioridad(int id, String prioridad, String comentario, MutableLiveData<Resource<String>> resultado) {
        enviarAccion(apiService.cambiarPrioridad(id, new PrioridadRequest(prioridad, comentario)), resultado);
    }

    public void comentar(int id, String comentario, MutableLiveData<Resource<String>> resultado) {
        enviarAccion(apiService.comentar(id, CambioEstadoRequest.comentario(comentario)), resultado);
    }

    //Ejecuta una acción en el API y devuelve el mensaje de confirmación ("message")
    private void enviarAccion(Call<ApiResponse<Object>> call, MutableLiveData<Resource<String>> resultado) {
        resultado.setValue(Resource.loading(null));
        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultado.postValue(Resource.success(response.body().getMessage()));
                } else {
                    resultado.postValue(Resource.error(ApiError.mensaje(response), null, response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                resultado.postValue(Resource.error(SIN_CONEXION + ". Intente nuevamente con conexión.", null, 0));
            }
        });
    }

    //El técnico comparte su ubicación actual (la ve el Administrador en el mapa)
    public void compartirUbicacion(double latitud, double longitud, MutableLiveData<Resource<String>> resultado) {
        enviarAccion(apiService.compartirUbicacion(new UbicacionRequest(latitud, longitud)), resultado);
    }

    public void listarTecnicos(MutableLiveData<Resource<List<Tecnico>>> resultado) {
        resultado.setValue(Resource.loading(null));
        apiService.getTecnicos().enqueue(new Callback<ApiResponse<List<Tecnico>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Tecnico>>> call, Response<ApiResponse<List<Tecnico>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    resultado.postValue(Resource.success(response.body().getData()));
                } else {
                    resultado.postValue(Resource.error(ApiError.mensaje(response), null, response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Tecnico>>> call, Throwable t) {
                resultado.postValue(Resource.error(SIN_CONEXION, null, 0));
            }
        });
    }

    // ===================== SUCURSALES Y ACTIVOS =====================

    public void listarSucursales(MutableLiveData<Resource<List<Sucursal>>> resultado) {
        executor.execute(() -> {
            List<Sucursal> cache = new ArrayList<>();
            for (SucursalEntity entity : database.sucursalDao().listar()) {
                cache.add(entity.aModelo());
            }
            resultado.postValue(Resource.loading(cache));

            apiService.getSucursales().enqueue(new Callback<ApiResponse<List<Sucursal>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Sucursal>>> call,
                                       Response<ApiResponse<List<Sucursal>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        List<Sucursal> sucursales = response.body().getData();
                        executor.execute(() -> {
                            List<SucursalEntity> entidades = new ArrayList<>();
                            for (Sucursal sucursal : sucursales) {
                                entidades.add(SucursalEntity.desdeModelo(sucursal));
                            }
                            database.sucursalDao().insertar(entidades);
                        });
                        resultado.postValue(Resource.success(sucursales));
                    } else {
                        resultado.postValue(Resource.error(ApiError.mensaje(response), cache, response.code()));
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Sucursal>>> call, Throwable t) {
                    resultado.postValue(Resource.error(SIN_CONEXION, cache, 0));
                }
            });
        });
    }

    public void buscarActivo(String codigoQr, MutableLiveData<Resource<Activo>> resultado) {
        resultado.setValue(Resource.loading(null));
        apiService.getActivo(codigoQr).enqueue(new Callback<ApiResponse<Activo>>() {
            @Override
            public void onResponse(Call<ApiResponse<Activo>> call, Response<ApiResponse<Activo>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    resultado.postValue(Resource.success(response.body().getData()));
                } else {
                    resultado.postValue(Resource.error(ApiError.mensaje(response), null, response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Activo>> call, Throwable t) {
                resultado.postValue(Resource.error(SIN_CONEXION, null, 0));
            }
        });
    }
}
