package pe.edu.usat.incidentapp.repository;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.firebase.messaging.FirebaseMessaging;

import pe.edu.usat.incidentapp.local.AppDatabase;
import pe.edu.usat.incidentapp.model.ApiResponse;
import pe.edu.usat.incidentapp.model.FcmTokenRequest;
import pe.edu.usat.incidentapp.model.Login;
import pe.edu.usat.incidentapp.model.LoginRequest;
import pe.edu.usat.incidentapp.network.ApiService;
import pe.edu.usat.incidentapp.network.RetrofitClient;
import pe.edu.usat.incidentapp.util.ApiError;
import pe.edu.usat.incidentapp.util.Resource;
import pe.edu.usat.incidentapp.util.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Autenticación con el API (POST /auth/login) y manejo de la sesión JWT.
 */
public class AuthRepository {
    private static AuthRepository instance;

    private final ApiService apiService;
    private final AppDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private AuthRepository(Context context) {
        apiService = RetrofitClient.createService();
        database = AppDatabase.getInstance(context);
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new AuthRepository(context.getApplicationContext());
        }
    }

    public static AuthRepository getInstance() {
        return instance;
    }

    public void login(String correo, String password, MutableLiveData<Resource<Login>> resultado) {
        resultado.setValue(Resource.loading(null));

        apiService.login(new LoginRequest(correo, password)).enqueue(new Callback<ApiResponse<Login>>() {
            @Override
            public void onResponse(Call<ApiResponse<Login>> call, Response<ApiResponse<Login>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Login login = response.body().getData();

                    //Almacenar el token JWT para que se envíe en las siguientes peticiones
                    SessionManager.getInstance().guardarSesion(login.getToken(), correo);

                    //Registrar el token FCM del celular para recibir notificaciones push
                    registrarTokenFcm();

                    //Limpiar la caché local antes de mostrar los datos de la empresa del usuario
                    executor.execute(() -> {
                        limpiarCache();
                        resultado.postValue(Resource.success(login));
                    });
                } else { //400, 401, 403, 500
                    resultado.postValue(Resource.error(ApiError.mensaje(response), null, response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Login>> call, Throwable t) {
                resultado.postValue(Resource.error(
                        "No se pudo conectar con el servidor. Verifique su conexión a internet.", null, 0));
            }
        });
    }

    //Elimina el token almacenado y la caché local del usuario
    public void cerrarSesion() {
        //Desregistrar el token FCM (este celular deja de recibir notificaciones del usuario)
        String jwt = SessionManager.getInstance().getToken();
        if (jwt != null) {
            apiService.registrarFcmToken("Bearer " + jwt, new FcmTokenRequest(null)).enqueue(sinRespuesta());
        }
        SessionManager.getInstance().cerrarSesion();
        executor.execute(this::limpiarCache);
    }

    // ===================== FIREBASE CLOUD MESSAGING =====================

    //Obtiene el token FCM del dispositivo y lo registra en el API para asociarlo al usuario
    public void registrarTokenFcm() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(this::enviarTokenFcm);
    }

    //También lo llama el servicio FCM cuando Firebase genera un token nuevo
    public void enviarTokenFcm(String tokenFcm) {
        if (!SessionManager.getInstance().haySesionActiva()) {
            return;
        }
        String jwt = SessionManager.getInstance().getToken();
        apiService.registrarFcmToken("Bearer " + jwt, new FcmTokenRequest(tokenFcm)).enqueue(sinRespuesta());
    }

    //El registro del token no muestra nada al usuario: si falla, se reintenta en el próximo login
    private Callback<ApiResponse<Object>> sinRespuesta() {
        return new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
            }
        };
    }

    //Cada empresa tiene sus propios incidentes y sucursales: no se mezclan en Room
    private void limpiarCache() {
        database.incidenteDao().eliminarTodos();
        database.historialDao().eliminarTodos();
        database.sucursalDao().eliminarTodos();
    }
}
