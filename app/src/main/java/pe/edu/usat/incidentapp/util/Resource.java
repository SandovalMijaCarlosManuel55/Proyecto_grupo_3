package pe.edu.usat.incidentapp.util;

/**
 * Envuelve el resultado de una operación con su estado de interfaz:
 * LOADING (esperando respuesta), SUCCESS (datos) o ERROR (mensaje).
 */
public class Resource<T> {
    public enum Estado { LOADING, SUCCESS, ERROR }

    //Código HTTP 401: el token expiró o no es válido
    public static final int CODIGO_NO_AUTORIZADO = 401;

    private final Estado estado;
    private final T data;
    private final String mensaje;
    private final int codigo;

    private Resource(Estado estado, T data, String mensaje, int codigo) {
        this.estado = estado;
        this.data = data;
        this.mensaje = mensaje;
        this.codigo = codigo;
    }

    public static <T> Resource<T> loading(T data) {
        return new Resource<>(Estado.LOADING, data, null, 0);
    }

    public static <T> Resource<T> success(T data) {
        return new Resource<>(Estado.SUCCESS, data, null, 200);
    }

    public static <T> Resource<T> error(String mensaje, T data, int codigo) {
        return new Resource<>(Estado.ERROR, data, mensaje, codigo);
    }

    public Estado getEstado() {
        return estado;
    }

    public T getData() {
        return data;
    }

    public String getMensaje() {
        return mensaje;
    }

    public int getCodigo() {
        return codigo;
    }
}
