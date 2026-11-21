package sgtu.aplicacion;

import sgtu.dominio.Turno;

/*
Clase de utilidad para ServicioTurnos
 Resultado de una operación confirmada y guardada.
 parametro 1: turno -> copia del turno afectado
 parametro 2: mensaje -> confirmación para el operador (RF-12)
*/

public record ResultadoOperacion(Turno turno, String mensaje) {
}
