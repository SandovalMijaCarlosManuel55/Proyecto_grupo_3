package pe.edu.usat.incidentapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import pe.edu.usat.incidentapp.model.Login;
import pe.edu.usat.incidentapp.repository.AuthRepository;
import pe.edu.usat.incidentapp.util.Resource;
import pe.edu.usat.incidentapp.util.SessionManager;

public class LoginViewModel extends ViewModel {
    private final AuthRepository repository = AuthRepository.getInstance();
    private final MutableLiveData<Resource<Login>> resultadoLogin = new MutableLiveData<>();

    public LiveData<Resource<Login>> getResultadoLogin() {
        return resultadoLogin;
    }

    public void iniciarSesion(String correo, String password) {
        repository.login(correo, password, resultadoLogin);
    }

    public boolean haySesionActiva() {
        return SessionManager.getInstance().haySesionActiva();
    }

    public String getUltimoCorreo() {
        return SessionManager.getInstance().getCorreo();
    }
}
