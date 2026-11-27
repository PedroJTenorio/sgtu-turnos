# Matriz de cumplimiento (ERS-SGTU-001 v1.0)

Claves de pruebas: **GT** = `GestorTurnosTest`, **TT** = `TurnoTest`, **ST** = `ServicioTurnosTest`,
**RA** = `RepositorioArchivoTest`, **RT** = `RendimientoTest`.
**Manual** = `docs/evidencia/recorrido-manual.md` (escenarios 1 a 15, capturas en `docs/capturas/interfaz-NN-*.png`).
Última ejecución registrada de `mvn test` antes de las correcciones de calidad: 107 pruebas, 0 fallos (25/11/2026, commit 18a0d41).
Tras las correcciones de calidad (`docs/REVISION_CALIDAD.md`) el total es de **110 pruebas** (3 nuevas en `RepositorioArchivoTest`); el resultado de cada integración queda en `docs/evidencia/registro-integracion.md`.

| ID | Componente | Evidencia |
|---|---|---|
| RF-01 | `GestorTurnos.consultarVentanillas`, `VentanaPrincipal` | GT `ConsultaVentanillas`; Manual 1 y 15 |
| RF-02, RN-01, RN-02 | `GestorTurnos.registrarTurno`, `Turno` | GT `Registro` (orden, IDs únicos, reloj detenido/retrocede, T1000 después de T999); TT |
| RF-03 | `GestorTurnos.consultarFila` | GT `Consultas.filaSoloPendientesEnOrden`; Manual 3 |
| RF-04, RN-06 | `GestorTurnos.asignarSiguiente` | GT `Asignacion` (CV-01, V03 y luego V01, asignar inicia atención); Manual 1 |
| RF-05, RN-04, RN-05, RN-10 | `GestorTurnos.asignarSiguiente` | GT `cv02…`, `cv03…`, `filaVaciaSeRechaza`, `turnoEnUnaSolaVentanilla`; Manual 2, 3, 9 |
| RF-06, RF-07, RN-08 | `GestorTurnos.finalizarAtencion`, `Turno.finalizar` | GT `Finalizacion` (CV-04); TT; Manual 4 y 8 |
| RF-08, RF-09, RN-07 | `GestorTurnos.cancelarTurno`, `Turno.cancelar` | GT `Cancelacion` (CV-05, CV-06, inexistente, doble cancelación); Manual 5, 6, 7, 10 |
| RF-10, RN-09 | `GestorTurnos.consultarHistorial` | GT `historialConLosCuatroEstados`, `terminalesNoVuelvenALaFila` (CV-10); Manual 11 |
| RF-11, RNF-R01, RD-01 | `RepositorioArchivo`, `IRepositorioTurnos`, `ServicioTurnos` | RA `guardarYCargarConservaTodo`; ST CV-07, CV-08, `reinicioConArchivoReal`; Manual 12 |
| RF-12, RD-02 | `ServicioTurnos` (aplicar, guardar, confirmar o deshacer) | ST `falloAl…RestauraEstado` (CV-09), `rechazoNoGuarda`; RA `fallaDeGuardadoNoDanaElArchivoAnterior`, `noQuedaArchivoTemporal` |
| RD-03 | `RepositorioArchivo.cargar`, `Main` | RA `archivoDanadoSeInformaYNoSeModifica`, `archivoVacioEsDanado`, `mensajeIndicaLinea`, `estadoDesconocidoSeInforma`, `secuenciaInvalidaIndicaSuCausa`, `estadoIncoherenteIndicaSuCausa`; ST `lecturaFallidaNoEscribe`, `datosInvalidosNoSeSobrescriben`; Manual 14 |
| RN-03, RC-03 | `Ventanillas`, `GestorTurnos` | GT `ventanillaInvalidaSeRechaza` (asignar y finalizar), `ventanillaSeNormaliza` |
| RI-01, RI-02, RI-03 | `VentanaPrincipal` | Manual 1 a 15; Manual 15 (controles y etiquetas) |
| RNF-U01 | `VentanaPrincipal` | Manual 15 |
| RNF-U02 | Mensajes de `GestorTurnos` y `ServicioTurnos` | ST `rechazoExplicaLaCausa`; Manual 2, 6, 8, 10 |
| RNF-U03 | `VentanaPrincipal.RenderizadorEstado` | Manual 11 (texto del estado + color) |
| RNF-U04 | `VentanaPrincipal.cancelarTurno` | Manual 5 y 7 (diálogo de confirmación) |
| RNF-S01 | Dominio (`Turno`, `GestorTurnos`) | TT y GT invocan el dominio con transiciones inválidas |
| RNF-S02 | Todo el repositorio | Búsqueda de claves o contraseñas en `src/` sin resultados (comando abajo) |
| RNF-S03 | Todo el repositorio | Sin código de red: búsqueda `java.net` / `http` en `src/` sin resultados |
| RNF-S04 | `GestorTurnos.validarIdTurno`, `validarVentanilla` | GT `idMalformadoSeRechaza`, `ventanillaInvalidaSeRechaza`; Manual 10 |
| RNF-M01 | Paquetes `dominio`, `persistencia`, `aplicacion`, `ui` | Las pruebas de `dominio` corren sin interfaz ni archivos; ST usa `RepositorioEnMemoria` |
| RNF-M02 | Todo | Estándar en `README.md`; `docs/REVISION_CALIDAD.md` (T-16) |
| RNF-M03 | `src/test` | 110 pruebas; `docs/PRUEBAS.md`; resultados de cada integración en `docs/evidencia/registro-integracion.md` |
| RNF-M04 | `README.md`, `pom.xml` | Instrucciones de preparar, ejecutar y probar (README final, T-18) |
| RNF-M05 | `docs/REVISION_CALIDAD.md` | Hallazgos con prioridad (T-16) |
| RNF-R02 | Dominio + servicio | GT y ST comparan el estado antes y después de un rechazo (`rechazoNoGuarda`, `cv02…`) |
| RNF-P01 | `RendimientoTest` | RT (límite 2 s con 200 turnos); mediciones en `docs/evidencia/rendimiento.md` |
| RC-01…RC-07 | Proyecto completo | Java 17 + Swing; una sola instancia; sin autenticación; sin refactor exigido |