package sgtu.persistencia;

/**
 * Se lanza cuando no se pueden leer o escribir los datos en el almacenamiento.
 * Es una excepción comprobada (igual que {@code OperacionInvalidaException}) para que quien
 * use el repositorio esté obligado a decidir qué hacer cuando el almacenamiento falla.
 */
public class PersistenciaException extends Exception {

    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensaje) {
        super(mensaje);
    }

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
