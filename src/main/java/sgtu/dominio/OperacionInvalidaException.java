package sgtu.dominio;

/**
 * Se lanza cuando una operación viola una regla de negocio de la ERS.
 * El mensaje explica la causa para mostrarlo tal cual al operador (RNF-U02).
 */
public class OperacionInvalidaException extends Exception {

    private static final long serialVersionUID = 1L;

    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}