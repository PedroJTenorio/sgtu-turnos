Resultado esperado de `mvn test`: **110 pruebas, 0 fallos, 0 omitidas**. El resultado de cada integración está en `docs/evidencia/registro-integracion.md`.
Entorno: Java 17, Maven 3.9.9, JUnit Jupiter 5.10.2.

Reporte Surefire HTML (no se guarda en el repositorio porque `target/` está en `.gitignore`; se genera con):
```
mvn -B clean test org.apache.maven.plugins:maven-surefire-report-plugin:3.2.5:report
```
Queda en `target/site/surefire-report.html`.

## Qué prueba cada clase
| Clase | Pruebas | Qué comprueba | Tipos de caso |
|---|---|---|---|
| `TurnoTest` | 11 | Estado inicial, ID derivado de la secuencia, transiciones válidas e ilegales, estados terminales, coherencia al restaurar, copias independientes | normal, inválido |
| `GestorTurnosTest` | 63 | FIFO (reloj detenido, que retrocede, IDs no alfabéticos), asignación a V01–V03, finalización, cancelación, consultas, CV-01 a CV-06 y CV-10, entradas nulas/vacías/malformadas | normal, inválido, límite |
| `ServicioTurnosTest` | 13 | Guardar y confirmar; fallo de guardado deshace y no anuncia éxito (CV-09); rechazos no guardan; reinicio (CV-07, CV-08); datos inválidos no se sobrescriben; archivo real | normal, inválido |
| `RepositorioArchivoTest` | 22 | Ida y vuelta con los cuatro estados, sin archivo, archivo dañado o vacío, escritura atómica sin temporales, fallo que no daña el archivo anterior | normal, inválido, límite |
| `RendimientoTest` | 1 | Registrar, consultar fila, asignar y finalizar con 200 turnos en menos de 2 s (RNF-P01) | límite |

## Casos de verificación de la ERS
| Caso | Dónde se verifica |
|---|---|
| CV-01 | GT `cv01AsignacionEnOrden`; Manual 1 |
| CV-02 | GT `cv02VentanillaOcupadaSeRechaza`; Manual 2 |
| CV-03 | GT `cv03NoHayCuartaAtencion`; Manual 3 |
| CV-04 | GT `cv04ReasignaTrasFinalizar`; Manual 4 |
| CV-05 | GT `cv05CancelaYOmite`; Manual 5 |
| CV-06 | GT `cv06NoSeCancelaEnAtencion`; Manual 6 |
| CV-07 | ST `cv07ReinicioRecuperaEstado`; Manual 12 |
| CV-08 | ST `cv08NoSeReutilizanIds`; Manual 13 |
| CV-09 | ST cuatro pruebas `falloAl…RestauraEstado` (solo automática) |
| CV-10 | GT `historialConLosCuatroEstados`; Manual 11 |