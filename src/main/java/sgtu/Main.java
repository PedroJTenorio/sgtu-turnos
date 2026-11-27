package sgtu;

import java.nio.file.Path;
import java.time.Clock;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import sgtu.aplicacion.ServicioTurnos;
import sgtu.persistencia.PersistenciaException;
import sgtu.persistencia.RepositorioArchivo;
import sgtu.ui.VentanaPrincipal;

/**
 * Punto de entrada. Uso: {@code java -jar sgtu.jar [ruta-del-archivo-de-datos]}.
 * Si no se indica ruta se usa datos/turnos.txt dentro de la carpeta actual.
 */
public final class Main {

    private static final String RUTA_PREDETERMINADA = "datos/turnos.txt";

    private Main() {
        // Clase de arranque: no se instancia.
    }

    public static void main(String[] args) {
        String ruta = RUTA_PREDETERMINADA;
        if (args.length > 0) {
            ruta = args[0];
        }
        try {
            RepositorioArchivo repositorio = new RepositorioArchivo(Path.of(ruta));
            ServicioTurnos servicio = ServicioTurnos.iniciar(repositorio, Clock.systemDefaultZone());
            SwingUtilities.invokeLater(() -> new VentanaPrincipal(servicio).setVisible(true));
        } catch (PersistenciaException e) {
            // RD-03: se informa y se termina sin escribir, para no pisar los datos existentes.
            JOptionPane.showMessageDialog(null, e.getMessage(), "No se pudo iniciar el SGTU",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
