package pe.edu.usat.incidentapp.network;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import pe.edu.usat.incidentapp.model.Activo;
import pe.edu.usat.incidentapp.model.ApiResponse;
import pe.edu.usat.incidentapp.model.AsignarRequest;
import pe.edu.usat.incidentapp.model.CambioEstadoRequest;
import pe.edu.usat.incidentapp.model.FcmTokenRequest;
import pe.edu.usat.incidentapp.model.Incidente;
import pe.edu.usat.incidentapp.model.IncidenteDetalle;
import pe.edu.usat.incidentapp.model.IncidenteRegistrado;
import pe.edu.usat.incidentapp.model.Login;
import pe.edu.usat.incidentapp.model.LoginRequest;
import pe.edu.usat.incidentapp.model.PrioridadRequest;
import pe.edu.usat.incidentapp.model.Sucursal;
import pe.edu.usat.incidentapp.model.Tecnico;
import pe.edu.usat.incidentapp.model.UbicacionRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    //Aqui se implementa las llamadas a los endpoints

    //Público: devuelve el JWT con id, nombre y rol
    @POST("auth/login")
    Call<ApiResponse<Login>> login(@Body LoginRequest request);

    //Registra (o elimina con null) el token FCM del dispositivo.
    //El JWT se envía explícito porque al cerrar sesión ya no está guardado
    @POST("auth/fcm-token")
    Call<ApiResponse<Object>> registrarFcmToken(@Header("Authorization") String authorization,
                                                @Body FcmTokenRequest request);

    //Lista filtrada según el rol del JWT. Si estado/prioridad son null, Retrofit los omite
    @GET("incidentes")
    Call<ApiResponse<List<Incidente>>> getIncidentes(
            @Query("estado") String estado,
            @Query("prioridad") String prioridad
    );

    //Registrar incidente con multipart/form-data (hasta 2 fotos en el campo "fotos")
    @Multipart
    @POST("incidentes")
    Call<ApiResponse<IncidenteRegistrado>> registrarIncidente(
            @Part("titulo") RequestBody titulo,
            @Part("descripcion") RequestBody descripcion,
            @Part("categoria") RequestBody categoria,
            @Part("prioridad") RequestBody prioridad,
            @Part("id_sucursal") RequestBody idSucursal,
            @Part("codigo_qr") RequestBody codigoQr,
            @Part List<MultipartBody.Part> fotos
    );

    @GET("sucursales")
    Call<ApiResponse<List<Sucursal>>> getSucursales();

    @GET("activos/{codigo_qr}")
    Call<ApiResponse<Activo>> getActivo(@Path("codigo_qr") String codigoQr);

    //Detalle completo: incidente + historial + fotos
    @GET("incidentes/{id}")
    Call<ApiResponse<IncidenteDetalle>> getIncidente(@Path("id") int id);

    //Cambia el estado (comentario obligatorio). El API notifica al solicitante por FCM
    @PUT("incidentes/{id}/estado")
    Call<ApiResponse<Object>> cambiarEstado(@Path("id") int id, @Body CambioEstadoRequest request);

    //Asigna un técnico (solo Administrador). El API notifica al técnico por FCM
    @PUT("incidentes/{id}/asignar")
    Call<ApiResponse<Object>> asignarTecnico(@Path("id") int id, @Body AsignarRequest request);

    //Cambia la prioridad (solo Administrador). Queda registrado en el historial
    @PUT("incidentes/{id}/prioridad")
    Call<ApiResponse<Object>> cambiarPrioridad(@Path("id") int id, @Body PrioridadRequest request);

    //Agrega un comentario sin cambiar el estado
    @POST("incidentes/{id}/comentarios")
    Call<ApiResponse<Object>> comentar(@Path("id") int id, @Body CambioEstadoRequest request);

    //Técnicos de la empresa con su última ubicación (Administrador)
    @GET("usuarios/tecnicos")
    Call<ApiResponse<List<Tecnico>>> getTecnicos();

    //El técnico comparte su ubicación actual
    @PUT("usuarios/ubicacion")
    Call<ApiResponse<Object>> compartirUbicacion(@Body UbicacionRequest request);
}
