package sgtu.persistencia;

import java.util.List;
import sgtu.dominio.Turno;

/**
 * Contrato de almacenamiento. Se usa una interfaz para que el resto del sistema no dependa de una
 * tecnología concreta; en las pruebas se sustituye por una versión en memoria o que falla a propósito.
 */
public interface IRepositorioTurnos {

    /** Devuelve los turnos guardados, o una lista vacía si todavía no hay datos. */
    List<Turno> cargar() throws PersistenciaException;

    /** Guarda todos los turnos de forma completa o no los guarda (RD-02). */
    void guardar(List<Turno> turnos) throws PersistenciaException;
}
