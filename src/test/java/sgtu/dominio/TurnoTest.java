package sgtu.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Turno: estado inicial, transiciones y restauración")
class TurnoTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 11, 10, 0);

    @Test
    @DisplayName("Un turno nuevo está En espera y sin ventanilla (RF-02)")
    void turnoNuevoEstaEnEsperaSinVentanilla() {
        Turno turno = new Turno(1, FECHA);
        assertEquals(EstadoTurno.EN_ESPERA, turno.getEstado());
        assertNull(turno.getVentanillaAsignada());
        assertEquals("T001", turno.getId());
    }

    @Test
    @DisplayName("El ID crece con la secuencia, incluso pasando de tres dígitos")
    void idSeDerivaDeLaSecuencia() {
        assertEquals("T009", Turno.generarId(9));
        assertEquals("T1000", Turno.generarId(1000));
    }

    @Test
    @DisplayName("Secuencia menor a 1 se rechaza")
    void secuenciaInvalidaSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> new Turno(0, FECHA));
        assertThrows(IllegalArgumentException.class, () -> new Turno(-3, FECHA));
    }

    @Test
    @DisplayName("Ciclo completo: En espera → En atención → Atendido")
    void cicloCompletoDeAtencion() throws OperacionInvalidaException {
        Turno turno = new Turno(1, FECHA);
        turno.iniciarAtencion("V01");
        assertEquals(EstadoTurno.EN_ATENCION, turno.getEstado());
        assertEquals("V01", turno.getVentanillaAsignada());
        turno.finalizar();
        assertEquals(EstadoTurno.ATENDIDO, turno.getEstado());
        assertEquals("V01", turno.getVentanillaAsignada());
    }

    @Test
    @DisplayName("Cancelar desde En espera deja el turno Cancelado sin ventanilla")
    void cancelarDesdeEspera() throws OperacionInvalidaException {
        Turno turno = new Turno(1, FECHA);
        turno.cancelar();
        assertEquals(EstadoTurno.CANCELADO, turno.getEstado());
        assertNull(turno.getVentanillaAsignada());
    }

    @Test
    @DisplayName("No se puede cancelar un turno En atención (RN-07)")
    void noSeCancelaEnAtencion() throws OperacionInvalidaException {
        Turno turno = new Turno(2, FECHA);
        turno.iniciarAtencion("V02");
        OperacionInvalidaException error = assertThrows(OperacionInvalidaException.class, turno::cancelar);
        assertEquals(EstadoTurno.EN_ATENCION, turno.getEstado());
        assertEquals(true, error.getMessage().contains("T002"));
    }

    @Test
    @DisplayName("Los estados terminales no admiten ninguna transición (RN-09)")
    void estadosTerminalesNoCambian() throws OperacionInvalidaException {
        Turno atendido = new Turno(1, FECHA);
        atendido.iniciarAtencion("V01");
        atendido.finalizar();
        Turno cancelado = new Turno(2, FECHA);
        cancelado.cancelar();
        assertThrows(OperacionInvalidaException.class, atendido::cancelar);
        assertThrows(OperacionInvalidaException.class, atendido::finalizar);
        assertThrows(OperacionInvalidaException.class, () -> atendido.iniciarAtencion("V03"));
        assertThrows(OperacionInvalidaException.class, cancelado::cancelar);
        assertThrows(OperacionInvalidaException.class, cancelado::finalizar);
        assertThrows(OperacionInvalidaException.class, () -> cancelado.iniciarAtencion("V03"));
        assertEquals(EstadoTurno.ATENDIDO, atendido.getEstado());
        assertEquals(EstadoTurno.CANCELADO, cancelado.getEstado());
    }

    @Test
    @DisplayName("Solo se finaliza un turno En atención (RN-08)")
    void soloSeFinalizaEnAtencion() {
        Turno turno = new Turno(1, FECHA);
        assertThrows(OperacionInvalidaException.class, turno::finalizar);
        assertEquals(EstadoTurno.EN_ESPERA, turno.getEstado());
    }

    @Test
    @DisplayName("Restaurar rechaza combinaciones incoherentes de estado y ventanilla")
    void restaurarValidaCoherencia() {
        assertThrows(IllegalArgumentException.class, () -> Turno.restaurar(1, FECHA, EstadoTurno.EN_ATENCION, null));
        assertThrows(IllegalArgumentException.class, () -> Turno.restaurar(1, FECHA, EstadoTurno.EN_ATENCION, "V09"));
        assertThrows(IllegalArgumentException.class, () -> Turno.restaurar(1, FECHA, EstadoTurno.ATENDIDO, null));
        assertThrows(IllegalArgumentException.class, () -> Turno.restaurar(1, FECHA, EstadoTurno.EN_ESPERA, "V01"));
        assertThrows(IllegalArgumentException.class, () -> Turno.restaurar(1, FECHA, EstadoTurno.CANCELADO, "V01"));
    }

    @Test
    @DisplayName("Restaurar conserva los datos de un turno válido")
    void restaurarConservaDatos() {
        Turno turno = Turno.restaurar(7, FECHA, EstadoTurno.EN_ATENCION, "V03");
        assertEquals(7, turno.getSecuencia());
        assertEquals(EstadoTurno.EN_ATENCION, turno.getEstado());
        assertEquals("V03", turno.getVentanillaAsignada());
        assertEquals(FECHA, turno.getFechaHoraRegistro());
    }

    @Test
    @DisplayName("Una copia es independiente del original")
    void copiaEsIndependiente() throws OperacionInvalidaException {
        Turno original = new Turno(1, FECHA);
        Turno copia = original.copiar();
        copia.cancelar();
        assertNotSame(original, copia);
        assertEquals(EstadoTurno.EN_ESPERA, original.getEstado());
    }
}