package pe.edu.usat.incidentapp.network;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pe.edu.usat.incidentapp.util.SessionManager;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // API desplegada en PythonAnywhere (conectada a la BD jcachay$dambgr3).
    public static final String URL_API_SERVICE = "https://pokemonsac.pythonanywhere.com/";

    // API en localhost (emulador). 10.0.2.2 es el localhost de la PC visto desde el emulador.
    // public static final String URL_API_SERVICE = "http://10.0.2.2:3007/";

    //Interceptor OkHttp que agrega el JWT en cada petición: Authorization: Bearer {token}
    private static class AuthInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder();

            String token = SessionManager.getInstance().getToken();
            if (token != null && !token.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + token);
            }

            return chain.proceed(requestBuilder.build());
        }
    }

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .addInterceptor(new AuthInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private static final Retrofit API_SERVICE = new Retrofit.Builder()
            .baseUrl(URL_API_SERVICE)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static ApiService apiService;

    public static ApiService createService() {
        if (apiService == null) {
            apiService = API_SERVICE.create(ApiService.class);
        }
        return apiService;
    }
}
