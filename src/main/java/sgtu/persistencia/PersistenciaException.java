package sgtu.persistencia;

//Clase usada para comunicar una excepción al momento de leer o escribir información en el repositorio
public class PersistenciaException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public PersistenciaException(String message) {
        super(message);
    }
    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
