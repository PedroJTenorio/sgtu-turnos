package sgtu.persistencia;

import sgtu.dominio.Turno;

import java.util.List;


 // Contrato de almacenamiento. Se usan contratos para que el dominio no dependa exclusivamente de
 // una clase con una tecnología específica.
 // en las pruebas se sustituye por una versión en memoria o que falla a propósito.

public interface IRepositorioTurnos {

    // Devuelve los turnos guardados, o una lista vacía si todavía no hay datos.
    List<Turno> cargar() throws PersistenciaException;

    // Guarda todos los turnos de forma completa o no los guarda (RD-02).
    void guardar(List<Turno> turnos) throws PersistenciaException;
}

