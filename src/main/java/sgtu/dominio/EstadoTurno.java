package sgtu.dominio;

/**
 * Estados posibles de un turno (ERS sección 3.2).
 * "Atendido" y "Cancelado" son estados terminales.
 */
public enum EstadoTurno {
    EN_ESPERA("En espera"),
    EN_ATENCION("En atención"),
    ATENDIDO("Atendido"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoTurno(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}