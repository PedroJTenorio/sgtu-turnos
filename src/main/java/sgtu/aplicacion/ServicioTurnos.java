package sgtu.aplicacion;

import java.time.Clock;
import java.util.List;
import java.util.function.Function;
import sgtu.dominio.EstadoVentanilla;
import sgtu.dominio.GestorTurnos;
import sgtu.dominio.OperacionInvalidaException;
import sgtu.dominio.Turno;
import sgtu.persistencia.IRepositorioTurnos;
import sgtu.persistencia.PersistenciaException;

/**
 * Coordina el dominio con el almacenamiento. Cada operación que cambia datos
 * se aplica, se guarda y solo entonces se confirma; si el guardado falla se
 * restaura el estado anterior y no se anuncia éxito (RF-12, RD-02).
 * Está pensado para un único operador local, por lo que no es seguro entre hilos (RC-02).
 */
public class ServicioTurnos {

    /** Operación del dominio que devuelve el turno afectado. */
    @FunctionalInterface
    private interface OperacionDominio {
        Turno ejecutar() throws OperacionInvalidaException;
    }

    private final GestorTurnos gestor;
    private final IRepositorioTurnos repositorio;

    private ServicioTurnos(GestorTurnos gestor, IRepositorioTurnos repositorio) {
        this.gestor = gestor;
        this.repositorio = repositorio;
    }

    /**
     * Arranca el servicio recuperando los datos guardados. Si no se pueden leer o son
     * inválidos lanza la excepción y no escribe nada, para no pisar los datos (RD-03).
     */
    public static ServicioTurnos iniciar(IRepositorioTurnos repositorio, Clock reloj)
            throws PersistenciaException {
        List<Turno> guardados = repositorio.cargar();
        try {
            return new ServicioTurnos(new GestorTurnos(reloj, guardados), repositorio);
        } catch (IllegalArgumentException e) {
            throw new PersistenciaException(
                    "Los datos guardados son inválidos: " + e.getMessage() + " No se modificaron.", e);
        }
    }

    /** RF-02, RF-12: registrar un turno nunca es rechazado por el dominio, solo puede fallar al guardar. */
    public ResultadoOperacion registrarTurno() throws PersistenciaException {
        List<Turno> instantanea = tomarInstantanea();
        Turno nuevo = gestor.registrarTurno();
        return guardarYConfirmar(nuevo, instantanea,
                turno -> "Turno " + turno.getId() + " registrado: queda En espera.");
    }

    public ResultadoOperacion asignarSiguiente(String idVentanilla)
            throws OperacionInvalidaException, PersistenciaException {
        return ejecutarYGuardar(() -> gestor.asignarSiguiente(idVentanilla),
                turno -> "Turno " + turno.getId() + " asignado a la ventanilla "
                        + turno.getVentanillaAsignada() + ": atención iniciada.");
    }

    public ResultadoOperacion finalizarAtencion(String idVentanilla)
            throws OperacionInvalidaException, PersistenciaException {
        return ejecutarYGuardar(() -> gestor.finalizarAtencion(idVentanilla),
                turno -> "Atención del turno " + turno.getId() + " finalizada: la ventanilla "
                        + turno.getVentanillaAsignada() + " queda libre.");
    }

    public ResultadoOperacion cancelarTurno(String idTurno)
            throws OperacionInvalidaException, PersistenciaException {
        return ejecutarYGuardar(() -> gestor.cancelarTurno(idTurno),
                turno -> "Turno " + turno.getId() + " cancelado.");
    }

    public List<EstadoVentanilla> consultarVentanillas() {
        return gestor.consultarVentanillas();
    }

    public List<Turno> consultarFila() {
        return gestor.consultarFila();
    }

    public List<Turno> consultarHistorial() {
        return gestor.consultarHistorial();
    }

    // Copia de todos los turnos para poder deshacer si el guardado falla.
    private List<Turno> tomarInstantanea() {
        return gestor.consultarHistorial();
    }

    /**
     * Aplica la operación, guarda y confirma. Una operación rechazada por el dominio
     * no cambia nada, así que ni siquiera se intenta guardar (RNF-R02).
     */
    private ResultadoOperacion ejecutarYGuardar(OperacionDominio operacion, Function<Turno, String> mensaje)
            throws OperacionInvalidaException, PersistenciaException {
        List<Turno> instantanea = tomarInstantanea();
        Turno afectado = operacion.ejecutar();
        return guardarYConfirmar(afectado, instantanea, mensaje);
    }

    // Guarda el estado actual; si falla, restaura la instantánea y no anuncia éxito (RD-02).
    private ResultadoOperacion guardarYConfirmar(Turno afectado, List<Turno> instantanea,
                                                 Function<Turno, String> mensaje) throws PersistenciaException {
        try {
            repositorio.guardar(gestor.consultarHistorial());
        } catch (PersistenciaException e) {
            gestor.reemplazarTurnos(instantanea);
            throw new PersistenciaException(e.getMessage() + " La operación no se aplicó.", e);
        }
        return new ResultadoOperacion(afectado, mensaje.apply(afectado));
    }
}
