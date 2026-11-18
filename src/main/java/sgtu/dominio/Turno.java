package sgtu.dominio;

import java.time.LocalDateTime;
import java.util.Objects;

/*
 Solicitud individual de atención. Protege sus propias transiciones de estado
 (ERS 3.2): los métodos que cambian el estado solo son visibles dentro del paquete.
*/
public class Turno {

    private final long secuencia;
    private final LocalDateTime fechaHoraRegistro;
    private EstadoTurno estado;
    private String ventanillaAsignada;

    //Crea un turno nuevo En espera y sin ventanilla (RF-02).
    public Turno(long secuencia, LocalDateTime fechaHoraRegistro) {
        if (secuencia < 1) {
            throw new IllegalArgumentException("La secuencia debe ser mayor o igual a 1.");
        }
        this.secuencia = secuencia;
        this.fechaHoraRegistro = Objects.requireNonNull(fechaHoraRegistro, "La fecha de registro es obligatoria.");
        this.estado = EstadoTurno.EN_ESPERA;
        this.ventanillaAsignada = null;
    }

    // Reconstruye un turno guardado validando que su estado y su ventanilla sean coherentes entre sí.
    public static Turno restaurar(long secuencia, LocalDateTime fechaHoraRegistro, EstadoTurno estado, String ventanillaAsignada) {
        Turno turno = new Turno(secuencia, fechaHoraRegistro);
        Objects.requireNonNull(estado, "El estado es obligatorio.");
        boolean requiereVentanilla = estado == EstadoTurno.EN_ATENCION || estado == EstadoTurno.ATENDIDO;
        if (requiereVentanilla && !Ventanillas.existe(ventanillaAsignada)) {
            throw new IllegalArgumentException("El turno " + generarId(secuencia) + " en estado " + estado.getEtiqueta() + " requiere una ventanilla válida.");
        }
        if (!requiereVentanilla && ventanillaAsignada != null) {
            throw new IllegalArgumentException("El turno " + generarId(secuencia) + " en estado " + estado.getEtiqueta() + " no debe tener ventanilla.");
        }
        turno.estado = estado;
        turno.ventanillaAsignada = ventanillaAsignada;
        return turno;
    }

    // El identificador se deriva de la secuencia, por lo que ambos son únicos a la vez.
    public static String generarId(long secuencia) {
        return String.format("T%03d", secuencia);
    }

    public Turno copiar() {
        Turno copia = new Turno(secuencia, fechaHoraRegistro);
        copia.estado = estado;
        copia.ventanillaAsignada = ventanillaAsignada;
        return copia;
    }

    public String getId() {
        return generarId(secuencia);
    }

    public long getSecuencia() {
        return secuencia;
    }

    public LocalDateTime getFechaHoraRegistro() {
        return fechaHoraRegistro;
    }

    public EstadoTurno getEstado() {
        return estado;
    }

    // Ventanilla asignada, o null si el turno nunca fue asignado.
    public String getVentanillaAsignada() {
        return ventanillaAsignada;
    }

    void iniciarAtencion(String idVentanilla) throws OperacionInvalidaException {
        if (estado != EstadoTurno.EN_ESPERA) {
            throw new OperacionInvalidaException("El turno " + getId() + " no se puede asignar porque está " + estado.getEtiqueta() + ". Solo se asignan turnos En espera.");
        }
        estado = EstadoTurno.EN_ATENCION;
        ventanillaAsignada = idVentanilla;
    }

    void finalizar() throws OperacionInvalidaException {
        if (estado != EstadoTurno.EN_ATENCION) {
            throw new OperacionInvalidaException("El turno " + getId() + " no se puede finalizar porque está " + estado.getEtiqueta() + ". Solo se finalizan turnos En atención.");
        }
        estado = EstadoTurno.ATENDIDO;
    }

    void cancelar() throws OperacionInvalidaException {
        if (estado != EstadoTurno.EN_ESPERA) {
            throw new OperacionInvalidaException("El turno " + getId() + " no se puede cancelar porque está " + estado.getEtiqueta() + ". Solo se cancelan turnos En espera.");
        }
        estado = EstadoTurno.CANCELADO;
    }
}