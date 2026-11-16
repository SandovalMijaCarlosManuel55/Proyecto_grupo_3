package pe.edu.usat.incidentapp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;

import pe.edu.usat.incidentapp.model.Incidente;
import pe.edu.usat.incidentapp.model.Metricas;
import pe.edu.usat.incidentapp.util.SlaHelper;

public class MetricasTest {
    private static final long HORA = 3600_000L;

    private static String haceHoras(double horas) {
        return SlaHelper.formatearFechaUtc(new Date(System.currentTimeMillis() - (long) (horas * HORA)));
    }

    private static Incidente incidente(int id, String prioridad, String estado, double horasRegistro,
                                       String tecnico, Double horasResolucion) {
        Incidente i = new Incidente(id, "t", "d", "Red", prioridad, estado, haceHoras(horasRegistro),
                "Ana", tecnico, "Sede", null);
        if (horasResolucion != null) {
            i.setFechaResolucion(haceHoras(horasResolucion));
        }
        return i;
    }

    @Test
    public void calculaEstadosSlaYDesempeno() {
        Metricas m = Metricas.calcular(Arrays.asList(
                incidente(1, "Alta", "Abierto", 1, "Luis", null),          // verde (quedan 3h de 4h)
                incidente(2, "Alta", "En proceso", 3.5, "Luis", null),     // amarillo (queda < 25%)
                incidente(3, "Baja", "Abierto", 80, null, null),           // rojo (más de 72h)
                incidente(4, "Alta", "Resuelto", 10, "Luis", 8.0),         // resuelto en 2h (dentro SLA)
                incidente(5, "Media", "Cerrado", 40, "Jorge", 10.0)));     // resuelto en 30h (fuera SLA)

        assertEquals(5, m.getTotal());
        assertEquals(1, m.getSinAsignar());
        assertEquals(2, (int) m.getPorEstado().get("Abierto"));
        assertEquals(3, (int) m.getPorPrioridad().get("Alta"));
        assertEquals(1, m.getSlaVerde());
        assertEquals(1, m.getSlaAmarillo());
        assertEquals(1, m.getSlaRojo());
        assertEquals(2, m.getResueltos());
        assertEquals(50, m.getPorcentajeDentroSla());
        assertEquals(16 * HORA, m.getPromedioResolucionMs(), 60_000);

        Metricas.DesempenoTecnico luis = m.getTecnicos().get(0);
        assertEquals("Luis", luis.getNombre());
        assertEquals(3, luis.getAsignados());
        assertEquals(2, luis.getActivos());
        assertEquals(1, luis.getResueltos());
    }

    @Test
    public void listaVaciaDevuelveCeros() {
        Metricas m = Metricas.calcular(Arrays.asList());
        assertEquals(0, m.getTotal());
        assertEquals(0, m.getPorcentajeDentroSla());
        assertEquals("-", Metricas.duracion(m.getPromedioResolucionMs()));
    }
}
