package sgtu.dominio;

/**
 * Vista de una ventanilla en un momento dado (RF-01).
 * La disponibilidad se deriva de si existe un turno en atención asignado a ella.
 *
 * @param id          identificador V01, V02 o V03
 * @param turnoActual copia del turno en atención, o null si está libre
 */
public record EstadoVentanilla(String id, Turno turnoActual) {

    public boolean estaLibre() {
        return turnoActual == null;
    }
}