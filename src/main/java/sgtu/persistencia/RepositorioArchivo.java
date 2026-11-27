package sgtu.persistencia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import sgtu.dominio.EstadoTurno;
import sgtu.dominio.Turno;

/**
 * Guarda los turnos en un archivo de texto local (RD-01, RC-04), una línea por turno:
 * <pre>
 *   (Encabezado) SGTU;1
 *   (Campos)     secuencia;fechaHoraISO;ESTADO;ventanilla
 * </pre>
 * El guardado escribe primero un archivo temporal y luego lo mueve sobre el definitivo,
 * así un fallo a medias nunca deja el archivo real con datos parciales (RD-02).
 * La lectura nunca modifica el archivo: si está dañado solo informa (RD-03).
 */
public class RepositorioArchivo implements IRepositorioTurnos {

    private static final String ENCABEZADO = "SGTU;1";
    private static final String SEPARADOR = ";";
    private static final int CAMPOS_POR_TURNO = 4;

    private final Path ruta;

    public RepositorioArchivo(Path ruta) {
        this.ruta = ruta;
    }

    @Override
    public List<Turno> cargar() throws PersistenciaException {
        if (!Files.exists(ruta)) {
            return new ArrayList<>();
        }
        List<String> lineas = leerLineas();
        validarEncabezado(lineas);
        List<Turno> turnos = new ArrayList<>();
        for (int i = 1; i < lineas.size(); i++) {
            turnos.add(interpretarLinea(lineas.get(i), i + 1));
        }
        return turnos;
    }

    @Override
    public void guardar(List<Turno> turnos) throws PersistenciaException {
        List<String> lineas = new ArrayList<>();
        lineas.add(ENCABEZADO);
        for (Turno turno : turnos) {
            lineas.add(convertirALinea(turno));
        }
        // El archivo temporal se guarda en la misma carpeta que el definitivo y con la terminación ".tmp"
        // para distinguirlo.
        Path temporal = ruta.resolveSibling(ruta.getFileName() + ".tmp");
        try {
            Path carpeta = ruta.toAbsolutePath().getParent();
            Files.createDirectories(carpeta);
            Files.write(temporal, lineas, StandardCharsets.UTF_8);
            moverSobreDefinitivo(temporal);
        } catch (IOException e) {
            borrarTemporal(temporal);
            throw new PersistenciaException("No fue posible guardar los datos en " + ruta + ": " + e.getMessage(), e);
        }
    }

    private List<String> leerLineas() throws PersistenciaException {
        try {
            return Files.readAllLines(ruta, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("No fue posible leer el archivo de datos " + ruta
                    + ". No se modificó. Detalle: " + e.getMessage(), e);
        }
    }

    private void validarEncabezado(List<String> lineas) throws PersistenciaException {
        if (lineas.isEmpty() || !ENCABEZADO.equals(lineas.get(0))) {
            throw new PersistenciaException("El archivo de datos " + ruta
                    + " está dañado o no tiene el formato esperado (encabezado). No se modificó.");
        }
    }

    private Turno interpretarLinea(String linea, int numeroLinea) throws PersistenciaException {
        // El -1 como segundo argumento conserva los campos vacíos del final: un turno sin ventanilla
        // termina con un campo "".
        String[] campos = linea.split(SEPARADOR, -1);
        if (campos.length != CAMPOS_POR_TURNO) {
            throw lineaInvalida(numeroLinea, "Se esperaban " + CAMPOS_POR_TURNO + " campos.", null);
        }
        long secuencia = leerSecuencia(campos[0], numeroLinea);
        LocalDateTime fecha = leerFecha(campos[1], numeroLinea);
        EstadoTurno estado = leerEstado(campos[2], numeroLinea);
        String ventanilla = null;
        if (!campos[3].isEmpty()) {
            ventanilla = campos[3];
        }
        try {
            // Reconstrucción del turno con los campos leídos; valida que estado y ventanilla sean coherentes.
            return Turno.restaurar(secuencia, fecha, estado, ventanilla);
        } catch (IllegalArgumentException e) {
            throw lineaInvalida(numeroLinea, e.getMessage(), e);
        }
    }

    private long leerSecuencia(String texto, int numeroLinea) throws PersistenciaException {
        try {
            return Long.parseLong(texto);
        } catch (NumberFormatException e) {
            throw lineaInvalida(numeroLinea, "La secuencia no es un número", e);
        }
    }

    private LocalDateTime leerFecha(String texto, int numeroLinea) throws PersistenciaException {
        try {
            return LocalDateTime.parse(texto);
        } catch (DateTimeParseException e) {
            throw lineaInvalida(numeroLinea, "La fecha y hora no son válidas", e);
        }
    }

    private EstadoTurno leerEstado(String texto, int numeroLinea) throws PersistenciaException {
        try {
            return EstadoTurno.valueOf(texto);
        } catch (IllegalArgumentException e) {
            throw lineaInvalida(numeroLinea, "Estado de turno no permitido", e);
        }
    }

    private PersistenciaException lineaInvalida(int numeroLinea, String motivo, Throwable causa) {
        String mensaje = "El archivo de datos " + ruta + " está dañado (línea "
                + numeroLinea + ": " + motivo + "). No se modificó.";
        if (causa == null) {
            return new PersistenciaException(mensaje);
        }
        return new PersistenciaException(mensaje, causa);
    }

    private String convertirALinea(Turno turno) {
        String ventanilla = "";
        if (turno.getVentanillaAsignada() != null) {
            ventanilla = turno.getVentanillaAsignada();
        }
        return turno.getSecuencia() + SEPARADOR + turno.getFechaHoraRegistro() + SEPARADOR
                + turno.getEstado().name() + SEPARADOR + ventanilla;
    }

    private void moverSobreDefinitivo(Path temporal) throws IOException {
        try {
            Files.move(temporal, ruta, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Algunos sistemas de archivos no permiten el movimiento atómico.
            Files.move(temporal, ruta, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // Se usa cuando el guardado falla: elimina el temporal para que no queden restos junto a los datos.
    private void borrarTemporal(Path temporal) {
        try {
            Files.deleteIfExists(temporal);
        } catch (IOException e) {
            // Si no se puede borrar el temporal no afecta a los datos reales; se ignora a propósito.
        }
    }
}
