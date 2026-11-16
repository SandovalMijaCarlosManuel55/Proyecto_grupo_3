package pe.edu.usat.incidentapp.util;

import pe.edu.usat.incidentapp.R;

/**
 * Colores de la prioridad, el estado y el semáforo SLA (los usan la lista y el detalle).
 */
public class ColoresHelper {

    //rojo=Alta, naranja=Media, verde=Baja
    public static int prioridad(String prioridad) {
        if ("Alta".equals(prioridad)) return R.color.prioridad_alta;
        if ("Media".equals(prioridad)) return R.color.prioridad_media;
        return R.color.prioridad_baja;
    }

    public static int estado(String estado, boolean pendiente) {
        if (pendiente) return R.color.estado_pendiente;
        if ("En proceso".equals(estado)) return R.color.estado_en_proceso;
        if ("Resuelto".equals(estado)) return R.color.estado_resuelto;
        if ("Cerrado".equals(estado)) return R.color.estado_cerrado;
        return R.color.estado_abierto;
    }

    public static int semaforo(SlaHelper.Semaforo semaforo) {
        switch (semaforo) {
            case VERDE:
                return R.color.sla_verde;
            case AMARILLO:
                return R.color.sla_amarillo;
            case ROJO:
                return R.color.sla_rojo;
            default:
                return R.color.estado_cerrado;
        }
    }
}
