# Revisión final de calidad (RNF-M05)

Revisó: Pedro Tenorio · Fecha: 2026-11-26
Alcance: `src/main` (13 archivos, 938 líneas antes de las correcciones) y `src/test` (7 archivos), más los documentos de `docs/` y `README.md`.

Este documento tiene dos partes: los hallazgos de la revisión (sobre el commit revisado) y su **estado** después de las correcciones. Las correcciones se hicieron en un PR aparte, sin cambiar ninguna regla de negocio, y se comprobaron con las pruebas (110 en total: 107 anteriores y 3 nuevas).

## Método
Lectura completa de cada archivo de `src/main`; búsquedas con `grep` y `awk` (anexo); compilación con `javac -Xlint:all`; contraste de los documentos con el código real; y prueba de la interfaz en pantalla virtual para confirmar los hallazgos de usabilidad antes y después de corregirlos.

## Qué se observó bien
- El dominio no depende de la interfaz ni de archivos; las reglas se validan dentro de `Turno` y `GestorTurnos`, aunque se invoque sin pasar por la pantalla (RNF-M01, RNF-S01).
- Las transiciones de estado son visibles solo dentro del paquete y las consultas devuelven copias.
- El orden FIFO depende de la secuencia y no del reloj ni del texto del ID (RN-02).
- `ServicioTurnos` guarda antes de confirmar y restaura el estado si el guardado falla (RD-02); una operación rechazada no intenta guardar (RNF-R02).
- `RepositorioArchivo` escribe en un temporal y lo mueve sobre el definitivo; un archivo dañado se informa y no se modifica (RD-02, RD-03).
- Búsquedas en `src/main`: sin `System.out`/`System.err`, sin `printStackTrace`, sin `TODO`/`FIXME`, sin operadores ternarios; en `src`: sin contraseñas, tokens ni código de red (RNF-S02, RNF-S03).
- Mensajes de error que nombran la causa y el ID o la ventanilla (RNF-U02).
- Estado de ventanilla derivado del turno En atención: no hay un segundo dato que pueda contradecirse.

## Hallazgos y su estado
Estado: **Corregido** (con el cambio hecho), **Parcial**, **Se mantiene** (decisión justificada) o **Pendiente** (ya planificado en otra tarea).

### Código
| # | Hallazgo | Dónde (commit revisado) | Prio. | Estado |
|---|---|---|---|---|
| 1 | Todo error al interpretar una línea que no fuera número o fecha se informaba como «Estado de turno no permitido» (el `catch (IllegalArgumentException)` también atrapaba la secuencia menor a 1 y la combinación incoherente de estado y ventanilla) y no se encadenaba la causa | `RepositorioArchivo.interpretarLinea`, líneas 112-113 y 117 | Media | **Corregido**: la lectura se separó en `leerSecuencia`, `leerFecha`, `leerEstado` y la restauración informa su propio mensaje, con la causa encadenada. 3 pruebas nuevas; 2 de ellas fallan con el código anterior |
| 2 | Ventanillas como `String` («V01»): la validación está repartida | `Ventanillas.existe`, `GestorTurnos.validarVentanilla`, `Turno.restaurar` | Media | **Se mantiene**: un `enum` cambia la API pública que usan la interfaz y casi todas las pruebas; el riesgo no compensa a pocos días de la entrega. |
| 3 | `VentanaPrincipal` mezcla construcción de pantalla, acciones y renderizado | `ui/VentanaPrincipal.java` (227 líneas) | Media | **Parcial**: el renderizador pasó a su propia clase `RenderizadorEstadoTurno` y la tabla del historial se arma en un método aparte; la clase sigue en unas 230 líneas |
| 4 | `registrarTurno` atrapaba `OperacionInvalidaException` para lanzar `IllegalStateException`, que además no capturaba la interfaz | `ServicioTurnos`, líneas 47-54 | Media | **Corregido**: `registrarTurno` ya no pasa por la operación que declara la excepción; guarda con `guardarYConfirmar` |
| 5 | Índices de columna fijos (0, 1, 3) junto a una constante | `VentanaPrincipal.crearPanelTabla`, líneas 101-104 | Baja | **Corregido**: constantes `COLUMNA_ID`, `COLUMNA_REGISTRO`, `COLUMNA_ESTADO`, `COLUMNA_VENTANILLA` |
| 6 | Se pedía confirmación de cancelación aun con el campo de ID vacío | `VentanaPrincipal.cancelarTurno` | Baja | **Corregido**: con el campo vacío no hay confirmación y se muestra el rechazo del dominio. Comprobado en pantalla virtual |
| 7 | El campo de ID se borraba tras cualquier operación correcta (p. ej. «Asignar siguiente») | `VentanaPrincipal.ejecutar`, línea 160 | Baja | **Corregido**: `ejecutar` devuelve si la operación se aplicó y solo la cancelación limpia el campo. Comprobado en pantalla virtual |
| 8 | Un rechazo se muestra dos veces: barra de mensajes y cuadro modal | `VentanaPrincipal.ejecutar` | Baja | **Se mantiene**: es una redundancia deliberada (el cuadro asegura que el operador lo vea) y quitarla obligaría a rehacer las capturas del recorrido manual y del manual de usuario |
| 9 | `colorDe` devolvía el color de «Cancelado» para cualquier etiqueta desconocida | `VentanaPrincipal`, líneas 215-226 | Baja | **Corregido**: tabla de colores por estado; una etiqueta desconocida conserva el color normal |
| 10 | Normalización repetida y `toUpperCase()` dependiente de la configuración regional | `GestorTurnos.validarVentanilla` / `validarIdTurno` | Baja | **Corregido**: método `normalizar` con `Locale.ROOT` |
| 11 | Tres búsquedas lineales casi iguales | `GestorTurnos`, líneas 190-217 | Baja | **Corregido**: una búsqueda `buscarPrimero(Predicate)` |
| 12 | `calcularSiguienteSecuencia` recorría toda la lista aunque está ordenada | `GestorTurnos`, líneas 139-147 | Baja | **Corregido**: último + 1 (la lista siempre está ordenada) |
| 13 | El respaldo usaba `consultarHistorial()`; el nombre no dice «instantánea» | `ServicioTurnos`, línea 91 | Baja | **Corregido**: método `tomarInstantanea()` |
| 14 | Mensajes con `String.format` posicional, con argumentos que no se usaban | `ServicioTurnos` | Baja | **Corregido**: cada operación arma su mensaje con una función; los textos son idénticos |
| 30 | `PersistenciaException` era una excepción no comprobada (`RuntimeException`) mientras `OperacionInvalidaException` es comprobada; las cláusulas `throws` eran decorativas y un fallo de almacenamiento podía pasar sin tratarse | `persistencia/PersistenciaException.java` | Media | **Corregido**: ahora es comprobada |
| 31 | `javac -Xlint:all` avisaba de `this-escape` en `GestorTurnos` y `VentanaPrincipal` (constructor que llama a métodos sobrescribibles) | `GestorTurnos` línea 34; `VentanaPrincipal` línea 54 | Baja | **Corregido**: ambas clases son `final`; compilación sin avisos |

### Estilo, formato y comentarios
| # | Hallazgo | Dónde | Prio. | Estado |
|---|---|---|---|---|
| 15 | 19 líneas de más de 120 caracteres (varias, más de 150) | `GestorTurnos` (9), `Turno` (6), `Main` (2), `VentanaPrincipal` (1), `ServicioTurnos` (1) | Baja | **Corregido**: ninguna línea supera 120 caracteres |
| 16 | Ningún comentario Javadoc; los bloques `/* */` y los `@param` no los procesa `javadoc` | `src/main` | Baja | **Corregido**: comentarios de clase y de los métodos principales convertidos a Javadoc |
| 17 | Comentarios inconsistentes: `//Texto` sin espacio, errores de ortografía, un comentario incompleto y otro que no correspondía al método | `Turno` línea 17; `RepositorioArchivo` líneas 60-61, 91-93, 139 | Baja | **Corregido** |
| 18 | Comentario de `ResultadoOperacion` la llamaba «Clase de utilidad» y usaba «parametro 1/2» | `ResultadoOperacion`, líneas 5-10 | Baja | **Corregido** |
| 19 | Importaciones fuera de orden | `ServicioTurnos`, `IRepositorioTurnos` | Baja | **Corregido** |

### Pruebas
| # | Hallazgo | Dónde | Prio. | Estado |
|---|---|---|---|---|
| 20 | `ServicioTurnoTest` (singular) no seguía el patrón `<Clase>Test` | `src/test/java/sgtu/aplicacion/` | Baja | **Corregido**: renombrada a `ServicioTurnosTest` |
| 21 | No hay prueba automática de la interfaz Swing | `src/test` | Ninguna | **Se mantiene**: aceptado por alcance; se cubre con el recorrido manual |

### Documentación y evidencia
| # | Hallazgo | Dónde | Prio. | Estado |
|---|---|---|---|---|
| 22 | Matriz y `PRUEBAS.md` citaban `ServicioTurnosTest` y la clase se llamaba `ServicioTurnoTest` | `docs/MATRIZ_CUMPLIMIENTO.md`, `docs/PRUEBAS.md` | Media | **Corregido** al renombrar la clase (hallazgo 20) |
| 23 | Marcadores sin completar: «Maven <versión>» y «commit <hash de desarrollo>» | `docs/PRUEBAS.md`, `docs/evidencia/recorrido-manual.md` | Media | **Corregido** |
| 24 | Se remitía a un reporte Surefire HTML que no está en `docs/evidencia/` | `docs/PRUEBAS.md`, matriz (RNF-M03) | Media | **Corregido**: se explica que el reporte no se versiona (`target/` está en `.gitignore`), con el comando para generarlo; la evidencia es `registro-integracion.md` |
| 25 | Las capturas están en `docs/capturas/` y la matriz no daba la ruta | `docs/MATRIZ_CUMPLIMIENTO.md` | Baja | **Corregido** |
| 26 | `registro-integracion.md`: las filas de PR5 y PR6 tenían «OK» en la columna equivocada y faltan integraciones posteriores | `docs/evidencia/registro-integracion.md` | Baja | **Corregido**: columnas alineadas y agregadas las filas de las integraciones posteriores (T-13 a T-16 y este PR de correcciones) |
| 27 | La observación «pide confirmación de registro…» no corresponde al código: solo la cancelación pide confirmación | `docs/evidencia/recorrido-manual.md` | Baja | **Corregido**: se retiró y se dejó la observación real, indicando que ya se corrigió |
| 28 | El recorrido manual (Windows 11) y la medición de rendimiento (Ubuntu 24.04) se hicieron con distinto sistema operativo en el mismo portátil | `recorrido-manual.md`, `rendimiento.md` | Baja | **Corregido** |
| 29 | `README.md` sigue siendo el texto base | `README.md` | Alta si no se completa | **Pendiente**: lo resuelve T-18 |

## Decisiones de diseño que se mantienen (no son defectos)
- Se reescribe todo el archivo en cada operación: aceptable para el alcance (cientos de turnos).
- `T0001` se acepta como `T001` (se interpreta como número); no contradice la ERS y evita rechazar una entrada razonable.
- Un archivo con una línea en blanco de más se considera dañado: estricto, pero a favor de no sobrescribir datos.
- `RepositorioEnMemoria` hecho a mano en lugar de Mockito, y Swing en lugar de JavaFX: cambios justificados respecto al entorno de ADA-08.

## Anexo: comandos usados
```
wc -l src/main/java/sgtu/*/*.java src/main/java/sgtu/Main.java
grep -rnE "System\.(out|err)|printStackTrace|TODO|FIXME" src/main        # sin resultados
grep -rnE " \? .* : " src/main | grep -vE ":[0-9]+:\s*(\*|//|/\*)"        # sin resultados
grep -rniE "password|passwd|token|apikey|secret|java\.net|http://|https://" src   # sin resultados
awk 'length>120 {print FILENAME":"FNR" ("length")"}' $(find src/main -name '*.java')   # antes: 19 líneas; después: ninguna
grep -c "/\*\*" $(find src/main -name '*.java')                           # antes: 0 en todos
javac -Xlint:all -d out $(find src/main -name '*.java')                   # antes: 3 avisos; después: ninguno
```
