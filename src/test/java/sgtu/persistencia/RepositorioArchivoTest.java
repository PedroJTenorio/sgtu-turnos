package sgtu.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sgtu.dominio.EstadoTurno;
import sgtu.dominio.Turno;


/*
    Clase para test de RepositorioArchivo
    La información acerca de cada test se encuentra en cada @DisplayName, además de que muchas
    de las pruebas poseen nombres y procedimientos bastante comprensibles.
 */
@DisplayName("RepositorioArchivo: guardar, recuperar y rechazar datos dañados")
class RepositorioArchivoTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 11, 10, 30, 15);

    @TempDir
    Path carpeta;
    //Turnos de prueba
    private List<Turno> turnosDeLosCuatroEstados() {
        return List.of(
                Turno.restaurar(1, FECHA, EstadoTurno.ATENDIDO, "V01"),
                Turno.restaurar(2, FECHA.plusSeconds(5), EstadoTurno.EN_ATENCION, "V02"),
                Turno.restaurar(3, FECHA.plusSeconds(10), EstadoTurno.CANCELADO, null),
                Turno.restaurar(4, FECHA.plusSeconds(15), EstadoTurno.EN_ESPERA, null));
    }

    @Test
    @DisplayName("Sin archivo previo se devuelve una lista vacía (primer arranque)")
    void sinArchivoDevuelveVacio() throws PersistenciaException {
        RepositorioArchivo repositorio = new RepositorioArchivo(carpeta.resolve("turnos.txt"));

        assertTrue(repositorio.cargar().isEmpty());
    }

    @Test
    @DisplayName("RNF-R01: se recuperan los cuatro estados, secuencias, fechas y ventanillas")
    void guardarYCargarConservaTodo() throws PersistenciaException {
        RepositorioArchivo repositorio = new RepositorioArchivo(carpeta.resolve("turnos.txt"));

        repositorio.guardar(turnosDeLosCuatroEstados());
        List<Turno> recuperados = repositorio.cargar();

        assertEquals(4, recuperados.size());
        assertEquals(EstadoTurno.ATENDIDO, recuperados.get(0).getEstado());
        assertEquals("V01", recuperados.get(0).getVentanillaAsignada());
        assertEquals(EstadoTurno.EN_ATENCION, recuperados.get(1).getEstado());
        assertEquals("V02", recuperados.get(1).getVentanillaAsignada());
        assertEquals(EstadoTurno.CANCELADO, recuperados.get(2).getEstado());
        assertNull(recuperados.get(2).getVentanillaAsignada());
        assertEquals(EstadoTurno.EN_ESPERA, recuperados.get(3).getEstado());
        assertEquals(FECHA.plusSeconds(15), recuperados.get(3).getFechaHoraRegistro());
        assertEquals(4, recuperados.get(3).getSecuencia());
    }

    @Test
    @DisplayName("Guardar dos veces reemplaza el contenido anterior por completo")
    void guardarReemplazaContenido() throws PersistenciaException {
        RepositorioArchivo repositorio = new RepositorioArchivo(carpeta.resolve("turnos.txt"));
        repositorio.guardar(turnosDeLosCuatroEstados());

        repositorio.guardar(List.of(new Turno(1, FECHA)));

        assertEquals(1, repositorio.cargar().size());
    }

    @Test
    @DisplayName("Guardar crea las carpetas que falten")
    void guardarCreaCarpetas() throws PersistenciaException {
        Path ruta = carpeta.resolve("datos").resolve("anidada").resolve("turnos.txt");

        new RepositorioArchivo(ruta).guardar(List.of(new Turno(1, FECHA)));

        assertTrue(Files.exists(ruta));
    }

    @Test
    @DisplayName("RD-02: tras guardar no queda archivo temporal")
    void noQuedaArchivoTemporal() throws PersistenciaException, IOException {
        RepositorioArchivo repositorio = new RepositorioArchivo(carpeta.resolve("turnos.txt"));

        repositorio.guardar(turnosDeLosCuatroEstados());

        try (var archivos = Files.list(carpeta)) {
            assertEquals(1, archivos.count());
        }
    }

    @Test
    @DisplayName("RD-02: si el guardado falla, el archivo anterior queda intacto")
    void fallaDeGuardadoNoDanaElArchivoAnterior() throws PersistenciaException, IOException {
        Path ruta = carpeta.resolve("turnos.txt");
        RepositorioArchivo repositorio = new RepositorioArchivo(ruta);
        repositorio.guardar(turnosDeLosCuatroEstados());
        String antes = Files.readString(ruta, StandardCharsets.UTF_8);
        // Un directorio con el nombre del archivo temporal impide escribirlo: simula un fallo de E/S.
        Files.createDirectory(carpeta.resolve("turnos.txt.tmp"));

        assertThrows(PersistenciaException.class, () -> repositorio.guardar(List.of(new Turno(9, FECHA))));

        assertEquals(antes, Files.readString(ruta, StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @DisplayName("RD-03: un archivo dañado se informa y no se modifica")
    @ValueSource(strings = {
            "contenido que no es de SGTU",
            "SGTU;1\n1;2026-11-11T10:00:00;EN_ESPERA",
            "SGTU;1\n1;2026-11-11T10:00:00;EN_ESPERA;;extra",
            "SGTU;1\nuno;2026-11-11T10:00:00;EN_ESPERA;",
            "SGTU;1\n1;ayer;EN_ESPERA;",
            "SGTU;1\n1;2026-11-11T10:00:00;INVENTADO;",
            "SGTU;1\n1;2026-11-11T10:00:00;EN_ATENCION;",
            "SGTU;1\n1;2026-11-11T10:00:00;EN_ATENCION;V07",
            "SGTU;1\n1;2026-11-11T10:00:00;EN_ESPERA;V01",
            "SGTU;1\n0;2026-11-11T10:00:00;EN_ESPERA;",
            "SGTU;1\n\n"})
    void archivoDanadoSeInformaYNoSeModifica(String contenido) throws IOException {
        Path ruta = carpeta.resolve("turnos.txt");
        Files.writeString(ruta, contenido, StandardCharsets.UTF_8);
        RepositorioArchivo repositorio = new RepositorioArchivo(ruta);

        PersistenciaException error = assertThrows(PersistenciaException.class, repositorio::cargar);

        assertTrue(error.getMessage().contains("No se modificó"));
        assertEquals(contenido, Files.readString(ruta, StandardCharsets.UTF_8));
        assertFalse(Files.exists(carpeta.resolve("turnos.txt.tmp")));
    }

    @Test
    @DisplayName("Un archivo vacío (sin encabezado) se considera dañado")
    void archivoVacioEsDanado() throws IOException {
        Path ruta = carpeta.resolve("turnos.txt");
        Files.writeString(ruta, "", StandardCharsets.UTF_8);

        assertThrows(PersistenciaException.class, () -> new RepositorioArchivo(ruta).cargar());
    }

    @Test
    @DisplayName("El mensaje de línea dañada indica el número de línea")
    void mensajeIndicaLinea() throws IOException {
        Path ruta = carpeta.resolve("turnos.txt");
        Files.writeString(ruta, "SGTU;1\n1;2026-11-11T10:00:00;EN_ESPERA;\n2;mala;EN_ESPERA;\n",
                StandardCharsets.UTF_8);

        PersistenciaException error = assertThrows(PersistenciaException.class,
                () -> new RepositorioArchivo(ruta).cargar());

        assertTrue(error.getMessage().contains("línea 3"));
    }
}
