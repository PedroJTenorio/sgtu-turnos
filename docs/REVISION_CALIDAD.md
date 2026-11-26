# Revisión final de calidad (RNF-M05)

Revisó: Pedro Tenorio · Fecha: 2026-11-26 · Tareas Revisadas: T-1 a T-15 integradas
Alcance: `src/main` (13 archivos, 938 líneas) y `src/test` (7 archivos), más los documentos de `docs/` y `README.md`.
**No se aplicó ningún cambio de código ni de otros documentos.** Los puntos de abajo son oportunidades; cada una se corregiría en su propia tarea y PR.

## Método
Lectura completa de cada archivo de `src/main`, búsquedas con `grep` y `awk` (comandos en el anexo), y contraste de los documentos con el código real.

## Qué se observó bien
- El dominio no depende de la interfaz ni de archivos; las reglas se validan dentro de `Turno` y `GestorTurnos`, aunque se invoque sin pasar por la pantalla (RNF-M01, RNF-S01).
- Las transiciones de estado (`iniciarAtencion`, `finalizar`, `cancelar`) son visibles solo dentro del paquete y las consultas devuelven copias.
- El orden FIFO depende de la secuencia y no del reloj ni del texto del ID (RN-02).
- `ServicioTurnos` guarda antes de confirmar y restaura el estado si el guardado falla (RD-02); una operación rechazada no intenta guardar (RNF-R02).
- `RepositorioArchivo` escribe en un temporal y lo mueve sobre el definitivo; un archivo dañado se informa y no se modifica (RD-02, RD-03).
- Búsquedas en `src/main`: sin `System.out`/`System.err`, sin `printStackTrace`, sin `TODO`/`FIXME`, sin operadores ternarios; en `src`: sin contraseñas, tokens ni código de red (RNF-S02, RNF-S03).
- Mensajes de error que nombran la causa y el ID o la ventanilla (RNF-U02).
- Estado de ventanilla derivado del turno En atención: no hay un segundo dato que pueda contradecirse.

## Oportunidades de mejora en el código
| # | Hallazgo | Dónde | Mejora posible | Prioridad |
|---|---|---|---|---|
| 1 | Todo error al interpretar una línea que no sea número o fecha se informa como «Estado de turno no permitido». El `catch (IllegalArgumentException)` también atrapa la secuencia menor a 1 y la combinación incoherente de estado y ventanilla de `Turno.restaurar`, así que el mensaje al usuario es engañoso (el dañado de RD-03 se informa bien, pero con la causa equivocada). Además `lineaInvalida` no encadena la causa original | `RepositorioArchivo.interpretarLinea`, líneas 112-113; `lineaInvalida`, línea 117 | Separar la conversión del estado de la restauración, o usar el mensaje de la excepción en lugar de un texto fijo | Media |
| 2 | Ventanillas como `String` («V01»): la validación está repartida en tres lugares | `Ventanillas.existe` (línea 17), `GestorTurnos.validarVentanilla` (línea 163), `Turno.restaurar` (línea 33) | Un `enum` o un tipo propio que valide una sola vez | Media |
| 3 | `VentanaPrincipal` (227 líneas) mezcla construcción de pantalla, acciones y renderizado; es la clase más grande del proyecto | `ui/VentanaPrincipal.java` | Extraer el panel de ventanillas y el renderizador a clases propias | Media |
| 4 | `registrarTurno` atrapa `OperacionInvalidaException` y lanza `IllegalStateException` solo porque la interfaz funcional declara esa excepción. Esa `IllegalStateException` no la captura `VentanaPrincipal.ejecutar` | `ServicioTurnos`, líneas 47-54; `VentanaPrincipal.ejecutar`, líneas 156-169 | Parametrizar la excepción de la interfaz funcional o separar la operación que no puede fallar | Media |
| 5 | Los índices de columna están fijos (0, 1 y 3) junto a la constante `COLUMNA_ESTADO_HISTORIAL`, y dependen del orden de `COLUMNAS_HISTORIAL`. Si cambia el orden de las columnas, los anchos quedan en la columna equivocada | `VentanaPrincipal.crearPanelTabla`, líneas 101-104 | Constantes con nombre para cada columna | Baja |
| 6 | Se pide confirmación de cancelación aun con el campo de ID vacío (también lo anota el recorrido manual) | `VentanaPrincipal.cancelarTurno`, líneas 143-153 | Validar que haya texto antes de abrir el diálogo | Baja (usabilidad) |
| 7 | `ejecutar` borra el campo «ID del turno a cancelar» tras **cualquier** operación correcta, no solo tras cancelar: si el operador escribió un ID y pulsa «Asignar siguiente», se pierde lo escrito | `VentanaPrincipal.ejecutar`, línea 160 | Limpiar el campo solo en la cancelación | Baja (usabilidad) |
| 8 | Un rechazo se muestra dos veces: en la barra de mensajes y en un cuadro modal que obliga a un clic extra | `VentanaPrincipal.ejecutar`, líneas 162-163 y 165-166 | Elegir uno (por ejemplo, solo la barra para rechazos de negocio) | Baja (usabilidad) |
| 9 | `RenderizadorEstado.colorDe` devuelve el color de «Cancelado» para cualquier etiqueta que no reconozca: un estado nuevo se vería como cancelado sin avisar | `VentanaPrincipal`, líneas 215-226 | Un mapa estado → color o un `switch` exhaustivo sobre `EstadoTurno` | Baja |
| 10 | La normalización (`trim` + `toUpperCase`) se repite en dos validadores, y `toUpperCase()` usa la configuración regional del equipo | `GestorTurnos.validarVentanilla` y `validarIdTurno`, líneas 167 y 179 | Método auxiliar común con `Locale.ROOT` | Baja |
| 11 | Tres búsquedas lineales casi iguales | `GestorTurnos`, líneas 190-217 | Una búsqueda genérica con predicado; irrelevante en rendimiento con 200 turnos | Baja |
| 12 | `calcularSiguienteSecuencia` recorre toda la lista aunque está ordenada por secuencia | `GestorTurnos`, líneas 139-147 | Tomar el último elemento (la lista está ordenada) | Baja |
| 13 | El respaldo previo a cada operación usa `consultarHistorial()`; el nombre no expresa «instantánea» | `ServicioTurnos.ejecutarYGuardar`, línea 91 | Método con nombre propio | Baja |
| 14 | Los mensajes de confirmación usan `String.format` con plantillas posicionales (`%1$s`, `%2$s`); en cancelar y registrar el segundo argumento (ventanilla) no se usa | `ServicioTurnos`, líneas 49, 59, 65, 70, 99 | Mensajes construidos en la presentación o en un objeto de mensajes | Baja |

## Estilo, formato y comentarios
| # | Hallazgo | Dónde | Mejora posible | Prioridad |
|---|---|---|---|---|
| 15 | 19 líneas superan 120 caracteres (varias, más de 150: `GestorTurnos` 52, 169; `Turno` 34, 79, 87, 94) | `GestorTurnos` (9), `Turno` (6), `Main` (2), `VentanaPrincipal` (1), `ServicioTurnos` (1) | Partir las líneas; fijar el límite en el estándar de codificación del README | Baja |
| 16 | No hay ningún comentario Javadoc (`/** … */`); la documentación es de bloque `/* … */` o de línea `//`. El bloque `@param` de `EstadoVentanilla` y `@throws` de `GestorTurnos.reemplazarTurnos` no lo procesa la herramienta `javadoc` | Todos los archivos de `src/main` | Convertir los comentarios de clase y métodos públicos a Javadoc | Baja |
| 17 | Estilo de comentarios inconsistente: `//Texto` sin espacio, textos con errores de ortografía («tempoeral», «vacios», «asociado»), un comentario incompleto («…para distinguir») y un comentario que no corresponde al método (`borrarTemporal` dice «Tras mover el contenido…» pero se llama cuando el guardado falla) | `Turno` línea 17; `RepositorioArchivo` líneas 60-61, 91-93, 139 | Unificar estilo y corregir textos | Baja |
| 18 | El comentario de `ResultadoOperacion` la llama «Clase de utilidad» (es un `record` con datos) y enumera «parametro 1/2» fuera de formato | `ResultadoOperacion`, líneas 5-10 | Corregir el comentario | Baja |
| 19 | Importaciones fuera de orden y bloques en blanco irregulares | `ServicioTurnos` (líneas 9-10), `IRepositorioTurnos` (líneas 3-7: la importación de `sgtu` va antes que la de `java.util`) | Ordenar las importaciones | Baja |

## Pruebas
| # | Hallazgo | Dónde | Mejora posible | Prioridad |
|---|---|---|---|---|
| 20 | El archivo y la clase se llaman `ServicioTurnoTest` (singular); el resto sigue el patrón `<Clase>Test` y la clase probada es `ServicioTurnos` | `src/test/java/sgtu/aplicacion/ServicioTurnoTest.java` | Renombrar a `ServicioTurnosTest` (y confirmar que Surefire lo sigue ejecutando) | Baja |
| 21 | No hay prueba automática de la interfaz Swing; los requisitos de pantalla (RI-01 a RI-03, RNF-U01 a RNF-U04) dependen del recorrido manual | `src/test` | Aceptado por alcance; se declara en la matriz | Ninguna |

## Documentación y evidencia
La corrección corresponde a la tarea de su autor, no a este PR.
| # | Hallazgo | Dónde | Mejora posible | Prioridad |
|---|---|---|---|---|
| 22 | La matriz y `PRUEBAS.md` citan `ServicioTurnosTest`, pero la clase real es `ServicioTurnoTest` (hallazgo 20); una búsqueda de la clase citada no la encuentra | `docs/MATRIZ_CUMPLIMIENTO.md` (línea 3), `docs/PRUEBAS.md` (tabla) | Corregir el nombre en los documentos o renombrar la clase | Media |
| 23 | Marcadores sin completar: `PRUEBAS.md` dice «Maven <versión>» y no incluye la línea de «última ejecución» con fecha y commit; `recorrido-manual.md` conserva «commit <hash de desarrollo>» | `docs/PRUEBAS.md` línea 1; `docs/evidencia/recorrido-manual.md` línea 3 | Sustituir por los valores reales | Media |
| 24 | La matriz y `PRUEBAS.md` remiten a un reporte Surefire HTML en `docs/evidencia/` que no está en esa carpeta (solo hay `registro-integracion.md`, `recorrido-manual.md` y `rendimiento.md`) | `docs/PRUEBAS.md` línea 1; `docs/MATRIZ_CUMPLIMIENTO.md` (RNF-M03) | Adjuntar el reporte o la salida de `mvn test` de T-12, o retirar la referencia | Media |
| 25 | Las capturas están en `docs/capturas/`, mientras la matriz habla de «capturas `interfaz-NN-*.png`» sin ruta y la planificación indicaba `docs/evidencia/` | `docs/MATRIZ_CUMPLIMIENTO.md` línea 5 | Indicar la ruta real | Baja |
| 26 | `registro-integracion.md`: las dos últimas filas desplazan «OK» a la columna «Observaciones». | `docs/evidencia/registro-integracion.md` | Alinear columnas, usar la numeración vigente y completar | Baja |
| 27 | Observación de usabilidad dudosa: «Pide confirmación de registro aún con el campo de ID erróneo»; en el código solo la cancelación pide confirmación | `docs/evidencia/recorrido-manual.md`, sección «Observaciones de Usabilidad» | Verificar y corregir el texto (probablemente se refería a cancelar) | Baja |
| 28 | `README.md` sigue siendo el texto base: no explica cómo ejecutar ni probar (RNF-M04); se resuelve en T-18 | `README.md` | Completarlo | Alta |

## Decisiones de diseño que se mantienen (no son defectos)
- Se reescribe todo el archivo en cada operación: aceptable para el alcance (cientos de turnos).
- `T0001` se acepta como `T001` (se interpreta como número); no contradice la ERS y evita rechazar una entrada razonable.
- Un archivo con una línea en blanco de más se considera dañado: estricto, pero sirve para no sobrescribir datos.
- `RepositorioEnMemoria` hecho a mano en lugar de Mockito, y Swing en lugar de JavaFX.

## Límites de esta revisión
Revisión manual hecha por una sola persona, sin medición de cobertura y sin ejecución de la interfaz en esta revisión (se apoya en el recorrido de T-14). Las líneas indicadas corresponden al commit revisado.

## Anexo: comandos usados
```
wc -l src/main/java/sgtu/*/*.java src/main/java/sgtu/Main.java
grep -rnE "System\.(out|err)|printStackTrace|TODO|FIXME" src/main        # sin resultados
grep -rnE " \? .* : " src/main | grep -vE ":[0-9]+:\s*(\*|//|/\*)"        # sin resultados
grep -rniE "password|passwd|token|apikey|secret|java\.net|http://|https://" src   # sin resultados
awk 'length>120 {print FILENAME":"FNR" ("length")"}' $(find src/main -name '*.java')   # 19 líneas
grep -c "/\*\*" $(find src/main -name '*.java')                           # 0 en todos
grep -rn "RepositorioTurnos" --include=*.md --include=*.java . | grep -v "IRepositorioTurnos"   # sin resultados
ls src/test/java/sgtu/aplicacion/                                         # ServicioTurnoTest.java
```