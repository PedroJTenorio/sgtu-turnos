package sgtu.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sgtu.RelojManual;
import sgtu.persistencia.RepositorioArchivo;

@DisplayName("RNF-P01: respuesta con 200 turnos almacenados (archivo real)")
class RendimientoTest {

    private static final long LIMITE_MILISEGUNDOS = 2000;
    private static final int TURNOS = 200;

    @FunctionalInterface
    private interface Medible {
        void ejecutar() throws Exception;
    }

    private long medir(Medible operacion) throws Exception {
        long inicio = System.nanoTime();
        operacion.ejecutar();
        return Duration.ofNanos(System.nanoTime() - inicio).toMillis();
    }

    @Test
    @DisplayName("Registrar, consultar fila, asignar y finalizar tardan menos de 2 s con 200 turnos")
    void operacionesNormalesResponden(@TempDir Path carpeta) throws Exception {
        ServicioTurnos servicio = ServicioTurnos.iniciar(
                new RepositorioArchivo(carpeta.resolve("turnos.txt")), RelojManual.enFechaFija());
        for (int i = 0; i < TURNOS; i++) {
            servicio.registrarTurno();
        }
        assertEquals(TURNOS, servicio.consultarHistorial().size());

        long registrar = medir(() -> servicio.registrarTurno());
        long consultarFila = medir(() -> servicio.consultarFila());
        long asignar = medir(() -> servicio.asignarSiguiente("V01"));
        long finalizar = medir(() -> servicio.finalizarAtencion("V01"));

        System.out.println("Tiempos con " + TURNOS + " turnos (ms): registrar=" + registrar
                + ", consultarFila=" + consultarFila + ", asignar=" + asignar + ", finalizar=" + finalizar);
        assertTrue(registrar < LIMITE_MILISEGUNDOS);
        assertTrue(consultarFila < LIMITE_MILISEGUNDOS);
        assertTrue(asignar < LIMITE_MILISEGUNDOS);
        assertTrue(finalizar < LIMITE_MILISEGUNDOS);
    }
}