package sgtu.aplicacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sgtu.RelojManual;
import sgtu.dominio.EstadoTurno;
import sgtu.dominio.EstadoVentanilla;
import sgtu.dominio.OperacionInvalidaException;
import sgtu.dominio.Turno;
import sgtu.persistencia.PersistenciaException;
import sgtu.persistencia.RepositorioArchivo;


/*
    Nuevamente, la funcionalidad de cada test se encuentra descrita en las anotaciones DisplayName
 */
@DisplayName("ServicioTurnos: guardado, reinicio, fallos y mensajes")
class ServicioTurnosTest {

    private RelojManual reloj;
    private RepositorioEnMemoria repositorio;
    private ServicioTurnos servicio;

    @BeforeEach
    void preparar() throws PersistenciaException {
        reloj = RelojManual.enFechaFija();
        repositorio = new RepositorioEnMemoria();
        servicio = ServicioTurnos.iniciar(repositorio, reloj);
    }

    private String foto(ServicioTurnos origen) {
        StringBuilder texto = new StringBuilder();
        for (Turno turno : origen.consultarHistorial()) {
            texto.append(turno.getId()).append('|').append(turno.getEstado()).append('|')
                    .append(turno.getVentanillaAsignada()).append('\n');
        }
        return texto.toString();
    }

    @Test
    @DisplayName("RF-12: una operación correcta se guarda y se confirma con el ID implicado")
    void operacionCorrectaSeGuardaYConfirma() throws Exception {
        ResultadoOperacion registro = servicio.registrarTurno();
        ResultadoOperacion asignacion = servicio.asignarSiguiente("V02");
        ResultadoOperacion finalizacion = servicio.finalizarAtencion("V02");
        servicio.registrarTurno();
        ResultadoOperacion cancelacion = servicio.cancelarTurno("T002");

        assertTrue(registro.mensaje().contains("T001"));
        assertTrue(asignacion.mensaje().contains("T001") && asignacion.mensaje().contains("V02"));
        assertTrue(finalizacion.mensaje().contains("V02"));
        assertTrue(cancelacion.mensaje().contains("T002"));
        assertEquals(5, repositorio.getVecesGuardado());
        assertEquals(2, repositorio.getGuardados().size());
    }

    @Test
    @DisplayName("RNF-U02: el rechazo explica la causa e indica el ID o la ventanilla")
    void rechazoExplicaLaCausa() throws Exception {
        servicio.registrarTurno();
        servicio.asignarSiguiente("V01");
        servicio.registrarTurno();

        assertTrue(assertThrows(OperacionInvalidaException.class, () -> servicio.asignarSiguiente("V01"))
                .getMessage().contains("ocupada"));
        assertTrue(assertThrows(OperacionInvalidaException.class, () -> servicio.cancelarTurno("T001"))
                .getMessage().contains("T001"));
        assertTrue(assertThrows(OperacionInvalidaException.class, () -> servicio.cancelarTurno("T050"))
                .getMessage().contains("no existe"));
        assertTrue(assertThrows(OperacionInvalidaException.class, () -> servicio.finalizarAtencion("V03"))
                .getMessage().contains("libre"));
        assertTrue(assertThrows(OperacionInvalidaException.class, () -> servicio.cancelarTurno("abc"))
                .getMessage().contains("no es válido"));
    }

    @Test
    @DisplayName("RNF-R02: una operación rechazada no cambia datos ni intenta guardar")
    void rechazoNoGuarda() throws Exception {
        servicio.registrarTurno();
        int guardadosAntes = repositorio.getVecesGuardado();
        String antes = foto(servicio);

        assertThrows(OperacionInvalidaException.class, () -> servicio.finalizarAtencion("V01"));
        assertThrows(OperacionInvalidaException.class, () -> servicio.cancelarTurno("T009"));

        assertEquals(guardadosAntes, repositorio.getVecesGuardado());
        assertEquals(antes, foto(servicio));
    }

    @Test
    @DisplayName("CV-09 / RD-02: si falla el guardado al registrar, no hay éxito ni turno fantasma")
    void falloAlRegistrarRestauraEstado() throws Exception {
        servicio.registrarTurno();
        repositorio.configurarFalloAlGuardar(true);

        PersistenciaException error = assertThrows(PersistenciaException.class, () -> servicio.registrarTurno());

        assertTrue(error.getMessage().contains("La operación no se aplicó"));
        assertEquals(1, servicio.consultarHistorial().size());
        assertEquals(1, repositorio.getGuardados().size());
    }

    @Test
    @DisplayName("CV-09 / RD-02: si falla el guardado al asignar, no queda relación parcial")
    void falloAlAsignarRestauraEstado() throws Exception {
        servicio.registrarTurno();
        servicio.registrarTurno();
        String antes = foto(servicio);
        repositorio.configurarFalloAlGuardar(true);

        assertThrows(PersistenciaException.class, () -> servicio.asignarSiguiente("V01"));

        assertEquals(antes, foto(servicio));
        assertTrue(servicio.consultarVentanillas().get(0).estaLibre());
        assertEquals(2, servicio.consultarFila().size());
    }

    @Test
    @DisplayName("CV-09 / RD-02: si falla el guardado al finalizar, la ventanilla sigue ocupada")
    void falloAlFinalizarRestauraEstado() throws Exception {
        servicio.registrarTurno();
        servicio.asignarSiguiente("V03");
        repositorio.configurarFalloAlGuardar(true);

        assertThrows(PersistenciaException.class, () -> servicio.finalizarAtencion("V03"));

        EstadoVentanilla v03 = servicio.consultarVentanillas().get(2);
        assertFalse(v03.estaLibre());
        assertEquals(EstadoTurno.EN_ATENCION, v03.turnoActual().getEstado());
    }

    @Test
    @DisplayName("CV-09 / RD-02: si falla el guardado al cancelar, el turno sigue En espera")
    void falloAlCancelarRestauraEstado() throws Exception {
        servicio.registrarTurno();
        repositorio.configurarFalloAlGuardar(true);

        assertThrows(PersistenciaException.class, () -> servicio.cancelarTurno("T001"));

        assertEquals(EstadoTurno.EN_ESPERA, servicio.consultarFila().get(0).getEstado());
    }

    @Test
    @DisplayName("Tras un fallo de guardado el servicio sigue funcionando cuando el almacenamiento se recupera")
    void servicioSeRecuperaDelFallo() throws Exception {
        servicio.registrarTurno();
        repositorio.configurarFalloAlGuardar(true);
        assertThrows(PersistenciaException.class, () -> servicio.registrarTurno());
        repositorio.configurarFalloAlGuardar(false);

        ResultadoOperacion resultado = servicio.registrarTurno();

        // La secuencia del turno fallido no quedó consumida: el siguiente es T002.
        assertEquals("T002", resultado.turno().getId());
        assertEquals(2, repositorio.getGuardados().size());
    }

    @Test
    @DisplayName("CV-07: al reiniciar se recuperan estados, asignaciones y orden")
    void cv07ReinicioRecuperaEstado() throws Exception {
        servicio.registrarTurno();
        servicio.registrarTurno();
        servicio.registrarTurno();
        servicio.asignarSiguiente("V02");
        servicio.cancelarTurno("T003");

        ServicioTurnos reiniciado = ServicioTurnos.iniciar(repositorio, reloj);

        assertEquals(foto(servicio), foto(reiniciado));
        assertFalse(reiniciado.consultarVentanillas().get(1).estaLibre());
        assertEquals("T001", reiniciado.consultarVentanillas().get(1).turnoActual().getId());
        assertEquals(List.of("T002"), List.of(reiniciado.consultarFila().get(0).getId()));
    }

    @Test
    @DisplayName("CV-08: después de reiniciar no se reutiliza ningún identificador")
    void cv08NoSeReutilizanIds() throws Exception {
        servicio.registrarTurno();
        servicio.registrarTurno();
        servicio.cancelarTurno("T002");

        ServicioTurnos reiniciado = ServicioTurnos.iniciar(repositorio, reloj);
        ResultadoOperacion nuevo = reiniciado.registrarTurno();

        assertEquals("T003", nuevo.turno().getId());
    }

    @Test
    @DisplayName("RD-03: si no se pueden leer los datos, no se inicia ni se escribe nada")
    void lecturaFallidaNoEscribe() {
        repositorio.configurarFalloAlCargar(true);

        assertThrows(PersistenciaException.class, () -> ServicioTurnos.iniciar(repositorio, reloj));

        assertEquals(0, repositorio.getVecesGuardado());
    }

    @Test
    @DisplayName("RD-03: datos que violan las invariantes se informan sin sobrescribir")
    void datosInvalidosNoSeSobrescriben() {
        LocalDateTime fecha = LocalDateTime.of(2026, 11, 11, 10, 0);
        repositorio.precargar(List.of(
                Turno.restaurar(1, fecha, EstadoTurno.EN_ATENCION, "V01"),
                Turno.restaurar(2, fecha, EstadoTurno.EN_ATENCION, "V01")));

        PersistenciaException error = assertThrows(PersistenciaException.class,
                () -> ServicioTurnos.iniciar(repositorio, reloj));

        assertTrue(error.getMessage().contains("inválidos"));
        assertEquals(0, repositorio.getVecesGuardado());
    }

    @Test
    @DisplayName("Integración con archivo real: reinicio con turnos en los cuatro estados (RNF-R01)")
    void reinicioConArchivoReal(@TempDir Path carpeta) throws Exception {
        Path ruta = carpeta.resolve("datos").resolve("turnos.txt");
        ServicioTurnos primero = ServicioTurnos.iniciar(new RepositorioArchivo(ruta), reloj);
        for (int i = 0; i < 4; i++) {
            primero.registrarTurno();
        }
        primero.asignarSiguiente("V01");
        primero.finalizarAtencion("V01");
        primero.asignarSiguiente("V02");
        primero.cancelarTurno("T003");

        ServicioTurnos segundo = ServicioTurnos.iniciar(new RepositorioArchivo(ruta), reloj);

        assertEquals(foto(primero), foto(segundo));
        List<Turno> historial = segundo.consultarHistorial();
        assertEquals(EstadoTurno.ATENDIDO, historial.get(0).getEstado());
        assertEquals(EstadoTurno.EN_ATENCION, historial.get(1).getEstado());
        assertEquals(EstadoTurno.CANCELADO, historial.get(2).getEstado());
        assertEquals(EstadoTurno.EN_ESPERA, historial.get(3).getEstado());
        assertFalse(segundo.consultarVentanillas().get(1).estaLibre());
    }
}
