# Medición de rendimiento (RNF-P01)

Fecha: 2026-11-24 · Medido por: Fabrizzio

## Equipo de la demostración
- Equipo / modelo: ASUS TUF Gaming A15 FA506ICB
- Sistema operativo: Ubuntu 24.04.3 LTS
- Procesador: AMD Ryzen 7 4800H with Radeon Graphics
- Memoria RAM: 16 GB (15 GiB utilizables reportados por Ubuntu)
- Disco (SSD/HDD): SSD NVMe (KINGSTON SNV2S500G y SAMSUNG MZVLQ512HBLU-00B00)
- Java: Oracle JDK 17.0.12 LTS (build 17.0.12+8-LTS-286)
- Maven: 3.9.9

## Resultados con 200 turnos (límite: 2000 ms por operación)
| Corrida | registrar | consultarFila | asignar | finalizar | Resultado |
|---|---|---|---|---|---|
| 1 | 0 ms (< 1 ms) | 0 ms (< 1 ms) | 1 ms | 0 ms (< 1 ms) | Pasó (255 ms total test) |
| 2 | 0 ms (< 1 ms) | 0 ms (< 1 ms) | 1 ms | 0 ms (< 1 ms) | Pasó (252 ms total test) |
| 3 | 0 ms (< 1 ms) | 0 ms (< 1 ms) | 1 ms | 0 ms (< 1 ms) | Pasó (258 ms total test) |

## Verificación de mutación del límite
Se modificó temporalmente `LIMITE_MILISEGUNDOS` de `2000` a `0` y la prueba falló con `AssertionFailedError` en la validación de límite, confirmando que las aserciones detectan correctamente un incumplimiento. Posteriormente se restauró a `2000` y se ejecutó la suite completa (`107` pruebas, `0` fallos).

## Conclusión
RNF-P01 se cumple ampliamente en este equipo: con 200 turnos almacenados en archivo real, todas las operaciones evaluadas respondieron en 1 ms o menos (los valores reportados como `0 ms` corresponden a tiempos inferiores a 1 ms debido al truncamiento entero de `Duration.toMillis()`), muy por debajo del límite máximo de 2000 ms por operación.