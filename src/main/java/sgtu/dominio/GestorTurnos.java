package sgtu.dominio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/*
 Núcleo del dominio: cola FIFO única, tres ventanillas y transiciones de estado.
 No conoce la interfaz ni el almacenamiento (RNF-M01), y valida todas las reglas
 aunque se invoque sin pasar por la interfaz (RNF-S01).
 El orden FIFO depende solo de la secuencia, nunca del reloj ni del texto del ID (RN-02).
 Todas las consultas devuelven copias, así que quien consulta no puede alterar el estado.
 */
public class GestorTurnos {

    private static final Pattern FORMATO_ID_TURNO = Pattern.compile("T\\d{3,}");

    private final Clock reloj;
    private final List<Turno> turnos = new ArrayList<>();

    public GestorTurnos(Clock reloj) {
        this.reloj = Objects.requireNonNull(reloj, "El reloj es obligatorio.");
    }

    // Crea el gestor a partir de turnos ya existentes (por ejemplo, los recuperados al reiniciar).
    public GestorTurnos(Clock reloj, List<Turno> turnosExistentes) {
        this(reloj);
        reemplazarTurnos(turnosExistentes);
    }

    // Operaciones que cambian el estado

    // RF-02: crea un turno En espera al final de la cola.
    public Turno registrarTurno() {
        long secuencia = calcularSiguienteSecuencia();
        Turno nuevo = new Turno(secuencia, LocalDateTime.now(reloj));
        turnos.add(nuevo);
        return nuevo.copiar();
    }

    // RF-04, RF-05, RN-06, RN-10: asigna el turno más antiguo a una ventanilla libre.
    public Turno asignarSiguiente(String idVentanilla) throws OperacionInvalidaException {
        String ventanilla = validarVentanilla(idVentanilla);
        Turno ocupante = buscarTurnoEnAtencion(ventanilla);
        if (ocupante != null) {
            throw new OperacionInvalidaException("La ventanilla " + ventanilla + " está ocupada con el turno " + ocupante.getId() + ". Finalice esa atención antes de asignar otro turno.");
        }
        Turno siguiente = buscarPrimeroEnEspera();
        if (siguiente == null) {
            throw new OperacionInvalidaException("No hay turnos en espera para asignar a la ventanilla " + ventanilla + ".");
        }
        siguiente.iniciarAtencion(ventanilla);
        return siguiente.copiar();
    }

    // RF-06, RF-07, RN-08: finaliza el turno en atención de una ventanilla.
    public Turno finalizarAtencion(String idVentanilla) throws OperacionInvalidaException {
        String ventanilla = validarVentanilla(idVentanilla);
        Turno enAtencion = buscarTurnoEnAtencion(ventanilla);
        if (enAtencion == null) {
            throw new OperacionInvalidaException("La ventanilla " + ventanilla + " está libre: no hay una atención que finalizar.");
        }
        enAtencion.finalizar();
        return enAtencion.copiar();
    }

    // RF-08, RF-09, RN-07: cancela un turno que sigue En espera.
    public Turno cancelarTurno(String idTurno) throws OperacionInvalidaException {
        long secuencia = validarIdTurno(idTurno);
        Turno turno = buscarPorSecuencia(secuencia);
        if (turno == null) {
            throw new OperacionInvalidaException("El turno " + Turno.generarId(secuencia) + " no existe.");
        }
        turno.cancelar();
        return turno.copiar();
    }

    // Consultas

    // RF-01: estado de V01, V02 y V03.
    public List<EstadoVentanilla> consultarVentanillas() {
        List<EstadoVentanilla> resultado = new ArrayList<>();
        for (String id : Ventanillas.IDS) {
            Turno actual = buscarTurnoEnAtencion(id);
            if (actual != null) {
                actual = actual.copiar();
            }
            resultado.add(new EstadoVentanilla(id, actual));
        }
        return resultado;
    }

    // RF-03: solo turnos En espera, por secuencia ascendente.
    public List<Turno> consultarFila() {
        List<Turno> fila = new ArrayList<>();
        for (Turno turno : turnos) {
            if (turno.getEstado() == EstadoTurno.EN_ESPERA) {
                fila.add(turno.copiar());
            }
        }
        return fila;
    }

    // RF-10: todos los turnos registrados, en cualquier estado.
    public List<Turno> consultarHistorial() {
        List<Turno> historial = new ArrayList<>();
        for (Turno turno : turnos) {
            historial.add(turno.copiar());
        }
        return historial;
    }

    /*
     Sustituye todos los turnos por los indicados, validando las invariantes de la
     ERS 3.3. Se usa al recuperar datos y al deshacer una operación cuyo guardado falló.

     @throws IllegalArgumentException si los datos violan alguna invariante
    */
    public void reemplazarTurnos(List<Turno> nuevosTurnos) {
        List<Turno> copia = new ArrayList<>();
        for (Turno turno : nuevosTurnos) {
            copia.add(turno.copiar());
        }
        copia.sort(Comparator.comparingLong(Turno::getSecuencia));
        validarInvariantes(copia);
        turnos.clear();
        turnos.addAll(copia);
    }

    // Apoyo interno

    // Los turnos nunca se eliminan, por lo que max + 1 jamás reutiliza una secuencia (ERS 3.3).
    private long calcularSiguienteSecuencia() {
        long maxima = 0;
        for (Turno turno : turnos) {
            if (turno.getSecuencia() > maxima) {
                maxima = turno.getSecuencia();
            }
        }
        return maxima + 1;
    }

    private void validarInvariantes(List<Turno> lista) {
        Set<Long> secuencias = new HashSet<>();
        Set<String> ventanillasOcupadas = new HashSet<>();
        for (Turno turno : lista) {
            if (!secuencias.add(turno.getSecuencia())) {
                throw new IllegalArgumentException("La secuencia " + turno.getSecuencia() + " está repetida.");
            }
            boolean ventanillaRepetida = turno.getEstado() == EstadoTurno.EN_ATENCION && !ventanillasOcupadas.add(turno.getVentanillaAsignada());
            if (ventanillaRepetida) {
                throw new IllegalArgumentException("La ventanilla " + turno.getVentanillaAsignada() + " tiene más de un turno en atención.");
            }
        }
    }

    private String validarVentanilla(String idVentanilla) throws OperacionInvalidaException {
        if (idVentanilla == null || idVentanilla.isBlank()) {
            throw new OperacionInvalidaException("Debe indicar una ventanilla (V01, V02 o V03).");
        }
        String normalizada = idVentanilla.trim().toUpperCase();
        if (!Ventanillas.existe(normalizada)) {
            throw new OperacionInvalidaException("La ventanilla '" + idVentanilla.trim() + "' no existe. Las ventanillas disponibles son V01, V02 y V03.");
        }
        return normalizada;
    }

    // RNF-S04: valida el formato antes de usar el identificador. Devuelve la secuencia.
    private long validarIdTurno(String idTurno) throws OperacionInvalidaException {
        if (idTurno == null || idTurno.isBlank()) {
            throw new OperacionInvalidaException("Debe indicar el identificador del turno (por ejemplo, T001).");
        }
        String normalizado = idTurno.trim().toUpperCase();
        if (!FORMATO_ID_TURNO.matcher(normalizado).matches()) {
            throw new OperacionInvalidaException("El identificador '" + idTurno.trim() + "' no es válido. Use el formato T001.");
        }
        try {
            return Long.parseLong(normalizado.substring(1));
        } catch (NumberFormatException e) {
            throw new OperacionInvalidaException("El identificador '" + idTurno.trim() + "' es demasiado grande para existir.");
        }
    }

    private Turno buscarTurnoEnAtencion(String idVentanilla) {
        for (Turno turno : turnos) {
            boolean esDeLaVentanilla = turno.getEstado() == EstadoTurno.EN_ATENCION && idVentanilla.equals(turno.getVentanillaAsignada());
            if (esDeLaVentanilla) {
                return turno;
            }
        }
        return null;
    }

    // La lista siempre está ordenada por secuencia, así que el primero En espera es el más antiguo.
    private Turno buscarPrimeroEnEspera() {
        for (Turno turno : turnos) {
            if (turno.getEstado() == EstadoTurno.EN_ESPERA) {
                return turno;
            }
        }
        return null;
    }

    private Turno buscarPorSecuencia(long secuencia) {
        for (Turno turno : turnos) {
            if (turno.getSecuencia() == secuencia) {
                return turno;
            }
        }
        return null;
    }
}