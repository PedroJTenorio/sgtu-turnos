# Proceso y contribuciones — SGTU (Sistema de Gestión de Turnos Universitarios)

ADA-09 · Equipo 5. Datos al 28 de noviembre del 2026, sobre `desarrollo` (110 pruebas, 0 fallos).

## 1. Flujo de trabajo (ADA-08) aplicado al SGTU
1. Tarea en ClickUp (responsable, revisor, fechas, criterio, requisitos ERS) antes de empezar.
   2. Rama `features/<Nombre>` desde `desarrollo`. Por lo general, se trata que el nombre de la rama de una explicación de su propósito.
3. Commits `CU-<clave> [In Progress] texto`.
4. IA solo con pregunta concreta y fragmentos; se anota tarea, aporte y verificación.
5. `mvn test` (y reporte Surefire cuando el PR lo pide) antes del PR.
6. PR a `desarrollo` con la plantilla; lo aprueba el revisor asignado.
7. Correcciones en la misma rama; al integrar se actualiza ClickUp.
8. README o manual se actualiza si cambia la ejecución o el uso.

Orden de revisión: Pedro revisa a Francois, Francois a Fabrizzio, Fabrizzio a Diego y Diego a Pedro.

## 2. Cambios al entorno de ADA-08 (justificados)
- TestFX no aplica: la interfaz es Swing, no JavaFX.
- Mockito no es necesario: el servicio se prueba con un repositorio en memoria hecho a mano (`RepositorioEnMemoria`).
- Rama intermedia `desarrollo` como predeterminada; `main` solo recibe el PR final.

## 3. Tareas y aportaciones por integrante

| Tarea  | Descripción | Responsable | Revisor   | Entregable                                                            |
|--------|---|---|---|-----------------------------------------------------------------------|
| T-1    | Repositorio, esqueleto Maven, ramas y plantilla de PR | Pedro | Diego | `pom.xml`, `.gitignore`, plantilla                                    |
| T-2    | ClickUp: tareas, campos, vistas, dependencias | Diego | Fabrizzio | tablero                                                               |
| T-3    | Tiempo de preparación del entorno | Todos | Diego | hoja de indicadores                                                   |
| T-4    | PR1 dominio base | Pedro | Diego | `Turno`, estados, `Ventanillas`, `TurnoTest`                          |
| T-5    | Boceto de interfaz en Figma | Fabrizzio | Francois | enlace en ClickUp                                                     |
| T-6    | PR2 interfaz del repositorio | Diego | Fabrizzio | `IRepositorioTurnos`, `PersistenciaException`, `RepositorioEnMemoria` |
| T-7    | PR3 `GestorTurnos` | Francois | Pedro | `GestorTurnos`, `RelojManual`, `GestorTurnosTest`                     |
| T-8    | PR4 `RepositorioArchivo` | Diego | Fabrizzio | `RepositorioArchivo`, `RepositorioArchivoTest`                        |
| T-9    | Estudio local de `VentanaPrincipal` y `Main` | Fabrizzio | Francois | notas en ClickUp                                                      |
| T-10   | PR5 `ServicioTurnos` | Diego | Fabrizzio | `ServicioTurnos`, `ResultadoOperacion`, `ServicioTurnosTest`          |
| T-11   | PR6 interfaz Swing y arranque | Fabrizzio | Francois | `VentanaPrincipal`, `Main`                                            |
| T-12   | Integración y reporte | Pedro | Diego | `registro-integracion.md`                                             |
| T-13   | PR7 rendimiento | Fabrizzio | Francois | `RendimientoTest`, `rendimiento.md`                                   |
| T-14   | Recorrido manual con capturas | Francois | Pedro | `recorrido-manual.md`, `docs/capturas/`                               |
| T-15   | Matriz de cumplimiento y pruebas | Francois | Pedro | `MATRIZ_CUMPLIMIENTO.md`, `PRUEBAS.md`                                |
| T-16   | Revisión final de calidad | Pedro | Diego | `REVISION_CALIDAD.md`                                                 |
| T-16.5 | Correcciones de la revisión de calidad | Pedro | Diego | código, pruebas y documentos                                          |
| T-17   | Manual de usuario | Fabrizzio | Francois | `MANUAL_USUARIO.md`                                                   |
| T-18   | README final | Pedro | Diego | `README.md`                                                           |
| T-19   | Registro de IA y contribuciones | Diego | Fabrizzio | `PROCESO_Y_CONTRIBUCION.md`                                           |
| T-20   | PR a `main` y etiqueta v1.0 | Pedro | Diego | GitHub                                                                |
| T-21   | Ensayo de defensa | Todos | Todos | —                                                                     |

### Resumen por integrante

| Integrante | Componentes                                                                                                                                                                                                                        | Commits (`CU-`) | PR propios integrados | PR revisados |
|---|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---|---|---|
| Pedro | Esqueleto Maven (`pom.xml`, `.gitignore`), plantilla de PR; dominio base (`Turno`, estados, `Ventanillas`, `TurnoTest`); `registro-integracion.md`; `REVISION_CALIDAD.md` y sus correcciones; `README.md`; PR a `main`             | 10 | 6 | 3 |
| Francois | `GestorTurnos`, `RelojManual`, `GestorTurnosTest`; `recorrido-manual.md` y capturas; `MATRIZ_CUMPLIMIENTO.md`, `PRUEBAS.md`                                                                                                        | 7  | 3 | 3 |
| Diego | Tablero de ClickUp; `IRepositorioTurnos`, `PersistenciaException`, `RepositorioEnMemoria`; `RepositorioArchivo` y su prueba; `ServicioTurnos`, `ResultadoOperacion`, `ServicioTurnosTest`; documento de `PROCESO_Y_CONTRIBUCIONES` | 4  | 3 | 6 |
| Fabrizzio | Boceto en Figma; estudio de `VentanaPrincipal` y `Main`; interfaz Swing (`VentanaPrincipal`, `Main`); `RendimientoTest` y `rendimiento.md`; `MANUAL_USUARIO.md`                                                                    | 6  | 3 | 3 |

Los PR son los 15 cerrados en GitHub (0 abiertos). «Revisados» cuenta los PR que cada integrante aprobó e integró (el merge lo hizo el revisor asignado).

### Pull requests integrados en `desarrollo`
| PR | Autor | Tarea | Integró (revisor) |
|---|---|---|---|
| #1 | Pedro | T-4 dominio base | Diego |
| #2 | Diego | T-6 interfaz del repositorio | Fabrizzio |
| #3 | Diego | T-8 `RepositorioArchivo` | Fabrizzio |
| #4 | Francois | T-7 `GestorTurnos` | Pedro (integración manual, sin commit de merge) |
| #5 | Diego | T-10 `ServicioTurnos` | Fabrizzio |
| #6 | Fabrizzio | T-11 interfaz Swing | Francois |
| #7 | Pedro | T-12 registro de integración | Diego |
| #8 | Fabrizzio | T-13 rendimiento | Francois |
| #9 | Francois | T-14 recorrido manual | Pedro |
| #10 | Francois | T-15 matriz y pruebas | Pedro |
| #11 | Pedro | T-16 revisión de calidad | Diego |
| #12 | Pedro | Revisión de calidad (misma rama que #11 y #13) | Diego |
| #13 | Pedro | T-16.5 correcciones de calidad | Diego |
| #14 | Fabrizzio | T-17 manual de usuario | Francois |
| #15 | Pedro | T-18 README final | Diego |

* Falta el último PR para la inclusión de este documento o algún otro para corregir defectos y ajustar detalles.

## 4. Uso de IA y validación
Responsabilidad: el equipo es responsable del código y de los documentos; la IA propone y cada integrante verifica.

| Tarea | Integrante | (Herramienta) Aporte de IA                                                                                                                                     | Verificación realizada                                                                                                                                                                                 | Resultado |
|-------|------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| T-4   | Pedro      | (Claude) Elaboración de `TurnoTest` para realizar pruebas sobre la primera funcionalidad de la aplicación                                                      | Se corrieron las pruebas y se analizó minuciosamente cada assert generado                                                                                                                              | Aceptado  |
| T-5   | Fabrizzio  | (Gemini) Idea para paleta de colores y diseño preliminar de la interfaz                                                                                        | Evaluación del boceto entregado por la herramienta. Se ajustaron colores, posicionamiento de botones y tipografía                                                                                      | Ajustado  |
| T-6   | Diego      | (Gemini) Revisión general de `RepositorioEnMemoria` usado en tests posteriores                                                                                 | Lectura de la clase y comprobación de concordancia con la interfaz `IRepositorioTurnos`                                                                                                                | Aceptado  |
| T-7   | Francois   | (Gemini) Construcción de la clase `GestorTurnosTest` con anotaciones para parámetros, `DisplayName` y `Nested`                                                 | Análisis de cada método propuesto por la herramienta; se corrió la clase de pruebas y se verificó que no hubiera errores en la salida                                                                  | Aceptado  |
| T-8   | Diego      | (Gemini) Ayuda con `interpretarLinea()`, `convertirALinea()` y la lógica de archivos temporales para el archivo definitivo                                     | Se creó una clase de prueba para corroborar la funcionalidad. Se encontraron errores de codificación al escribir el archivo temporal y se corrigió a UTF-8                                             | Ajustado  |
| T-10  | Diego      | (Gemini) Construcción de `ServicioTurnosTest` para evaluar en conjunto `RelojManual`, `ServicioTurnos` y `RepositorioEnMemoria`                                | Como parte de la revisión estándar, se corrieron las pruebas elaboradas, buscando que no existiesen errores y analizó cuidadosamente el código para ver que los tests fuesen coherentes                | Ajustado  |
| T-11  | Fabrizzio  | (Gemini) Esqueleto de la vista principal, agregación de elementos y estilos de Java Swing y corrección de pequeños bugs visuales al renderizar tras una acción | Al tratarse de un elemento visual, fue posible realizar pruebas sobre los botones, el panel y el comportamiento general de las vistas. Se ajustaron el tamaño de las vistas y el layout de los paneles | Ajustado  |
| T-13  | Fabrizzio  | (Gemini) Elaboración de la clase test `RendimientoTest` como parte del aseguramiento de calidad del programa                                                   | Además de la lectura minuciosa del código, se corrió el código resultante con mvn test y se corroboró que se cumplían los requerimientos. No se realizaron ajustes posteriores al código.              | Aceptado  |
| T-18  | Pedro      | (Claude) Uso menor de IA documentado. Ayuda con el formateo, escritura y presentación final del documento markdown principal `README.md`                       | Análisis de sugerencias de fragmentos de texto y formato. Se tómo como referencia la estructura del README dado por la herramienta, ajustando el estilo y la organización del documento                | Ajustado  |

* Además de los aportes registrados en este apartado. Cada PR posee un apartado en su descripción mencionando algún uso de IA.
* Generalmente el uso de IA se limitó a clases test, comandos en consola, formateo para documentos o revisiones al código creado.

## 5. Indicadores del plan de comprobación (ADA-08, sección 5)

### 5.1 Tiempo de preparación del entorno
| Integrante | Minutos (clonar → compilar y ejecutar) | Fuente |
|---|----------------------------------------|---|
| Pedro | 1 minuto <                             | Hoja de indicadores de T-3 |
| Francois | 1 minuto <                             | Hoja de indicadores de T-3 |
| Diego | 1 minuto <                             | Hoja de indicadores de T-3 |
| Fabrizzio | 1 minuto <                             | Hoja de indicadores de T-3 |

* El < es para denotar que el tiempo de preparación pudo ser menor. No se registró oficialmente

### 5.2 Tareas terminadas a tiempo
| Tarea | Responsable | Vence | Cerrada | Resultado |
|---|---|---|---|---|
| T-1 | Pedro | 17/11 | 17/11 | A tiempo |
| T-2 | Diego | 17/11 | 18/11 | Tarde (1 día) |
| T-3 | Todos | 18/11 | 18/11 | A tiempo |
| T-4 | Pedro | 18/11 | 18/11 | A tiempo |
| T-5 | Fabrizzio | 18/11 | 18/11 | A tiempo |
| T-6 | Diego | 18/11 | 18/11 | A tiempo |
| T-7 | Francois | 19/11 | 19/11 | A tiempo |
| T-8 | Diego | 19/11 | 19/11 | A tiempo |
| T-9 | Fabrizzio | 20/11 | 20/11 | A tiempo |
| T-10 | Diego | 20/11 | 20/11 | A tiempo |
| T-11 | Fabrizzio | 23/11 | 23/11 | A tiempo |
| T-12 | Pedro | 23/11 | 23/11 | A tiempo |
| T-13 | Fabrizzio | 24/11 | 24/11 | A tiempo |
| T-14 | Francois | 24/11 | 24/11 | A tiempo |
| T-15 | Francois | 25/11 | 25/11 | A tiempo |
| T-16 | Pedro | 25/11 | 26/11 | Tarde (1 día) |
| T-16.5 | Pedro | 26/11 | 26/11 | A tiempo |
| T-17 | Fabrizzio | 26/11 | 27/11 | Tarde (1 día) |
| T-18 | Pedro | 26/11 | 27/11 | Tarde (1 día) |
| **Total** | | | | **15 de 19 (78.9 %)** |


### 5.3 Tareas sin responsable o criterios
| Concepto | Cantidad | Detalle |
|---|---|---|
| Tareas T-1 a T-21 y T-16.5 sin responsable | 0 | Las 22 tienen responsable |
| Tareas sin revisor | 0 | Las 22 tienen el campo «Revisor» |
| Tareas sin criterio escrito | 1 | T-16.5 se creó después de la revisión de calidad y no tiene descripción |
| Tareas abiertas sin responsable | 3 | Entregable 1, Entregable 5 y Defensa (de trazabilidad, no de trabajo) |


### 5.4 Frecuencia de pruebas
| Fecha | Integración | Autor → revisor | Pruebas | Resultado |
|---|---|---|---|---|
| 18/11 | PR1 dominio base (T-4) | Pedro → Diego | 11 | OK |
| 19/11 | PR3 `GestorTurnos` (T-7) | Francois → Pedro | 74 | OK |
| 20/11 | PR4 `RepositorioArchivo` (T-8) | Diego → Fabrizzio | 93 | OK |
| 21/11 | PR5 `ServicioTurnos` (T-10) | Diego → Fabrizzio | 106 | OK |
| 23/11 | PR6 interfaz (T-11) | Fabrizzio → Francois | 106 | OK |
| 24/11 | PR7 rendimiento (T-13) | Fabrizzio → Francois | 107 | OK |
| 24/11 | Recorrido manual (T-14) | Francois → Pedro | 107 | OK |
| 25/11 | Matriz y pruebas (T-15) | Francois → Pedro | 107 | OK |
| 26/11 | Revisión de calidad (T-16) | Pedro → Diego | 107 | OK |
| 26/11 | Correcciones de calidad | Pedro → Diego | 110 | OK |
| 27/11 | Manual de usuario (T-17) | Fabrizzio → Francois | 110 | OK |
| 27/11 | README final (T-18) | Pedro → Francois | 110 | OK |

### 5.5 Fallos detectados antes de integrar
| Fuente | Cantidad | Nota                                                                                                                              |
|---|---|-----------------------------------------------------------------------------------------------------------------------------------|
| Comentarios en los 15 PR cerrados (GitHub) | 20 comentarios | Correción en T-3                                                                                                                  |
| Tareas con etiqueta «defecto» (ClickUp) | 0 | Las únicas etiquetas son «semana 1» y «semana 2»                                                                                  |
| Hallazgos de `REVISION_CALIDAD.md` | 31 | Se cuentan aparte: son hallazgos de la revisión final, no fallos de un PR. 26 corregidos, 1 parcial, 3 se mantienen y 1 pendiente |

### 5.6 Tiempo de corrección
| Caso | Registro del fallo | Corrección verificada                                                                                                    | Tiempo                                             |
|---|--|--------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------|
| Hallazgos de la revisión de calidad (31) | T-16 cerrada el 26/11 a las 10:48 | T-16.5 cerrada el 26/11 a las 22:30                                                                                      | Aproximadamente 4 horas |
| Defectos de PR | T-04, T-06   | T-04: Falta de comentarios explicativos más detallados para la clase test.  T-06: Error en método guardar() de la clase `RepositorioEnMemoria` | T-04: 30 minutos, T-06: 15 minutos                 |

### 5.7 Uso de IA
| Integrante | Usos | Aceptado | Ajustado | Descartado |
|---|---|---|---|---|
| Pedro | 1 (T-4) | 1 | 0 | 0 |
| Francois | 1 (T-7) | 1 | 0 | 0 |
| Diego | 3 (T-6, T-8, T-10) | 1 | 2 | 0 |
| Fabrizzio | 1 (T-5) | 0 | 1 | 0 |
| **Total** | **6** | **3** | **3** | **0** |

Para T-10 falta describir la verificación. Fuente: tabla de la sección 4.

## 6. Incidentes y cambios respecto al plan
- La interfaz se hizo en Swing y no en JavaFX como se tenía pensado originalmente.
- Tareas fuera del plazo establecido: Generalmente en la última semana, varias tareas (registradas en ClickUp) fueron aplazadas y esto retrasó el trabajo del equipo.
- Formato de PR no seguido: Algunos pull requests en el repositorio no cuentan con la nomenclatura adecuada "CU-<NUMERO_TAREA> DESCRIPCION", lo que entorpece la trazabilidad de los cambios a la rama default.
- Correcciones intermedias: La tarea T-16.5 supone un commit no contemplado para ajustar la T-16. Esto pudo haber sido evitado con una retroalimentación adecuada del PR o una previsión de posibles cambios.

## 7. Limitaciones
- El tiempo de seguimiento de cada tarea en ClickUp y el mecanismo de PR en Github demora el tiempo de desarrollo de forma considerable. A pesar de ser una excelente forma de organizar el trabajo y dar seguimiento a los cambios, en proyectos con una mayor cantidad de tareas y pendientes puede entorpecer el tiempo de desarrollo.
- Pese a que se da seguimiento al código elaborado con inteligencia artificial, para proyectos de mayor tamaño es necesario adoptar marcos y estándares formales para auditar y corroborar el código generado.
- Algunos cálculos, como el tiempo de preparación del entorno y de corrección de PR no se registran apropiadamente.
- Las mediciones de rendimiento se realizaron solamente con un equipo de trabajo.