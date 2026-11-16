package pe.edu.usat.incidentapp.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pe.edu.usat.incidentapp.util.SlaHelper;

/**
 * Reportes y métricas del Administrador, calculados en la app a partir de GET /incidentes
 * (el Administrador recibe todos los incidentes de su empresa).
 * Igual que el semáforo, el SLA se calcula en el cliente con la fecha de registro y la prioridad.
 */
public class Metricas {
    public static final String[] ESTADOS = {"Abierto", "En proceso", "Resuelto", "Cerrado"};
    public static final String[] PRIORIDADES = {"Alta", "Media", "Baja"};

    private int total;
    private int sinAsignar;

    //Cantidad por estado y por prioridad (en el orden de ESTADOS y PRIORIDADES)
    private final Map<String, Integer> porEstado = new LinkedHashMap<>();
    private final Map<String, Integer> porPrioridad = new LinkedHashMap<>();

    //Semáforo SLA de los incidentes activos (Abierto / En proceso)
    private int slaVerde;
    private int slaAmarillo;
    private int slaRojo;

    //Incidentes resueltos (Resuelto o Cerrado con fecha de resolución)
    private int resueltos;
    private int resueltosDentroSla;
    private long sumaResolucionMs;

    //Desempeño del equipo de TI
    private final List<DesempenoTecnico> tecnicos = new ArrayList<>();

    public static Metricas calcular(List<Incidente> incidentes) {
        Metricas m = new Metricas();
        for (String estado : ESTADOS) m.porEstado.put(estado, 0);
        for (String prioridad : PRIORIDADES) m.porPrioridad.put(prioridad, 0);

        Map<String, DesempenoTecnico> porTecnico = new LinkedHashMap<>();

        for (Incidente i : incidentes) {
            if (i.isPendiente()) {
                continue; //Aún no existe en el servidor
            }
            m.total++;
            sumar(m.porEstado, i.getEstado());
            sumar(m.porPrioridad, i.getPrioridad());

            boolean activo = "Abierto".equals(i.getEstado()) || "En proceso".equals(i.getEstado());
            if (activo) {
                switch (SlaHelper.calcular(i.getPrioridad(), i.getEstado(), i.getFechaRegistro())) {
                    case VERDE: m.slaVerde++; break;
                    case AMARILLO: m.slaAmarillo++; break;
                    case ROJO: m.slaRojo++; break;
                    default: break;
                }
            }

            //Tiempo de resolución = fecha de resolución - fecha de registro
            long resolucionMs = tiempoResolucion(i);
            if (resolucionMs >= 0) {
                m.resueltos++;
                m.sumaResolucionMs += resolucionMs;
                if (resolucionMs <= SlaHelper.horasLimite(i.getPrioridad()) * 3600_000L) {
                    m.resueltosDentroSla++;
                }
            }

            if (i.getTecnico() == null) {
                m.sinAsignar++;
            } else {
                DesempenoTecnico t = porTecnico.get(i.getTecnico());
                if (t == null) {
                    t = new DesempenoTecnico(i.getTecnico());
                    porTecnico.put(i.getTecnico(), t);
                }
                t.asignados++;
                if (activo) t.activos++;
                if (resolucionMs >= 0) {
                    t.resueltos++;
                    t.sumaResolucionMs += resolucionMs;
                }
            }
        }
        m.tecnicos.addAll(porTecnico.values());
        //Primero los que más incidentes resolvieron
        m.tecnicos.sort((a, b) -> b.resueltos != a.resueltos ? b.resueltos - a.resueltos : b.asignados - a.asignados);
        return m;
    }

    private static void sumar(Map<String, Integer> mapa, String clave) {
        if (clave != null && mapa.containsKey(clave)) {
            mapa.put(clave, mapa.get(clave) + 1);
        }
    }

    //-1 si el incidente no está resuelto
    private static long tiempoResolucion(Incidente i) {
        boolean resuelto = "Resuelto".equals(i.getEstado()) || "Cerrado".equals(i.getEstado());
        Date registro = SlaHelper.parsearFecha(i.getFechaRegistro());
        Date resolucion = SlaHelper.parsearFecha(i.getFechaResolucion());
        if (!resuelto || registro == null || resolucion == null) {
            return -1;
        }
        return Math.max(0, resolucion.getTime() - registro.getTime());
    }

    //"5h 20m" o "1d 3h"; "-" si no hay datos
    public static String duracion(long ms) {
        if (ms < 0) return "-";
        long minutos = ms / 60000;
        long horas = minutos / 60;
        if (horas >= 24) return (horas / 24) + "d " + (horas % 24) + "h";
        return horas + "h " + (minutos % 60) + "m";
    }

    public int getTotal() { return total; }

    public int getSinAsignar() { return sinAsignar; }

    public Map<String, Integer> getPorEstado() { return porEstado; }

    public Map<String, Integer> getPorPrioridad() { return porPrioridad; }

    public int getSlaVerde() { return slaVerde; }

    public int getSlaAmarillo() { return slaAmarillo; }

    public int getSlaRojo() { return slaRojo; }

    public int getActivos() { return slaVerde + slaAmarillo + slaRojo; }

    public int getResueltos() { return resueltos; }

    //Porcentaje de incidentes resueltos dentro del tiempo límite del SLA
    public int getPorcentajeDentroSla() {
        return resueltos == 0 ? 0 : Math.round(resueltosDentroSla * 100f / resueltos);
    }

    public long getPromedioResolucionMs() {
        return resueltos == 0 ? -1 : sumaResolucionMs / resueltos;
    }

    public List<DesempenoTecnico> getTecnicos() { return tecnicos; }

    /** Desempeño de un técnico: asignados, activos, resueltos y tiempo promedio de resolución. */
    public static class DesempenoTecnico {
        private final String nombre;
        private int asignados;
        private int activos;
        private int resueltos;
        private long sumaResolucionMs;

        DesempenoTecnico(String nombre) {
            this.nombre = nombre;
        }

        public String getNombre() { return nombre; }

        public int getAsignados() { return asignados; }

        public int getActivos() { return activos; }

        public int getResueltos() { return resueltos; }

        public long getPromedioResolucionMs() {
            return resueltos == 0 ? -1 : sumaResolucionMs / resueltos;
        }
    }
}
