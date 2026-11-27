package sgtu.aplicacion;

import sgtu.dominio.Turno;

/**
 * Resultado de una operación confirmada y guardada.
 *
 * @param turno   copia del turno afectado
 * @param mensaje confirmación para el operador (RF-12)
 */
public record ResultadoOperacion(Turno turno, String mensaje) {
}
