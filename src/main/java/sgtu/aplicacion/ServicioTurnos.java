package sgtu.aplicacion;

import java.time.Clock;
import java.util.List;
import sgtu.dominio.EstadoVentanilla;
import sgtu.dominio.GestorTurnos;
import sgtu.dominio.OperacionInvalidaException;
import sgtu.dominio.Turno;
import sgtu.persistencia.PersistenciaException;
import sgtu.persistencia.IRepositorioTurnos;

/*
 Coordina el dominio con el almacenamiento. Cada operación que cambia datos
 se aplica, se guarda y solo entonces se confirma; si el guardado falla se
 restaura el estado anterior y no se anuncia éxito (RF-12, RD-02).
 Está pensado para un único operador local, por lo que no es seguro entre hilos (RC-02).
 */
public class ServicioTurnos {

    // Operación del dominio que devuelve el turno afectado.
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

    /*
     Arranca el servicio recuperando los datos guardados. Si no se pueden leer o son
     inválidos lanza la excepción y no escribe nada, para no pisar los datos (RD-03).
    */
    public static ServicioTurnos iniciar(IRepositorioTurnos repositorio, Clock reloj) throws PersistenciaException {
        List<Turno> guardados = repositorio.cargar();
        try {
            return new ServicioTurnos(new GestorTurnos(reloj, guardados), repositorio);
        } catch (IllegalArgumentException e) {
            throw new PersistenciaException("Los datos guardados son inválidos: " + e.getMessage() + " No se modificaron.", e);
        }
    }

    public ResultadoOperacion registrarTurno() throws PersistenciaException {
        try {
            return ejecutarYGuardar(() -> gestor.registrarTurno(), "Turno %1$s registrado: queda En espera.");
        } catch (OperacionInvalidaException e) {
            // registrarTurno no tiene reglas que puedan rechazarlo; esto indicaría un error de programación.
            throw new IllegalStateException("Registrar un turno no debería ser rechazado.", e);
        }
    }

    public ResultadoOperacion asignarSiguiente(String idVentanilla)
            throws OperacionInvalidaException, PersistenciaException {
        return ejecutarYGuardar(() -> gestor.asignarSiguiente(idVentanilla),
                "Turno %1$s asignado a la ventanilla %2$s: atención iniciada.");
    }

    public ResultadoOperacion finalizarAtencion(String idVentanilla)
            throws OperacionInvalidaException, PersistenciaException {
        return ejecutarYGuardar(() -> gestor.finalizarAtencion(idVentanilla),
                "Atención del turno %1$s finalizada: la ventanilla %2$s queda libre.");
    }

    public ResultadoOperacion cancelarTurno(String idTurno)
            throws OperacionInvalidaException, PersistenciaException {
        return ejecutarYGuardar(() -> gestor.cancelarTurno(idTurno), "Turno %1$s cancelado.");
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

    /*
     Aplica la operación, guarda y confirma. Una operación rechazada por el dominio
     no cambia nada, así que ni siquiera se intenta guardar (RNF-R02).
    */
    private ResultadoOperacion ejecutarYGuardar(OperacionDominio operacion, String plantillaMensaje)
            throws OperacionInvalidaException, PersistenciaException {
        List<Turno> respaldo = gestor.consultarHistorial();
        Turno afectado = operacion.ejecutar();
        try {
            repositorio.guardar(gestor.consultarHistorial());
        } catch (PersistenciaException e) {
            gestor.reemplazarTurnos(respaldo);
            throw new PersistenciaException(e.getMessage() + " La operación no se aplicó.", e);
        }
        String mensaje = String.format(plantillaMensaje, afectado.getId(), afectado.getVentanillaAsignada());
        return new ResultadoOperacion(afectado, mensaje);
    }
}
