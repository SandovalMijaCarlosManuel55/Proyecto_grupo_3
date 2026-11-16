package pe.edu.usat.incidentapp.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Semáforo SLA calculado en el cliente a partir de la fecha de registro y la prioridad.
 * Tiempo límite: Alta 4h, Media 24h, Baja 72h.
 * VERDE: dentro del tiempo. AMARILLO: queda menos del 25%. ROJO: tiempo excedido.
 */
public class SlaHelper {
    public enum Semaforo { VERDE, AMARILLO, ROJO, NO_APLICA }

    private static final String FORMATO_API = "yyyy-MM-dd HH:mm:ss";

    public static long horasLimite(String prioridad) {
        if ("Alta".equals(prioridad)) return 4;
        if ("Media".equals(prioridad)) return 24;
        return 72;
    }

    //La fecha del API está en UTC (servidor MySQL de PythonAnywhere)
    public static Date parsearFecha(String fecha) {
        if (fecha == null) {
            return null;
        }
        try {
            SimpleDateFormat formato = new SimpleDateFormat(FORMATO_API, Locale.US);
            formato.setTimeZone(TimeZone.getTimeZone("UTC"));
            return formato.parse(fecha);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String formatearFechaUtc(Date fecha) {
        SimpleDateFormat formato = new SimpleDateFormat(FORMATO_API, Locale.US);
        formato.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formato.format(fecha);
    }

    //Fecha en hora local para mostrar: dd/MM/yyyy HH:mm
    public static String fechaLegible(String fecha) {
        Date date = parsearFecha(fecha);
        if (date == null) {
            return fecha != null ? fecha : "-";
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date);
    }

    public static Semaforo calcular(String prioridad, String estado, String fechaRegistro) {
        //Los incidentes resueltos o cerrados ya no corren contra el SLA
        if ("Resuelto".equals(estado) || "Cerrado".equals(estado)) {
            return Semaforo.NO_APLICA;
        }
        Date registro = parsearFecha(fechaRegistro);
        if (registro == null) {
            return Semaforo.NO_APLICA;
        }
        long limiteMs = horasLimite(prioridad) * 60 * 60 * 1000;
        long transcurridoMs = System.currentTimeMillis() - registro.getTime();
        long restanteMs = limiteMs - transcurridoMs;

        if (restanteMs <= 0) return Semaforo.ROJO;
        if (restanteMs < limiteMs * 0.25) return Semaforo.AMARILLO;
        return Semaforo.VERDE;
    }

    //Texto con el tiempo restante o excedido del SLA
    public static String descripcion(String prioridad, String estado, String fechaRegistro) {
        Semaforo semaforo = calcular(prioridad, estado, fechaRegistro);
        if (semaforo == Semaforo.NO_APLICA) {
            return "SLA: atendido";
        }
        Date registro = parsearFecha(fechaRegistro);
        long limiteMs = horasLimite(prioridad) * 60 * 60 * 1000;
        long restanteMs = limiteMs - (System.currentTimeMillis() - registro.getTime());

        if (restanteMs <= 0) {
            return "SLA vencido hace " + duracion(-restanteMs);
        }
        return "SLA: quedan " + duracion(restanteMs);
    }

    private static String duracion(long ms) {
        long minutos = ms / 60000;
        long horas = minutos / 60;
        minutos = minutos % 60;
        if (horas >= 24) {
            return (horas / 24) + "d " + (horas % 24) + "h";
        }
        return horas + "h " + minutos + "m";
    }
}
