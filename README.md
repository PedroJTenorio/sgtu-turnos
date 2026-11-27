# SGTU — Sistema de Gestión de Turnos Universitarios

ADA-09 · Construcción de Software · UADY · Equipo 5 · Prof. René Humberto Moreno Acevedo
Implementación de la ERS-SGTU-001 v1.0 en Java 17, Maven y JUnit 5.

**Integrantes:** Fabrizzio España, Francois Pageau, Pedro Tenorio y Diego Zapata.

## Qué hace
Gestiona una fila de turnos en orden de llegada (FIFO) atendida por tres ventanillas (V01, V02 y V03).
Un turno pasa por los estados **En espera → En atención → Atendido**, o se **cancela** mientras está En espera.
Mantiene un historial de todos los turnos y conserva los datos en un archivo local entre ejecuciones.

## Requisitos
- Java 17 o superior (`java -version`)
- Maven 3.8 o superior (`mvn -v`); el proyecto se probó con Maven 3.9.9
- Git

## Obtener el código
```
git clone https://github.com/PedroJTenorio/sgtu-turnos.git
cd sgtu-turnos
```
La rama `desarrollo` es la de trabajo; `main` contiene la versión entregada.

## Compilar y ejecutar
```
mvn -DskipTests package
java -jar target/sgtu.jar
```
Ejecute siempre desde la carpeta raíz del proyecto: por omisión los datos se guardan en `datos/turnos.txt`
(Git no los sube). Para usar otro archivo: `java -jar target/sgtu.jar ruta/otro.txt`.
Si el archivo de datos está dañado, el programa avisa, no arranca y no lo modifica.
Guía de uso con capturas: [`docs/MANUAL_USUARIO.md`](docs/MANUAL_USUARIO.md).

## Pruebas
```
mvn test
```
Resultado esperado: `Tests run: 110, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS`.

Reporte HTML de Surefire:
```
mvn -B clean test org.apache.maven.plugins:maven-surefire-report-plugin:3.2.5:report
```
El reporte queda en `target/site/surefire-report.html` (no se sube a Git porque `target/` está ignorado).
Qué prueba cada clase y los casos CV-01 a CV-10 de la ERS: [`docs/PRUEBAS.md`](docs/PRUEBAS.md).

## Estructura del proyecto
| Paquete | Clases | Depende de |
|---|---|---|
| `sgtu.dominio` | `Turno`, `GestorTurnos`, `EstadoTurno`, `Ventanillas`, `EstadoVentanilla`, `OperacionInvalidaException`: reglas del sistema | nada |
| `sgtu.persistencia` | `IRepositorioTurnos`, `RepositorioArchivo`, `PersistenciaException`: guardado y lectura | dominio |
| `sgtu.aplicacion` | `ServicioTurnos`, `ResultadoOperacion`: aplica la operación, guarda y confirma o deshace | dominio, persistencia |
| `sgtu.ui` | `VentanaPrincipal`, `RenderizadorEstadoTurno` (Swing) | aplicación |
| `sgtu` | `Main`: arranque | todos |

Las pruebas están en `src/test/java` con la misma estructura de paquetes.
`RelojManual` y `RepositorioEnMemoria` son ayudas de prueba hechas a mano (sin Mockito).

## Decisiones de diseño
- **FIFO por número de secuencia**, no por la hora ni por el texto del ID: resiste relojes que retroceden y ordena bien T1000 después de T999 (RN-02).
- **Guardar antes de confirmar:** si el guardado falla, la operación se deshace y no se anuncia éxito (RF-12, RD-02).
- **Escritura atómica** con archivo temporal; un archivo dañado se informa sin sobrescribirlo (RD-02, RD-03).
- **Estado de ventanilla derivado:** una ventanilla está ocupada si existe un turno En atención asignado a ella; no hay un segundo dato que pueda contradecirse.
- **Dos tipos de error, ambos comprobados:** `OperacionInvalidaException` (regla de negocio) y `PersistenciaException` (almacenamiento).
- **Sin dependencias externas** salvo JUnit en pruebas.
- **Cambios respecto al entorno definido en ADA-08**, justificados: Swing en lugar de JavaFX; repositorio en memoria hecho a mano en lugar de Mockito; rama intermedia `desarrollo` como predeterminada.

## Estándar de codificación
- Java 17, paquete raíz `sgtu`, sangría de 4 espacios, llaves siempre, líneas de hasta 120 caracteres.
- Clases en `PascalCase`, métodos y variables en `camelCase`, constantes en `MAYUSCULAS_CON_GUION_BAJO`; interfaces con prefijo `I`.
- Nombres y mensajes en español.
- Comentarios Javadoc en clases y métodos públicos principales; comentarios de línea solo para aclarar una regla (con su ID de la ERS).
- Sin `System.out`, sin operadores ternarios, sin código muerto y sin secretos en el código.
- Las reglas de negocio viven en `sgtu.dominio`; la interfaz no decide reglas.
- Una excepción por tipo de error y mensajes que explican la causa e indican el ID o la ventanilla.

## Flujo de trabajo del equipo
Las tareas se llevan en ClickUp (responsable, revisor y criterio de aceptación). Cada tarea:
rama `features/<Nombre>` desde `desarrollo`, commits `CU-<clave> [In Progress|Complete] texto`,
Pull Request a `desarrollo` con la plantilla, `mvn test` en verde y revisión de otro integrante
(Pedro revisa a Francois, Francois a Fabrizzio, Fabrizzio a Diego y Diego a Pedro).
Se integra con *Create a merge commit*. `main` solo recibe el Pull Request final.

## Documentación
| Documento | Contenido |
|---|---|
| [`docs/MANUAL_USUARIO.md`](docs/MANUAL_USUARIO.md) | Manual de usuario con capturas |
| [`docs/MATRIZ_CUMPLIMIENTO.md`](docs/MATRIZ_CUMPLIMIENTO.md) | Requisitos de la ERS ligados a componente y evidencia |
| [`docs/PRUEBAS.md`](docs/PRUEBAS.md) | Pruebas y casos de verificación |
| [`docs/REVISION_CALIDAD.md`](docs/REVISION_CALIDAD.md) | Hallazgos de la revisión final y su estado |
| [`docs/evidencia/`](docs/evidencia) | Recorrido manual, medición de rendimiento y registro de integración |
| [`docs/capturas/`](docs/capturas) | Capturas del recorrido manual |
| [`docs/PROCESO_Y_CONTRIBUCIONES.md`](docs/PROCESO_Y_CONTRIBUCIONES.md) | Aportaciones, registro de uso de IA e indicadores |

## Limitaciones conocidas
- No hay prueba automática de la interfaz Swing; se cubre con el recorrido manual.
- Una sola instancia por archivo de datos; sin inicio de sesión ni varios usuarios.
- Los incumplimientos y hallazgos pendientes están en la matriz de cumplimiento y en la revisión de calidad.

## Uso de IA
Apoyo en la creación de secciones del código, creación de test, comentarios en javadoc y aporte en organización y estructura de la documentación se generó con Claude.
El registro por tarea (aporte y verificación) está en `docs/PROCESO_Y_CONTRIBUCIONES.md`.