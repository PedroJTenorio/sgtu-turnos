package sgtu.dominio;

import java.util.List;

/*
 Catálogo fijo de ventanillas lógicas (RN-03, RC-03).
 No se pueden agregar, quitar ni modificar.
*/
public final class Ventanillas {

    public static final List<String> IDS = List.of("V01", "V02", "V03");

    private Ventanillas() {
        // Clase de utilidad: no se instancia.
    }

    public static boolean existe(String id) {
        return id != null && IDS.contains(id);
    }
}