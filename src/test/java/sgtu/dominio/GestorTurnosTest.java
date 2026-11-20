package sgtu.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import sgtu.RelojManual;

@DisplayName("GestorTurnos: FIFO, ventanillas, estados y rechazos")
class GestorTurnosTest {

    private RelojManual reloj;
    private GestorTurnos gestor;

    @BeforeEach
    void preparar() {
        reloj = RelojManual.enFechaFija();
        gestor = new GestorTurnos(reloj);
    }

    // Registra n turnos y devuelve sus IDs en orden de registro.
    private List<String> registrar(int cantidad) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            ids.add(gestor.registrarTurno().getId());
            reloj.avanzar(Duration.ofSeconds(1));
        }
        return ids;
    }

    private List<String> idsDe(List<Turno> turnos) {
        List<String> ids = new ArrayList<>();
        for (Turno turno : turnos) {
            ids.add(turno.getId());
        }
        return ids;
    }

    // Foto del estado completo, para comprobar que un rechazo no cambia nada (RNF-R02).
    private String foto() {
        StringBuilder texto = new StringBuilder();
        for (Turno turno : gestor.consultarHistorial()) {
            texto.append(turno.getId()).append('|').append(turno.getEstado()).append('|')
                    .append(turno.getVentanillaAsignada()).append('\n');
        }
        return texto.toString();
    }

    private EstadoVentanilla ventanilla(String id) {
        for (EstadoVentanilla estado : gestor.consultarVentanillas()) {
            if (estado.id().equals(id)) {
                return estado;
            }
        }
        throw new AssertionError("No existe la ventanilla " + id);
    }

    @Nested
    @DisplayName("Registro de turnos (RF-02, RN-01, RN-02)")
    class Registro {

        @Test
        @DisplayName("Tres turnos quedan En espera, con IDs únicos y en orden")
        void tresTurnosConIdsUnicosYOrdenados() {
            List<String> ids = registrar(3);

            assertEquals(List.of("T001", "T002", "T003"), ids);
            assertEquals(3, new HashSet<>(ids).size());
            for (Turno turno : gestor.consultarHistorial()) {
                assertEquals(EstadoTurno.EN_ESPERA, turno.getEstado());
                assertNull(turno.getVentanillaAsignada());
            }
        }

        @Test
        @DisplayName("La fecha de registro sale del reloj")
        void fechaDeRegistroSaleDelReloj() {
            Turno turno = gestor.registrarTurno();

            assertEquals(LocalDateTime.of(2026, 11, 11, 10, 0), turno.getFechaHoraRegistro());
        }

        @Test
        @DisplayName("El orden FIFO no depende del reloj: misma hora exacta para todos")
        void fifoConRelojDetenido() throws OperacionInvalidaException {
            GestorTurnos detenido = new GestorTurnos(RelojManual.enFechaFija());
            detenido.registrarTurno();
            detenido.registrarTurno();
            detenido.registrarTurno();

            assertEquals("T001", detenido.asignarSiguiente("V01").getId());
            assertEquals("T002", detenido.asignarSiguiente("V02").getId());
            assertEquals("T003", detenido.asignarSiguiente("V03").getId());
        }

        @Test
        @DisplayName("El orden FIFO no depende del reloj: hora que retrocede")
        void fifoConRelojQueRetrocede() throws OperacionInvalidaException {
            gestor.registrarTurno();
            reloj.avanzar(Duration.ofHours(-5));
            gestor.registrarTurno();

            assertEquals("T001", gestor.asignarSiguiente("V01").getId());
            assertEquals("T002", gestor.asignarSiguiente("V02").getId());
        }

        @Test
        @DisplayName("El orden FIFO no es alfabético: T1000 va después de T999 (RN-02)")
        void fifoNoEsAlfabetico() throws OperacionInvalidaException {
            registrar(1001);
            for (int i = 0; i < 998; i++) {
                gestor.asignarSiguiente("V01");
                gestor.finalizarAtencion("V01");
            }

            // Quedan T999, T1000 y T1001 En espera. Alfabéticamente "T1000" < "T999".
            assertEquals(List.of("T999", "T1000", "T1001"), idsDe(gestor.consultarFila()));
            assertEquals("T999", gestor.asignarSiguiente("V01").getId());
            assertEquals("T1000", gestor.asignarSiguiente("V02").getId());
        }
    }

    @Nested
    @DisplayName("Consulta de ventanillas (RF-01)")
    class ConsultaVentanillas {

        @Test
        @DisplayName("Siempre existen V01, V02 y V03, todas libres al inicio")
        void tresVentanillasLibres() {
            List<EstadoVentanilla> ventanillas = gestor.consultarVentanillas();

            assertEquals(3, ventanillas.size());
            assertEquals("V01", ventanillas.get(0).id());
            assertEquals("V02", ventanillas.get(1).id());
            assertEquals("V03", ventanillas.get(2).id());
            for (EstadoVentanilla ventanilla : ventanillas) {
                assertTrue(ventanilla.estaLibre());
            }
        }

        @Test
        @DisplayName("Con un turno en V02, V02 está ocupada y V01/V03 libres")
        void soloV02Ocupada() throws OperacionInvalidaException {
            registrar(1);
            gestor.asignarSiguiente("V02");

            assertTrue(ventanilla("V01").estaLibre());
            assertFalse(ventanilla("V02").estaLibre());
            assertEquals("T001", ventanilla("V02").turnoActual().getId());
            assertTrue(ventanilla("V03").estaLibre());
        }
    }

    @Nested
    @DisplayName("Asignación (RF-04, RF-05, RN-04 a RN-06, RN-10)")
    class Asignacion {

        @Test
        @DisplayName("CV-01: V02 recibe T1, V01 recibe T2 y T3 sigue En espera")
        void cv01AsignacionEnOrden() throws OperacionInvalidaException {
            registrar(3);

            Turno primero = gestor.asignarSiguiente("V02");
            Turno segundo = gestor.asignarSiguiente("V01");

            assertEquals("T001", primero.getId());
            assertEquals("V02", primero.getVentanillaAsignada());
            assertEquals("T002", segundo.getId());
            assertEquals("V01", segundo.getVentanillaAsignada());
            assertEquals(List.of("T003"), idsDe(gestor.consultarFila()));
        }

        @Test
        @DisplayName("RF-04: desde V03 y luego V01 se asignan T1 y T2")
        void asignaDesdeV03YLuegoV01() throws OperacionInvalidaException {
            registrar(3);

            assertEquals("T001", gestor.asignarSiguiente("V03").getId());
            assertEquals("T002", gestor.asignarSiguiente("V01").getId());
        }

        @Test
        @DisplayName("Asignar inicia la atención de inmediato (RN-06)")
        void asignarIniciaAtencion() throws OperacionInvalidaException {
            registrar(1);

            Turno turno = gestor.asignarSiguiente("V01");

            assertEquals(EstadoTurno.EN_ATENCION, turno.getEstado());
        }

        @Test
        @DisplayName("CV-02: ventanilla ocupada rechaza y no cambia nada")
        void cv02VentanillaOcupadaSeRechaza() throws OperacionInvalidaException {
            registrar(3);
            gestor.asignarSiguiente("V02");
            String antes = foto();

            OperacionInvalidaException error = assertThrows(OperacionInvalidaException.class,
                    () -> gestor.asignarSiguiente("V02"));

            assertTrue(error.getMessage().contains("V02"));
            assertTrue(error.getMessage().contains("T001"));
            assertEquals(antes, foto());
        }

        @Test
        @DisplayName("RN-10: con la fila vacía no se altera nada")
        void filaVaciaSeRechaza() {
            String antes = foto();

            OperacionInvalidaException error = assertThrows(OperacionInvalidaException.class,
                    () -> gestor.asignarSiguiente("V01"));

            assertTrue(error.getMessage().contains("No hay turnos en espera"));
            assertEquals(antes, foto());
        }

        @Test
        @DisplayName("CV-03: con tres ventanillas ocupadas no hay cuarta atención")
        void cv03NoHayCuartaAtencion() throws OperacionInvalidaException {
            registrar(5);
            gestor.asignarSiguiente("V01");
            gestor.asignarSiguiente("V02");
            gestor.asignarSiguiente("V03");
            String antes = foto();

            for (String id : List.of("V01", "V02", "V03")) {
                assertThrows(OperacionInvalidaException.class, () -> gestor.asignarSiguiente(id));
            }

            assertEquals(antes, foto());
            assertEquals(List.of("T004", "T005"), idsDe(gestor.consultarFila()));
        }

        @Test
        @DisplayName("Un turno nunca queda en dos ventanillas a la vez (RN-05)")
        void turnoEnUnaSolaVentanilla() throws OperacionInvalidaException {
            registrar(3);
            gestor.asignarSiguiente("V01");
            gestor.asignarSiguiente("V02");
            gestor.asignarSiguiente("V03");

            Set<String> turnosEnAtencion = new HashSet<>();
            for (EstadoVentanilla ventanilla : gestor.consultarVentanillas()) {
                assertTrue(turnosEnAtencion.add(ventanilla.turnoActual().getId()));
            }
            assertEquals(3, turnosEnAtencion.size());
        }

        @Test
        @DisplayName("Ventanilla escrita con minúsculas y espacios se acepta")
        void ventanillaSeNormaliza() throws OperacionInvalidaException {
            registrar(1);

            Turno turno = gestor.asignarSiguiente("  v01 ");

            assertEquals("V01", turno.getVentanillaAsignada());
        }

        @ParameterizedTest
        @DisplayName("Ventanillas inexistentes o vacías se rechazan sin cambios (RN-03)")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "V04", "V00", "V1", "VENTANILLA", "V01; DROP TABLE", "../V01"})
        void ventanillaInvalidaSeRechaza(String idVentanilla) {
            registrar(2);
            String antes = foto();

            assertThrows(OperacionInvalidaException.class, () -> gestor.asignarSiguiente(idVentanilla));

            assertEquals(antes, foto());
        }
    }

    @Nested
    @DisplayName("Finalización (RF-06, RF-07, RN-08)")
    class Finalizacion {

        @Test
        @DisplayName("Finalizar deja el turno Atendido y libera la ventanilla")
        void finalizarLiberaVentanilla() throws OperacionInvalidaException {
            registrar(1);
            gestor.asignarSiguiente("V01");

            Turno atendido = gestor.finalizarAtencion("V01");

            assertEquals(EstadoTurno.ATENDIDO, atendido.getEstado());
            assertTrue(ventanilla("V01").estaLibre());
        }

        @Test
        @DisplayName("CV-04: tras finalizar en V02, V02 recibe el siguiente pendiente")
        void cv04ReasignaTrasFinalizar() throws OperacionInvalidaException {
            registrar(3);
            gestor.asignarSiguiente("V02");
            gestor.asignarSiguiente("V01");

            gestor.finalizarAtencion("V02");
            Turno siguiente = gestor.asignarSiguiente("V02");

            assertEquals("T003", siguiente.getId());
            assertEquals("V02", siguiente.getVentanillaAsignada());
        }

        @Test
        @DisplayName("RF-07: finalizar en ventanilla libre se rechaza y no toca otros turnos")
        void finalizarEnVentanillaLibreSeRechaza() throws OperacionInvalidaException {
            registrar(2);
            gestor.asignarSiguiente("V01");
            String antes = foto();

            OperacionInvalidaException error = assertThrows(OperacionInvalidaException.class,
                    () -> gestor.finalizarAtencion("V02"));

            assertTrue(error.getMessage().contains("V02"));
            assertEquals(antes, foto());
        }

        @Test
        @DisplayName("Finalizar solo afecta a la ventanilla indicada")
        void finalizarNoAfectaOtrasVentanillas() throws OperacionInvalidaException {
            registrar(3);
            gestor.asignarSiguiente("V01");
            gestor.asignarSiguiente("V02");
            gestor.asignarSiguiente("V03");

            gestor.finalizarAtencion("V02");

            assertFalse(ventanilla("V01").estaLibre());
            assertTrue(ventanilla("V02").estaLibre());
            assertFalse(ventanilla("V03").estaLibre());
        }

        @Test
        @DisplayName("No se puede finalizar dos veces la misma atención")
        void noSeFinalizaDosVeces() throws OperacionInvalidaException {
            registrar(1);
            gestor.asignarSiguiente("V01");
            gestor.finalizarAtencion("V01");
            String antes = foto();

            assertThrows(OperacionInvalidaException.class, () -> gestor.finalizarAtencion("V01"));

            assertEquals(antes, foto());
        }

        @ParameterizedTest
        @DisplayName("Ventanilla inexistente o vacía se rechaza")
        @NullAndEmptySource
        @ValueSource(strings = {"V09", " "})
        void ventanillaInvalidaSeRechaza(String idVentanilla) {
            assertThrows(OperacionInvalidaException.class, () -> gestor.finalizarAtencion(idVentanilla));
        }
    }

    @Nested
    @DisplayName("Cancelación (RF-08, RF-09, RN-07, RN-09)")
    class Cancelacion {

        @Test
        @DisplayName("CV-05: T2 cancelado sale de la fila, sigue en historial y la asignación lo omite")
        void cv05CancelaYOmite() throws OperacionInvalidaException {
            registrar(3);

            Turno cancelado = gestor.cancelarTurno("T002");

            assertEquals(EstadoTurno.CANCELADO, cancelado.getEstado());
            assertEquals(List.of("T001", "T003"), idsDe(gestor.consultarFila()));
            assertEquals(3, gestor.consultarHistorial().size());
            assertEquals("T001", gestor.asignarSiguiente("V01").getId());
            assertEquals("T003", gestor.asignarSiguiente("V02").getId());
        }

        @Test
        @DisplayName("Cancelar el primero de la fila hace que el segundo pase a ser el siguiente")
        void cancelarElPrimero() throws OperacionInvalidaException {
            registrar(2);

            gestor.cancelarTurno("T001");

            assertEquals("T002", gestor.asignarSiguiente("V01").getId());
        }

        @Test
        @DisplayName("CV-06: un turno En atención no se cancela y la ventanilla sigue ocupada")
        void cv06NoSeCancelaEnAtencion() throws OperacionInvalidaException {
            registrar(1);
            gestor.asignarSiguiente("V01");
            String antes = foto();

            assertThrows(OperacionInvalidaException.class, () -> gestor.cancelarTurno("T001"));

            assertEquals(antes, foto());
            assertFalse(ventanilla("V01").estaLibre());
        }

        @Test
        @DisplayName("Un turno Atendido no se cancela")
        void noSeCancelaAtendido() throws OperacionInvalidaException {
            registrar(1);
            gestor.asignarSiguiente("V01");
            gestor.finalizarAtencion("V01");
            String antes = foto();

            assertThrows(OperacionInvalidaException.class, () -> gestor.cancelarTurno("T001"));

            assertEquals(antes, foto());
        }

        @Test
        @DisplayName("Un turno Cancelado no se cancela otra vez")
        void noSeCancelaDosVeces() throws OperacionInvalidaException {
            registrar(1);
            gestor.cancelarTurno("T001");
            String antes = foto();

            assertThrows(OperacionInvalidaException.class, () -> gestor.cancelarTurno("T001"));

            assertEquals(antes, foto());
        }

        @Test
        @DisplayName("ID inexistente se rechaza e indica el ID")
        void idInexistenteSeRechaza() {
            registrar(2);
            String antes = foto();

            OperacionInvalidaException error = assertThrows(OperacionInvalidaException.class,
                    () -> gestor.cancelarTurno("T099"));

            assertTrue(error.getMessage().contains("T099"));
            assertEquals(antes, foto());
        }

        @Test
        @DisplayName("El ID se acepta con minúsculas y espacios alrededor")
        void idSeNormaliza() throws OperacionInvalidaException {
            registrar(1);

            assertEquals("T001", gestor.cancelarTurno("  t001 ").getId());
        }

        @ParameterizedTest
        @DisplayName("RNF-S04: IDs vacíos o malformados se rechazan sin cambios")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "1", "T", "T1", "T-1", "TABC", "T001; DROP TABLE turnos",
                "T001 OR 1=1", "' OR ''='", "<script>", "T99999999999999999999999"})
        void idMalformadoSeRechaza(String idTurno) {
            registrar(2);
            String antes = foto();

            assertThrows(OperacionInvalidaException.class, () -> gestor.cancelarTurno(idTurno));

            assertEquals(antes, foto());
        }
    }

    @Nested
    @DisplayName("Consultas: fila e historial (RF-03, RF-10, RN-09)")
    class Consultas {

        @Test
        @DisplayName("RF-03: la fila solo tiene pendientes y en orden FIFO")
        void filaSoloPendientesEnOrden() throws OperacionInvalidaException {
            registrar(5);
            gestor.asignarSiguiente("V01");
            gestor.cancelarTurno("T003");

            assertEquals(List.of("T002", "T004", "T005"), idsDe(gestor.consultarFila()));
        }

        @Test
        @DisplayName("RF-10: el historial muestra los cuatro estados")
        void historialConLosCuatroEstados() throws OperacionInvalidaException {
            registrar(4);
            gestor.asignarSiguiente("V01");
            gestor.finalizarAtencion("V01");
            gestor.asignarSiguiente("V02");
            gestor.cancelarTurno("T003");

            List<Turno> historial = gestor.consultarHistorial();

            assertEquals(EstadoTurno.ATENDIDO, historial.get(0).getEstado());
            assertEquals(EstadoTurno.EN_ATENCION, historial.get(1).getEstado());
            assertEquals(EstadoTurno.CANCELADO, historial.get(2).getEstado());
            assertEquals(EstadoTurno.EN_ESPERA, historial.get(3).getEstado());
        }

        @Test
        @DisplayName("Los atendidos y cancelados no regresan a la fila (RN-09)")
        void terminalesNoVuelvenALaFila() throws OperacionInvalidaException {
            registrar(2);
            gestor.asignarSiguiente("V01");
            gestor.finalizarAtencion("V01");
            gestor.cancelarTurno("T002");

            assertTrue(gestor.consultarFila().isEmpty());
            assertEquals(2, gestor.consultarHistorial().size());
        }

        @Test
        @DisplayName("Las consultas devuelven copias: modificarlas no altera el gestor")
        void consultasDevuelvenCopias() {
            registrar(2);

            gestor.consultarHistorial().clear();
            gestor.consultarFila().clear();

            assertEquals(2, gestor.consultarHistorial().size());
            assertEquals(2, gestor.consultarFila().size());
        }
    }

    @Nested
    @DisplayName("Recuperación de datos (ERS 3.3, RD-03)")
    class Recuperacion {

        @Test
        @DisplayName("Al recuperar turnos, el siguiente conserva el orden y no reutiliza secuencias (CV-07, CV-08)")
        void siguienteSecuenciaContinua() throws OperacionInvalidaException {
            registrar(3);
            gestor.asignarSiguiente("V01");
            gestor.cancelarTurno("T002");

            GestorTurnos recuperado = new GestorTurnos(reloj, gestor.consultarHistorial());
            Turno nuevo = recuperado.registrarTurno();

            assertEquals("T004", nuevo.getId());
            assertFalse(ventanilla("V01").estaLibre());
            assertEquals(List.of("T003", "T004"), idsDe(recuperado.consultarFila()));
        }

        @Test
        @DisplayName("Los turnos recuperados desordenados se ordenan por secuencia")
        void recuperadosSeOrdenan() {
            LocalDateTime fecha = LocalDateTime.of(2026, 11, 11, 10, 0);
            List<Turno> desordenados = List.of(new Turno(3, fecha), new Turno(1, fecha), new Turno(2, fecha));

            GestorTurnos recuperado = new GestorTurnos(reloj, desordenados);

            assertEquals(List.of("T001", "T002", "T003"), idsDe(recuperado.consultarFila()));
        }

        @Test
        @DisplayName("Secuencias repetidas se rechazan")
        void secuenciasRepetidasSeRechazan() {
            LocalDateTime fecha = LocalDateTime.of(2026, 11, 11, 10, 0);
            List<Turno> repetidos = List.of(new Turno(1, fecha), new Turno(1, fecha));

            assertThrows(IllegalArgumentException.class, () -> new GestorTurnos(reloj, repetidos));
        }

        @Test
        @DisplayName("Dos turnos En atención en la misma ventanilla se rechazan")
        void dosEnAtencionEnLaMismaVentanillaSeRechazan() {
            LocalDateTime fecha = LocalDateTime.of(2026, 11, 11, 10, 0);
            List<Turno> invalidos = List.of(
                    Turno.restaurar(1, fecha, EstadoTurno.EN_ATENCION, "V01"),
                    Turno.restaurar(2, fecha, EstadoTurno.EN_ATENCION, "V01"));

            assertThrows(IllegalArgumentException.class, () -> new GestorTurnos(reloj, invalidos));
        }

        @Test
        @DisplayName("Si los datos nuevos son inválidos, el estado anterior se conserva")
        void reemplazoInvalidoConservaEstado() {
            registrar(2);
            String antes = foto();
            LocalDateTime fecha = LocalDateTime.of(2026, 11, 11, 10, 0);
            List<Turno> invalidos = List.of(new Turno(5, fecha), new Turno(5, fecha));

            assertThrows(IllegalArgumentException.class, () -> gestor.reemplazarTurnos(invalidos));

            assertEquals(antes, foto());
        }
    }

    @Nested
    @DisplayName("Invariantes bajo operaciones aleatorias")
    class Invariantes {

        @Test
        @DisplayName("Con miles de operaciones al azar se cumplen las invariantes de la ERS 3.3")
        void invariantesSeConservan() {
            Random azar = new Random(20261111L);
            long ultimaSecuenciaAsignada = 0;
            for (int paso = 0; paso < 3000; paso++) {
                int opcion = azar.nextInt(4);
                String ventanilla = Ventanillas.IDS.get(azar.nextInt(3));
                try {
                    if (opcion == 0) {
                        gestor.registrarTurno();
                    } else if (opcion == 1) {
                        long secuencia = gestor.asignarSiguiente(ventanilla).getSecuencia();
                        assertTrue(secuencia > ultimaSecuenciaAsignada,
                                "La asignación debe respetar el orden FIFO");
                        ultimaSecuenciaAsignada = secuencia;
                    } else if (opcion == 2) {
                        gestor.finalizarAtencion(ventanilla);
                    } else {
                        gestor.cancelarTurno(Turno.generarId(1 + azar.nextInt(40)));
                    }
                } catch (OperacionInvalidaException e) {
                    // Los rechazos son esperables; lo importante es que nunca rompan las invariantes.
                }
                verificarInvariantes();
            }
        }

        private void verificarInvariantes() {
            Set<String> ventanillasUsadas = new HashSet<>();
            long anterior = 0;
            for (Turno turno : gestor.consultarHistorial()) {
                assertTrue(turno.getSecuencia() > anterior, "Secuencias únicas y crecientes");
                anterior = turno.getSecuencia();
                if (turno.getEstado() == EstadoTurno.EN_ATENCION) {
                    assertTrue(ventanillasUsadas.add(turno.getVentanillaAsignada()),
                            "Una ventanilla no puede tener dos turnos En atención");
                }
            }
            long anteriorEnFila = 0;
            for (Turno turno : gestor.consultarFila()) {
                assertEquals(EstadoTurno.EN_ESPERA, turno.getEstado());
                assertTrue(turno.getSecuencia() > anteriorEnFila, "La fila está ordenada por secuencia");
                anteriorEnFila = turno.getSecuencia();
            }
            int ocupadas = 0;
            for (EstadoVentanilla ventanilla : gestor.consultarVentanillas()) {
                if (!ventanilla.estaLibre()) {
                    ocupadas++;
                }
            }
            assertEquals(ventanillasUsadas.size(), ocupadas);
            assertTrue(ocupadas <= 3);
        }
    }
}