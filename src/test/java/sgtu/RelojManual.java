package sgtu;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

// Reloj de pruebas que solo avanza (o retrocede) cuando la prueba lo indica.
public class RelojManual extends Clock {

    private Instant ahora;

    public RelojManual(Instant inicio) {
        this.ahora = inicio;
    }

    public static RelojManual enFechaFija() {
        return new RelojManual(Instant.parse("2026-11-11T10:00:00Z"));
    }

    public void avanzar(Duration duracion) {
        ahora = ahora.plus(duracion);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }

    @Override
    public Instant instant() {
        return ahora;
    }
}