package com.example.proyecto_grupo_3.api;

import com.example.proyecto_grupo_3.model.AuthRequest;
import com.example.proyecto_grupo_3.model.AuthResponse;
import com.example.proyecto_grupo_3.model.Empresa;
import com.example.proyecto_grupo_3.model.LoginRequest;
import com.example.proyecto_grupo_3.model.Usuario;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {

    // Endpoint Flask-JWT para autenticación
    @POST("auth")
    Call<AuthResponse> autenticar(@Body AuthRequest authRequest);

    // Endpoints para Empresas
    @GET("api_obtener_empresas")
    Call<ApiResponse<List<Empresa>>> getEmpresas();

    @GET("api_obtener_empresas")
    Call<ApiResponse<List<Empresa>>> getEmpresasAuth(@Header("Authorization") String token);

    // Endpoints para Usuarios
    @GET("api_obtener_usuarios")
    Call<ApiResponse<List<Usuario>>> getUsuarios(@Header("Authorization") String token);

    @POST("api_insertar_usuario")
    Call<ApiResponse<Void>> insertarUsuario(@Body Usuario usuario);

    // Endpoints para Sucursales, Activos e Incidentes
    @GET("api_obtener_sucursales")
    Call<ApiResponse<List<Object>>> getSucursales(@Header("Authorization") String token);

    @GET("api_obtener_activos")
    Call<ApiResponse<List<Object>>> getActivos(@Header("Authorization") String token);

    @GET("api_obtener_incidentes")
    Call<ApiResponse<List<Object>>> getIncidentes(@Header("Authorization") String token);
}
